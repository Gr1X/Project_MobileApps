// Salin dan ganti seluruh isi file: features/admin/AdminMainScreen.kt

package com.example.project_mobileapps.features.admin

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.project_mobileapps.core.navigation.AdminMenu
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.features.admin.dashboard.AdminDashboardScreen
import com.example.project_mobileapps.features.admin.manageSchedule.AdminQueueMonitorScreen
import com.example.project_mobileapps.features.admin.manageSchedule.AdminQueueMonitorViewModel
import com.example.project_mobileapps.features.admin.manageSchedule.AdminQueueMonitorViewModelFactory
import com.example.project_mobileapps.features.admin.manageSchedule.ManagePracticeScheduleScreen
import com.example.project_mobileapps.features.admin.manageSchedule.ManagePracticeScheduleViewModel
import com.example.project_mobileapps.features.admin.manageSchedule.ManagePracticeScheduleViewModelFactory
import com.example.project_mobileapps.features.admin.manualQueue.AddManualQueueScreen
import com.example.project_mobileapps.features.admin.reports.PatientHistoryDetailScreen
import com.example.project_mobileapps.features.admin.reports.PatientHistoryDetailViewModel
import com.example.project_mobileapps.features.admin.reports.PatientHistoryDetailViewModelFactory
import com.example.project_mobileapps.features.admin.reports.ReportScreen
import com.example.project_mobileapps.features.admin.reports.ReportViewModel
import com.example.project_mobileapps.features.admin.reports.ReportViewModelFactory
import kotlinx.coroutines.launch

/**
 * Composable utama yang menjadi container untuk seluruh alur (flow) Admin.
 * Screen ini mengatur Navigation Drawer, Top App Bar, dan NavHost untuk semua
 * layar yang diakses oleh admin.
 *
 * @param onLogoutClick Callback yang dieksekusi ketika admin menekan tombol logout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    onLogoutClick: () -> Unit
) {
    // State untuk mengontrol kondisi drawer (terbuka/tertutup).
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    // Coroutine scope untuk mengontrol drawer secara asynchronous (misal: membuka dan menutup drawer).
    val scope = rememberCoroutineScope()
    // NavController khusus untuk navigasi di dalam alur admin (nested navigation).
    val adminNavController = rememberNavController()
    // Mengamati back stack untuk mendapatkan rute yang sedang aktif.
    val navBackStackEntry by adminNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Logika untuk menyembunyikan Top App Bar utama pada layar detail tertentu
    // yang memiliki Top App Bar-nya sendiri (misal: "Tambah Antrian Manual").
    val showMainAppBar = currentRoute != "add_manual_queue" && currentRoute != "patient_history_detail/{patientId}"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // Menggunakan Composable terpisah untuk konten drawer agar kode tetap bersih.
            AdminDrawerContent(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    adminNavController.navigate(route) {
                        // Membersihkan back stack hingga ke tujuan awal untuk menghindari tumpukan layar.
                        popUpTo(adminNavController.graph.startDestinationId) { this@navigate.launchSingleTop = true }
                    }
                    scope.launch { drawerState.close() }
                },
                onLogoutClick = onLogoutClick
            )
        }
    ) {
        Scaffold(
            topBar = {
                // Top App Bar utama hanya ditampilkan jika `showMainAppBar` bernilai true.
                if (showMainAppBar) {
                    TopAppBar(
                        title = {},
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                // Floating Action Button (FAB) untuk menambah antrian hanya muncul di layar pemantauan.
                if (currentRoute == AdminMenu.Monitoring.route) {
                    FloatingActionButton(onClick = { adminNavController.navigate("add_manual_queue") }) {
                        Icon(Icons.Default.Add, "Tambah Antrian Manual")
                    }
                }
            }
        ) { innerPadding ->
            // NavHost ini mengatur navigasi *di dalam* alur admin.
            NavHost(
                navController = adminNavController,
                startDestination = AdminMenu.Dashboard.route,
                // --- PERUBAHAN 3: Gunakan padding hanya jika App Bar utama tampil ---
                modifier = if (showMainAppBar) Modifier.padding(innerPadding) else Modifier
            ) {
                // Layar Dashboard Admin
                composable(AdminMenu.Dashboard.route) {
                    AdminDashboardScreen(
                        onNavigateToSchedule = { adminNavController.navigate(AdminMenu.Management.items[0].route) },
                        onNavigateToMonitoring = { adminNavController.navigate(AdminMenu.Monitoring.route) },
                        onNavigateToReports = { adminNavController.navigate(AdminMenu.Management.items[1].route) }
                    )
                }

                // Layar Pemantauan Antrian
                composable(AdminMenu.Monitoring.route) {
                    val user by AuthRepository.currentUser.collectAsState()
                    val userRole = user?.role
                    val monitorViewModel: AdminQueueMonitorViewModel = viewModel(
                        factory = AdminQueueMonitorViewModelFactory(AppContainer.queueRepository, AuthRepository)
                    )
                    AdminQueueMonitorScreen(viewModel = monitorViewModel, currentUserRole = userRole)
                }

                // Layar Manajemen Jadwal
                composable(AdminMenu.Management.items[0].route) { // "manage_schedule"
                    val practiceViewModel: ManagePracticeScheduleViewModel = viewModel(
                        factory = ManagePracticeScheduleViewModelFactory(AppContainer.queueRepository)
                    )
                    ManagePracticeScheduleScreen(viewModel = practiceViewModel)
                }

                // Layar Laporan
                composable(AdminMenu.Management.items[1].route) { // "reports"
                    val reportViewModel: ReportViewModel = viewModel(
                        factory = ReportViewModelFactory(AppContainer.queueRepository, AuthRepository)
                    )
                    ReportScreen(
                        viewModel = reportViewModel,
                        onPatientClick = { patientId ->
                            adminNavController.navigate("patient_history_detail/$patientId")
                        }
                    )
                }

                // Layar Detail Riwayat Pasien (diakses dari layar Laporan)
                composable("add_manual_queue") {
                    AddManualQueueScreen(
                        onNavigateBack = { adminNavController.popBackStack() }
                    )
                }

                composable(
                    route = "patient_history_detail/{patientId}",
                    arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                ) {
                    val detailViewModel: PatientHistoryDetailViewModel = viewModel(
                        factory = PatientHistoryDetailViewModelFactory(AppContainer.queueRepository, AuthRepository)
                    )
                    PatientHistoryDetailScreen(
                        viewModel = detailViewModel,
                        onNavigateBack = { adminNavController.popBackStack() }
                    )
                }
            }
        }
    }
}