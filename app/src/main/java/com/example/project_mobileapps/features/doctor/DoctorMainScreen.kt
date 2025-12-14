// File: features/doctor/DoctorMainScreen.kt
package com.example.project_mobileapps.features.doctor

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import com.example.project_mobileapps.core.navigation.DoctorMenu
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.features.admin.manageSchedule.*
import com.example.project_mobileapps.features.admin.medicine.ManageMedicineScreen
import com.example.project_mobileapps.features.admin.reports.PatientHistoryDetailScreen
import com.example.project_mobileapps.features.admin.reports.PatientHistoryDetailViewModel
import com.example.project_mobileapps.features.admin.reports.PatientHistoryDetailViewModelFactory
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

    // Logic: Sembunyikan TopAppBar jika sedang di halaman detail/input
    val showMainAppBar = (currentRoute?.startsWith("patient_history_detail") == false) &&
            (currentRoute?.startsWith("medical_record_input") == false) &&
            (currentRoute?.startsWith("medical_record_detail") == false) &&
            (currentRoute?.startsWith("manage_medicine") == false)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DoctorDrawerContent(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    doctorNavController.navigate(route) {
                        popUpTo(doctorNavController.graph.startDestinationId)
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
                        title = { },
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
                modifier = Modifier.padding(innerPadding)
            ) {
                // 1. DASHBOARD
                composable(DoctorMenu.Dashboard.route) {
                    val doctorViewModel: DoctorViewModel = viewModel(
                        factory = DoctorViewModelFactory(
                            AppContainer.queueRepository,
                            AuthRepository
                        )
                    )
                    DoctorDashboardScreen(
                        viewModel = doctorViewModel,
                        navController = doctorNavController
                    )
                }

                // 2. QUEUE MONITOR (ANTRIAN)
                composable(DoctorMenu.Queue.route) {
                    val user by AuthRepository.currentUser.collectAsState()
                    val userRole = user?.role
                    val monitorViewModel: AdminQueueMonitorViewModel = viewModel(
                        factory = AdminQueueMonitorViewModelFactory(
                            AppContainer.queueRepository,
                            AuthRepository
                        )
                    )
                    AdminQueueMonitorScreen(
                        viewModel = monitorViewModel,
                        currentUserRole = userRole,
                        // Navigasi ke History List
                        onNavigateToHistory = { patientId ->
                            doctorNavController.navigate("patient_history_detail/$patientId")
                        },
                        // [UPDATE] Navigasi ke Input EMR (Kirim juga userId!)
                        onNavigateToMedicalRecord = { qId, name, num, userId ->
                            doctorNavController.navigate("medical_record_input/$qId/$name/$num/$userId")
                        }
                    )
                }

                // 3. MEDICAL RECORD INPUT (Form Input)
                composable(
                    // [UPDATE] Tambahkan /{userId} di route
                    route = "medical_record_input/{qId}/{name}/{num}/{userId}",
                    arguments = listOf(
                        navArgument("qId") { type = NavType.StringType },
                        navArgument("name") { type = NavType.StringType },
                        navArgument("num") { type = NavType.StringType },
                        navArgument("userId") { type = NavType.StringType } // [UPDATE] Argument baru
                    )
                ) { backStackEntry ->
                    val qId = backStackEntry.arguments?.getString("qId") ?: ""
                    val name = backStackEntry.arguments?.getString("name") ?: ""
                    val num = backStackEntry.arguments?.getString("num") ?: ""
                    val userId = backStackEntry.arguments?.getString("userId") ?: "" // [UPDATE] Ambil userId

                    // [UPDATE PENTING] Gunakan MedicalRecordViewModelFactory
                    // Jangan pakai AdminQueueMonitorViewModel lagi di sini!
                    val medicalRecordViewModel: MedicalRecordViewModel = viewModel(
                        factory = MedicalRecordViewModelFactory(AppContainer.queueRepository)
                    )

                    MedicalRecordInputScreen(
                        queueId = qId,
                        patientName = name,
                        queueNumber = num,
                        patientId = userId, // [UPDATE] Pass userId ke screen
                        viewModel = medicalRecordViewModel, // Inject VM yang benar
                        onNavigateBack = { doctorNavController.popBackStack() },
                        onRecordSaved = {
                            doctorNavController.popBackStack(DoctorMenu.Queue.route, false)
                        }
                    )
                }

                // 4. DETAIL HISTORY LIST
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

                // 5. DETAIL EMR FULL PAGE
                composable(
                    route = "medical_record_detail/{visitId}",
                    arguments = listOf(navArgument("visitId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val visitId = backStackEntry.arguments?.getString("visitId") ?: ""

                    com.example.project_mobileapps.features.admin.reports.MedicalRecordDetailScreen(
                        queueId = visitId,
                        onNavigateBack = { doctorNavController.popBackStack() }
                    )
                }

                // 6. MANAJEMEN JADWAL
                composable(DoctorMenu.ManageSchedule.route) {
                    val practiceViewModel: ManagePracticeScheduleViewModel = viewModel(
                        factory = ManagePracticeScheduleViewModelFactory(AppContainer.queueRepository)
                    )
                    ManagePracticeScheduleScreen(viewModel = practiceViewModel)
                }

                // 7. MANAJEMEN OBAT
                composable("manage_medicine") {
                    ManageMedicineScreen(
                        onNavigateBack = { doctorNavController.popBackStack() }
                    )
                }
            }
        }
    }
}