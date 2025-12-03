package com.example.project_mobileapps.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.model.Role
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.features.admin.AdminMainScreen
import com.example.project_mobileapps.features.auth.AuthScreen
import com.example.project_mobileapps.features.auth.AuthViewModelFactory
import com.example.project_mobileapps.features.doctor.DoctorDashboardScreen
import com.example.project_mobileapps.features.doctor.DoctorMainScreen
import com.example.project_mobileapps.features.doctor.DoctorViewModel
import com.example.project_mobileapps.features.doctor.DoctorViewModelFactory
import com.example.project_mobileapps.features.patient.doctorDetail.DoctorDetailScreen
import com.example.project_mobileapps.features.getstarted.GetStartedScreen
import com.example.project_mobileapps.features.patient.home.MainScreen
import com.example.project_mobileapps.features.patient.news.ArticleDetailScreen
import com.example.project_mobileapps.features.patient.news.NewsScreen
import com.example.project_mobileapps.features.profile.HistoryScreen
import com.example.project_mobileapps.features.patient.queue.QueueConfirmationScreen
import com.example.project_mobileapps.features.profile.EditProfileScreen
import com.example.project_mobileapps.features.profile.HistoryDetailScreen
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Composable utama yang mendefinisikan seluruh grafik navigasi aplikasi.
 * Bertanggung jawab untuk mengatur semua rute (routes), argumen, dan alur transisi antar layar.
 *
 * @param navController Controller yang mengelola state navigasi aplikasi.
 * @param modifier Modifier untuk diterapkan pada NavHost.
 */
