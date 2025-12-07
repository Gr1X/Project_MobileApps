// File: features/admin/AdminMainScreen.kt
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
import com.example.project_mobileapps.features.doctor.ConsultationInputScreen // Import Screen Baru
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    onLogoutClick: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val adminNavController = rememberNavController()
    val navBackStackEntry by adminNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Sembunyikan App Bar di halaman-halaman tertentu
    val showMainAppBar = currentRoute != "add_manual_queue" &&
            (currentRoute?.startsWith("patient_history_detail") == false) &&
            (currentRoute?.startsWith("consultation_input") == false)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminDrawerContent(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    adminNavController.navigate(route) {
                        popUpTo(adminNavController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                    scope.launch { drawerState.close() }
                },
                onLogoutClick = onLogoutClick
            )
        }
    ) {
        Scaffold(
            topBar = {
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
                if (currentRoute == AdminMenu.Monitoring.route) {
                    FloatingActionButton(onClick = { adminNavController.navigate("add_manual_queue") }) {
                        Icon(Icons.Default.Add, "Tambah Antrian Manual")
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = adminNavController,
                startDestination = AdminMenu.Dashboard.route,
                modifier = if (showMainAppBar) Modifier.padding(innerPadding) else Modifier
            ) {
                // 1. DASHBOARD
                composable(AdminMenu.Dashboard.route) {
                    AdminDashboardScreen(
                        onNavigateToSchedule = { adminNavController.navigate(AdminMenu.Management.items[0].route) },
                        onNavigateToMonitoring = { adminNavController.navigate(AdminMenu.Monitoring.route) },
                        onNavigateToReports = { adminNavController.navigate(AdminMenu.Management.items[1].route) }
                    )
                }

                // 2. MONITORING ANTRIAN (ERROR FIXED HERE)
                composable(AdminMenu.Monitoring.route) {
                    val user by AuthRepository.currentUser.collectAsState()
                    val userRole = user?.role
                    val monitorViewModel: AdminQueueMonitorViewModel = viewModel(
                        factory = AdminQueueMonitorViewModelFactory(AppContainer.queueRepository, AuthRepository)
                    )

                    AdminQueueMonitorScreen(
                        viewModel = monitorViewModel,
                        currentUserRole = userRole,
                        // Tambahkan parameter navigasi yang hilang:
                        onNavigateToHistory = { patientId ->
                            adminNavController.navigate("patient_history_detail/$patientId")
                        },
                        onFinishConsultation = { qNo, pName ->
                            adminNavController.navigate("consultation_input/$qNo/$pName")
                        }
                    )
                }

                // 3. MANAJEMEN JADWAL
                composable(AdminMenu.Management.items[0].route) {
                    val practiceViewModel: ManagePracticeScheduleViewModel = viewModel(
                        factory = ManagePracticeScheduleViewModelFactory(AppContainer.queueRepository)
                    )
                    ManagePracticeScheduleScreen(viewModel = practiceViewModel)
                }

                // 4. LAPORAN
                composable(AdminMenu.Management.items[1].route) {
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

                // 5. MANUAL QUEUE
                composable("add_manual_queue") {
                    AddManualQueueScreen(
                        onNavigateBack = { adminNavController.popBackStack() }
                    )
                }

                // 6. DETAIL HISTORY (VIEW ONLY)
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

                // 7. INPUT REKAM MEDIS (INPUT FORM)
                composable(
                    route = "consultation_input/{queueNumber}/{patientName}",
                    arguments = listOf(
                        navArgument("queueNumber") { type = NavType.IntType },
                        navArgument("patientName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val qNo = backStackEntry.arguments?.getInt("queueNumber") ?: 0
                    val pName = backStackEntry.arguments?.getString("patientName") ?: ""

                    ConsultationInputScreen(
                        queueNumber = qNo,
                        patientName = pName,
                        onNavigateBack = { adminNavController.popBackStack() },
                        onConsultationFinished = {
                            // Kembali ke monitor setelah selesai
                            adminNavController.popBackStack(AdminMenu.Monitoring.route, false)
                        }
                    )
                }
            }
        }
    }
}