// File: features/doctor/DoctorMainScreen.kt
package com.example.project_mobileapps.features.doctor

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.*
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
import com.example.project_mobileapps.core.navigation.DoctorMenu
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.features.admin.manageSchedule.AdminQueueMonitorScreen
import com.example.project_mobileapps.features.admin.manageSchedule.AdminQueueMonitorViewModel
import com.example.project_mobileapps.features.admin.manageSchedule.AdminQueueMonitorViewModelFactory
import com.example.project_mobileapps.features.admin.manageSchedule.ManagePracticeScheduleScreen
import com.example.project_mobileapps.features.admin.manageSchedule.ManagePracticeScheduleViewModel
import com.example.project_mobileapps.features.admin.manageSchedule.ManagePracticeScheduleViewModelFactory
import com.example.project_mobileapps.features.admin.manualQueue.AddManualQueueScreen
import com.example.project_mobileapps.features.admin.medicine.ManageMedicineScreen
import com.example.project_mobileapps.features.admin.reports.MedicalRecordDetailScreen
import com.example.project_mobileapps.features.admin.reports.PatientHistoryDetailScreen
import com.example.project_mobileapps.features.admin.reports.PatientHistoryDetailViewModel
import com.example.project_mobileapps.features.admin.reports.PatientHistoryDetailViewModelFactory
import com.example.project_mobileapps.features.admin.reports.ReportScreen
import com.example.project_mobileapps.features.admin.reports.ReportViewModel
import com.example.project_mobileapps.features.admin.reports.ReportViewModelFactory
import com.example.project_mobileapps.ui.components.AppDrawerContent
import com.example.project_mobileapps.ui.components.NavigationItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorMainScreen(
    onLogoutClick: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val doctorNavController = rememberNavController()

    val navBackStackEntry by doctorNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // --- DEFINISI MENU DOKTER (DISAMAKAN DENGAN ADMIN) ---
    val doctorMenuItems = listOf(
        NavigationItem("Dashboard", Icons.Outlined.Dashboard, DoctorMenu.Dashboard.route),
        NavigationItem("Antrian Pasien", Icons.Outlined.MonitorHeart, DoctorMenu.Queue.route),
        // Menu Tambahan (Shared dengan Admin)
        NavigationItem("Jadwal Dokter", Icons.Outlined.CalendarMonth, "manage_schedule"),
        NavigationItem("Laporan & Pasien", Icons.Outlined.Assessment, "manage_reports"),
        NavigationItem("Manajemen Obat", Icons.Outlined.Medication, "manage_medicine")
    )

    // Logic Sembunyikan TopBar
    val showMainAppBar = currentRoute != "add_manual_queue" &&
            (currentRoute?.startsWith("medical_record_input") == false) &&
            (currentRoute?.startsWith("patient_history_detail") == false) &&
            (currentRoute?.startsWith("medical_record_detail") == false) &&
            (currentRoute?.startsWith("manage_medicine") == false) // TopBar obat beda sendiri

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                currentRoute = currentRoute ?: DoctorMenu.Dashboard.route,
                onNavigate = { route ->
                    doctorNavController.navigate(route) {
                        popUpTo(doctorNavController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                    scope.launch { drawerState.close() }
                },
                onLogout = onLogoutClick,
                menuItems = doctorMenuItems,
                userName = "Dr. Budi Santoso",
                userRole = "Dokter Umum"
            )
        }
    ) {
        Scaffold(
            topBar = {
                if (showMainAppBar) {
                    TopAppBar(
                        title = { /* Judul kosong/dinamis */ },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = doctorNavController,
                startDestination = DoctorMenu.Dashboard.route,
                modifier = if (showMainAppBar) Modifier.padding(innerPadding) else Modifier
            ) {
                // 1. DASHBOARD DOKTER
                composable(DoctorMenu.Dashboard.route) {
                    val viewModel: DoctorViewModel = viewModel(
                        factory = DoctorViewModelFactory(AppContainer.queueRepository, AuthRepository)
                    )
                    DoctorDashboardScreen(
                        viewModel = viewModel,
                        onNavigateToQueueList = {
                            doctorNavController.navigate(DoctorMenu.Queue.route)
                        }
                    )
                }

                // 2. MONITORING ANTRIAN
                composable(DoctorMenu.Queue.route) {
                    val user by AuthRepository.currentUser.collectAsState()
                    val monitorViewModel: AdminQueueMonitorViewModel = viewModel(
                        factory = AdminQueueMonitorViewModelFactory(
                            AppContainer.queueRepository,
                            AuthRepository
                        )
                    )

                    AdminQueueMonitorScreen(
                        viewModel = monitorViewModel,
                        currentUserRole = user?.role,
                        onNavigateToHistory = { patientId ->
                            doctorNavController.navigate("patient_history_detail/$patientId")
                        },
                        onNavigateToMedicalRecord = { qId, name, num, userId ->
                            doctorNavController.navigate("medical_record_input/$qId/$name/$num/$userId")
                        },
                        onNavigateToManualInput = {
                            doctorNavController.navigate("add_manual_queue")
                        }
                    )
                }

                // 3. JADWAL DOKTER (Shared)
                composable("manage_schedule") {
                    val practiceViewModel: ManagePracticeScheduleViewModel = viewModel(
                        factory = ManagePracticeScheduleViewModelFactory(AppContainer.queueRepository)
                    )
                    ManagePracticeScheduleScreen(viewModel = practiceViewModel)
                }

                // 4. LAPORAN & PASIEN (Shared)
                composable("manage_reports") {
                    val reportViewModel: ReportViewModel = viewModel(
                        factory = ReportViewModelFactory(AppContainer.queueRepository, AuthRepository)
                    )
                    ReportScreen(
                        viewModel = reportViewModel,
                        onPatientClick = { patientId ->
                            doctorNavController.navigate("patient_history_detail/$patientId")
                        }
                    )
                }

                // 5. MANAJEMEN OBAT (Shared)
                composable("manage_medicine") {
                    ManageMedicineScreen(
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }

                // --- SUB SCREENS ---

                // 6. INPUT REKAM MEDIS
                composable(
                    route = "medical_record_input/{qId}/{name}/{num}/{userId}",
                    arguments = listOf(
                        navArgument("qId") { type = NavType.StringType },
                        navArgument("name") { type = NavType.StringType },
                        navArgument("num") { type = NavType.StringType },
                        navArgument("userId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val qId = backStackEntry.arguments?.getString("qId") ?: ""
                    val name = backStackEntry.arguments?.getString("name") ?: ""
                    val num = backStackEntry.arguments?.getString("num") ?: ""
                    val userId = backStackEntry.arguments?.getString("userId") ?: ""

                    val medRecordVM: MedicalRecordViewModel = viewModel(
                        factory = MedicalRecordViewModelFactory(AppContainer.queueRepository)
                    )

                    MedicalRecordInputScreen(
                        queueId = qId,
                        patientName = name,
                        queueNumber = num,
                        patientId = userId,
                        viewModel = medRecordVM,
                        onNavigateBack = { doctorNavController.popBackStack() },
                        onRecordSaved = {
                            doctorNavController.popBackStack(DoctorMenu.Queue.route, false)
                        }
                    )
                }

                // 7. MANUAL INPUT
                composable("add_manual_queue") {
                    AddManualQueueScreen(
                        onNavigateBack = { doctorNavController.popBackStack() }
                    )
                }

                // 8. DETAIL HISTORY LIST
                composable(
                    route = "patient_history_detail/{patientId}",
                    arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
                    val detailViewModel: PatientHistoryDetailViewModel = viewModel(
                        factory = PatientHistoryDetailViewModelFactory(
                            AppContainer.queueRepository,
                            AuthRepository,
                            patientId
                        )
                    )
                    PatientHistoryDetailScreen(
                        viewModel = detailViewModel,
                        onNavigateBack = { doctorNavController.popBackStack() },
                        onNavigateToDetail = { visitId ->
                            doctorNavController.navigate("medical_record_detail/$visitId")
                        }
                    )
                }

                // 9. DETAIL EMR LENGKAP
                composable(
                    route = "medical_record_detail/{visitId}",
                    arguments = listOf(navArgument("visitId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val visitId = backStackEntry.arguments?.getString("visitId") ?: ""
                    MedicalRecordDetailScreen(
                        queueId = visitId,
                        onNavigateBack = { doctorNavController.popBackStack() }
                    )
                }
            }
        }
    }
}