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
import com.example.project_mobileapps.features.admin.reports.MedicalRecordDetailScreen
import com.example.project_mobileapps.features.admin.reports.PatientHistoryDetailScreen
import com.example.project_mobileapps.features.admin.reports.PatientHistoryDetailViewModel
import com.example.project_mobileapps.features.admin.reports.PatientHistoryDetailViewModelFactory
import com.example.project_mobileapps.features.admin.reports.ReportScreen
import com.example.project_mobileapps.features.admin.reports.ReportViewModel
import com.example.project_mobileapps.features.admin.reports.ReportViewModelFactory
import com.example.project_mobileapps.features.doctor.ConsultationInputScreen
import com.example.project_mobileapps.features.doctor.MedicalRecordInputScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    onLogoutClick: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Variabel NavController untuk Admin
    val adminNavController = rememberNavController()

    val navBackStackEntry by adminNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Logic Sembunyikan App Bar di halaman detail
    val showMainAppBar = currentRoute != "add_manual_queue" &&
            (currentRoute?.startsWith("patient_history_detail") == false) &&
            (currentRoute?.startsWith("medical_record_input") == false) &&
            (currentRoute?.startsWith("consultation_input") == false) &&
            (currentRoute?.startsWith("medical_record_detail") == false)

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

                // 2. MONITORING ANTRIAN
                composable(AdminMenu.Monitoring.route) {
                    val user by AuthRepository.currentUser.collectAsState()
                    val userRole = user?.role
                    val monitorViewModel: AdminQueueMonitorViewModel = viewModel(
                        factory = AdminQueueMonitorViewModelFactory(AppContainer.queueRepository, AuthRepository)
                    )

                    AdminQueueMonitorScreen(
                        viewModel = monitorViewModel,
                        currentUserRole = userRole,
                        // Navigasi ke History
                        onNavigateToHistory = { patientId ->
                            adminNavController.navigate("patient_history_detail/$patientId")
                        },
                        // Navigasi ke Input Rekam Medis
                        onNavigateToMedicalRecord = { qId, name, num ->
                            adminNavController.navigate("medical_record_input/$qId/$name/$num")
                        }
                    )
                }

                // 3. SCREEN INPUT REKAM MEDIS
                composable(
                    route = "medical_record_input/{qId}/{name}/{num}",
                    arguments = listOf(
                        navArgument("qId") { type = NavType.StringType },
                        navArgument("name") { type = NavType.StringType },
                        navArgument("num") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val qId = backStackEntry.arguments?.getString("qId") ?: ""
                    val name = backStackEntry.arguments?.getString("name") ?: ""
                    val num = backStackEntry.arguments?.getString("num") ?: ""

                    val vm: AdminQueueMonitorViewModel = viewModel(
                        factory = AdminQueueMonitorViewModelFactory(AppContainer.queueRepository, AuthRepository)
                    )

                    MedicalRecordInputScreen(
                        queueId = qId,
                        patientName = name,
                        queueNumber = num,
                        viewModel = vm,
                        onNavigateBack = { adminNavController.popBackStack() },
                        onRecordSaved = {
                            // Jika sukses simpan, kembali ke Monitor dan refresh
                            adminNavController.popBackStack(AdminMenu.Monitoring.route, false)
                        }
                    )
                }

                // 4. MANAJEMEN JADWAL
                composable(AdminMenu.Management.items[0].route) {
                    val practiceViewModel: ManagePracticeScheduleViewModel = viewModel(
                        factory = ManagePracticeScheduleViewModelFactory(AppContainer.queueRepository)
                    )
                    ManagePracticeScheduleScreen(viewModel = practiceViewModel)
                }

                // 5. LAPORAN
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

                // 6. MANUAL QUEUE
                composable("add_manual_queue") {
                    AddManualQueueScreen(
                        onNavigateBack = { adminNavController.popBackStack() }
                    )
                }

                // 7. DETAIL HISTORY (VIEW ONLY)
                composable(
                    route = "patient_history_detail/{patientId}",
                    arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val patientId = backStackEntry.arguments?.getString("patientId") ?: ""

                    val detailViewModel: PatientHistoryDetailViewModel = viewModel(
                        factory = PatientHistoryDetailViewModelFactory(
                            AppContainer.queueRepository,
                            AuthRepository,
                            patientId // Passing ID
                        )
                    )

                    PatientHistoryDetailScreen(
                        viewModel = detailViewModel,
                        // PERBAIKAN: Gunakan 'adminNavController' di sini
                        onNavigateBack = { adminNavController.popBackStack() },
                        onNavigateToDetail = { visitId ->
                            adminNavController.navigate("medical_record_detail/$visitId")
                        }
                    )
                }

                // 8. DETAIL EMR (FULL PAGE)
                composable(
                    route = "medical_record_detail/{visitId}",
                    arguments = listOf(navArgument("visitId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val visitId = backStackEntry.arguments?.getString("visitId") ?: ""

                    MedicalRecordDetailScreen(
                        queueId = visitId,
                        onNavigateBack = { adminNavController.popBackStack() }
                    )
                }

                // 9. SCREEN LAMA (Consultation Input - Opsional/Legacy)
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
                            adminNavController.popBackStack(AdminMenu.Monitoring.route, false)
                        }
                    )
                }
            }
        }
    }
}