@Composable
fun AppNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
    // Mengamati state user yang sedang login dari AuthRepository.
    val currentUser by AuthRepository.currentUser.collectAsState()

    // Secara dinamis menentukan tujuan awal aplikasi berdasarkan status login dan peran user.
    // Jika user belum login (null), arahkan ke alur otentikasi.
    // Jika sudah login, arahkan ke alur yang sesuai dengan perannya.
    val startDestination = when (currentUser?.role) {
        Role.PASIEN -> "main_flow"
        Role.DOKTER -> "doctor_flow"
        Role.ADMIN -> "admin_flow"
        null -> "auth_flow"
    }

    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        // --- ALUR OTENTIKASI (SEBELUM LOGIN) ---
        // 'navigation' digunakan untuk mengelompokkan beberapa layar terkait dalam satu alur (nested graph).
        // Ini membantu mengorganisir dan mereset back stack dengan lebih mudah.
        navigation(startDestination = "getStarted", route = "auth_flow") {
            composable("getStarted") {
                GetStartedScreen(
                    onGetStartedClick = { navController.navigate("auth/register") },
                    onLoginClick = { navController.navigate("auth/login") }
                )
            }
            composable("auth/{screen}", arguments = listOf(navArgument("screen") { type = NavType.StringType })) {
                val screen = it.arguments?.getString("screen") ?: "login"
                AuthScreen(
                    authViewModel = viewModel(factory = AuthViewModelFactory()),
                    startScreen = screen,
                    // Callback yang dijalankan setelah otentikasi berhasil.
                    // Menavigasi user ke alur yang sesuai dan menghapus alur otentikasi dari back stack.
                    onAuthSuccess = { user ->
                        val destination = when (user.role) {
                            Role.PASIEN -> "main_flow"
                            Role.DOKTER -> "doctor_flow"
                            Role.ADMIN -> "admin_flow"
                        }
                        navController.navigate(destination) { popUpTo("auth_flow") { inclusive = true } }
                    }
                )
            }
        }

        // --- ALUR UTAMA UNTUK PASIEN ---
        navigation(startDestination = "main_host", route = "main_flow") {
            // Layar utama yang menjadi host untuk Bottom Navigation Bar dan kontennya.
            composable("main_host") {
                MainScreen(rootNavController = navController)
            }
            // Layar detail dokter, menerima doctorId sebagai argumen.
            composable("doctorDetail/{doctorId}",
                arguments = listOf(navArgument("doctorId") { type = NavType.StringType })
            ) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                DoctorDetailScreen(
                    // Aksi saat tombol booking ditekan. Meluncurkan coroutine untuk proses asynchronous.
                    onBookingClick = { doctorId, keluhan ->
                        val user = AuthRepository.currentUser.value
                        if (user != null) {
                            scope.launch {
                                val result = AppContainer.queueRepository.takeQueueNumber(doctorId, user.uid, user.name, keluhan)
                                if (result.isSuccess) {
                                    // Navigasi ke layar konfirmasi jika berhasil.
                                    val newQueueNumber = result.getOrNull()?.queueNumber
                                    navController.navigate("queue_confirmation/$newQueueNumber") {
                                        // Hapus layar detail dokter dari back stack agar tidak bisa kembali.
                                        popUpTo("doctorDetail/{doctorId}") { inclusive = true }
                                    }
                                } else {
                                    // Tampilkan pesan error jika gagal.
                                    Toast.makeText(context, result.exceptionOrNull()?.message ?: "Gagal", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Layar konfirmasi setelah berhasil mengambil nomor antrian.
            composable(
                "queue_confirmation/{queueNumber}",
                arguments = listOf(navArgument("queueNumber") { type = NavType.StringType })
            ) { backStackEntry ->
                val queueNumber = backStackEntry.arguments?.getString("queueNumber") ?: "N/A"
                QueueConfirmationScreen(
                    queueNumber = queueNumber,
                    onNavigateToHome = {
                        navController.navigate("main_host") {
                            // Kembali ke home dan membersihkan back stack di atasnya.
                            popUpTo("main_host") { inclusive = true }
                        }
                    }
                )
            }

            // Layar untuk mengedit profil pengguna.
            composable("editProfile") {
                EditProfileScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable("history") {
                HistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onHistoryClick = { item ->
                        // Catatan tentang URL Encoding:
                        // Argumen string yang dilewatkan melalui route navigation tidak boleh mengandung karakter
                        // seperti spasi atau simbol lain. Oleh karena itu, kita perlu meng-encode setiap parameter
                        // sebelum membangun string route.
                        val encoder = { text: String -> URLEncoder.encode(text, StandardCharsets.UTF_8.toString()) }

                        val route = "historyDetail/" +
                                "${encoder(item.visitId)}/" +
                                "${encoder(item.visitDate)}/" +
                                "${encoder(item.doctorName)}/" +
                                "${encoder(item.initialComplaint)}/" +
                                "${encoder(item.status.name)}"

                        navController.navigate(route)
                    }
                )
            }

            // Layar detail riwayat kunjungan, menerima beberapa argumen.
            composable(
                route = "historyDetail/{visitId}/{visitDate}/{doctorName}/{complaint}/{status}",
                arguments = listOf(
                    navArgument("visitId") { type = NavType.StringType },
                    navArgument("visitDate") { type = NavType.StringType },
                    navArgument("doctorName") { type = NavType.StringType },
                    navArgument("complaint") { type = NavType.StringType },
                    navArgument("status") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val arguments = backStackEntry.arguments ?: return@composable
                // Di sini, kita perlu men-decode kembali argumen yang telah di-encode sebelumnya.
                val decoder = { arg: String -> URLDecoder.decode(arg, StandardCharsets.UTF_8.toString()) }

                val visitId = decoder(arguments.getString("visitId", ""))
                val visitDate = decoder(arguments.getString("visitDate", ""))
                val doctorName = decoder(arguments.getString("doctorName", ""))
                val complaint = decoder(arguments.getString("complaint", ""))
                // Mengkonversi string status kembali menjadi enum QueueStatus.
                val status = try {
                    QueueStatus.valueOf(decoder(arguments.getString("status", "SELESAI")))
                } catch (e: IllegalArgumentException) {
                    QueueStatus.SELESAI // Default value jika terjadi error.
                }

                HistoryDetailScreen(
                    visitId = visitId,
                    visitDate = visitDate,
                    doctorName = doctorName,
                    complaint = complaint,
                    status = status,
                    onNavigateBack = { navController.popBackStack() }
                )
            }


            composable("smart_meal_plan") {
                com.example.project_mobileapps.features.patient.mealplan.SmartMealPlanScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Layar daftar berita kesehatan.
            composable("news") {
                NewsScreen(
                    onNewsClick = { encodedUrl ->
                        // URL berita juga di-encode untuk memastikan navigasi yang aman.
                        navController.navigate("articleDetail/$encodedUrl")
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Layar untuk menampilkan detail artikel berita dalam WebView.
            composable(
                "articleDetail/{encodedUrl}",
                arguments = listOf(navArgument("encodedUrl") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("encodedUrl") ?: ""
                val decodedUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                ArticleDetailScreen(
                    url = decodedUrl,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // --- ALUR UTAMA UNTUK ADMIN ---
        navigation(startDestination = "admin_dashboard", route = "admin_flow") {
            composable("admin_dashboard") {
                val scope = rememberCoroutineScope()
                AdminMainScreen(
                    // Aksi logout akan membersihkan state user dan mengarahkan kembali ke alur otentikasi.
                    onLogoutClick = {
                        scope.launch {
                            AuthRepository.logout()
                            navController.navigate("auth_flow") {
                                popUpTo("admin_flow") { inclusive = true }
                            }
                        }
                    }
                )
            }
        }

        // --- ALUR UTAMA UNTUK DOKTER ---
        navigation(startDestination = "doctor_main", route = "doctor_flow") {
            composable("doctor_main") { // Buat route baru untuk wadah utama
                val scope = rememberCoroutineScope()
                DoctorMainScreen(
                    // Logika logout sama dengan admin.
                    onLogoutClick = {
                        scope.launch {
                            AuthRepository.logout()
                            navController.navigate("auth_flow") {
                                popUpTo("doctor_flow") { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }
}