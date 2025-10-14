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
import com.example.project_mobileapps.data.model.Role
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.features.admin.AdminDashboardScreen
import com.example.project_mobileapps.features.admin.AdminMainScreen
import com.example.project_mobileapps.features.auth.AuthScreen
import com.example.project_mobileapps.features.auth.AuthViewModelFactory
import com.example.project_mobileapps.features.doctor.DoctorDashboardScreen
import com.example.project_mobileapps.features.doctorDetail.DoctorDetailScreen
import com.example.project_mobileapps.features.getstarted.GetStartedScreen
import com.example.project_mobileapps.features.home.MainScreen
import com.example.project_mobileapps.features.home.PlaceholderScreen
import com.example.project_mobileapps.features.news.ArticleDetailScreen
import com.example.project_mobileapps.features.news.NewsScreen
import com.example.project_mobileapps.features.profile.HistoryScreen
import com.example.project_mobileapps.features.queue.QueueConfirmationScreen
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
    val currentUser by AuthRepository.currentUser.collectAsState()
    val startDestination = when (currentUser?.role) {
        Role.PASIEN -> "main_flow"
        Role.DOKTER -> "doctor_flow"
        Role.ADMIN -> "admin_flow"
        null -> "auth_flow"
    }

    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        // --- ALUR OTENTIKASI (SEBELUM LOGIN) ---
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

            // --- ALUR APLIKASI UTAMA (SETELAH LOGIN) ---
        navigation(startDestination = "main_host", route = "main_flow") {

            composable("main_host") {
                MainScreen(rootNavController = navController)
            }

            composable("doctorDetail/{doctorId}",
                arguments = listOf(navArgument("doctorId") { type = NavType.StringType })
            ) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                DoctorDetailScreen(
                    onBookingClick = { doctorId, keluhan ->
                        val user = AuthRepository.currentUser.value
                        if (user != null) {
                            scope.launch {
                                val result = AppContainer.queueRepository.takeQueueNumber(doctorId, user.uid, user.name, keluhan)
                                if (result.isSuccess) {
                                    val newQueueNumber = result.getOrNull()?.queueNumber
                                    navController.navigate("queue_confirmation/$newQueueNumber") {
                                        popUpTo("doctorDetail/{doctorId}") { inclusive = true }
                                    }
                                } else {
                                    Toast.makeText(context, result.exceptionOrNull()?.message ?: "Gagal", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                "queue_confirmation/{queueNumber}",
                arguments = listOf(navArgument("queueNumber") { type = NavType.StringType })
            ) { backStackEntry ->
                val queueNumber = backStackEntry.arguments?.getString("queueNumber") ?: "N/A"
                QueueConfirmationScreen(
                    queueNumber = queueNumber,
                    onNavigateToHome = {
                        navController.navigate("main_host") {
                            popUpTo("main_host") { inclusive = true }
                        }
                    }
                )
            }

            composable("history") {
                HistoryScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("news") {
                NewsScreen(
                    onNewsClick = { encodedUrl ->
                        navController.navigate("articleDetail/$encodedUrl")
                    }
                )
            }

            composable(
                "articleDetail/{encodedUrl}",
                arguments = listOf(navArgument("encodedUrl") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("encodedUrl") ?: ""
                // Decode the URL before passing it to the screen
                val decodedUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())

                 ArticleDetailScreen(
                    url = decodedUrl,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // --- ALUR DOKTER ---
        navigation(startDestination = "doctor_dashboard", route = "doctor_flow") {

            composable("doctor_dashboard") {
                DoctorDashboardScreen(
                    onLogoutClick = {
                        AuthRepository.logout()
                        navController.navigate("auth_flow") {
                            popUpTo("doctor_flow") { inclusive = true }
                        }
                    }
                )
            }

        }

        // Di dalam NavHost -> cari navigation(route = "admin_flow")
        navigation(startDestination = "admin_dashboard", route = "admin_flow") {
            composable("admin_dashboard") {
                AdminMainScreen(
                    onLogoutClick = {
                        AuthRepository.logout()
                        navController.navigate("auth_flow") {
                            popUpTo("admin_flow") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}