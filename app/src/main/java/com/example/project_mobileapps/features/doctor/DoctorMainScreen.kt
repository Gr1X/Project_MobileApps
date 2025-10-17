// File: features/doctor/DoctorMainScreen.kt
package com.example.project_mobileapps.features.doctor

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.project_mobileapps.core.navigation.DoctorMenu
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.features.admin.manageSchedule.ManagePracticeScheduleScreen
import com.example.project_mobileapps.features.admin.manageSchedule.ManagePracticeScheduleViewModel
import com.example.project_mobileapps.features.admin.manageSchedule.ManagePracticeScheduleViewModelFactory
import com.example.project_mobileapps.features.admin.manageSchedule.ManageScheduleScreen
import com.example.project_mobileapps.features.admin.manageSchedule.ManageScheduleViewModel
import com.example.project_mobileapps.features.admin.manageSchedule.ManageScheduleViewModelFactory
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
                TopAppBar(
                    title = { /* Biarkan kosong */ },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = doctorNavController,
                startDestination = DoctorMenu.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
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

                composable(DoctorMenu.Queue.route) {
                    // ✅ AMBIL INFORMASI USER
                    val user by AuthRepository.currentUser.collectAsState()
                    val userRole = user?.role

                    val manageScheduleViewModel: ManageScheduleViewModel = viewModel(
                        factory = ManageScheduleViewModelFactory(
                            AppContainer.queueRepository,
                            AuthRepository
                        )
                    )
                    // ✅ TERUSKAN ROLE KE SCREEN
                    ManageScheduleScreen(
                        viewModel = manageScheduleViewModel,
                        currentUserRole = userRole
                    )
                }
                composable(DoctorMenu.ManageSchedule.route) {
                    // Gunakan kembali ViewModel dan Composable dari Admin
                    val practiceViewModel: ManagePracticeScheduleViewModel = viewModel(
                        factory = ManagePracticeScheduleViewModelFactory(AppContainer.queueRepository)
                    )
                    ManagePracticeScheduleScreen(viewModel = practiceViewModel)
                }
            }
        }
    }
}