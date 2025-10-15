package com.example.project_mobileapps.features.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.project_mobileapps.core.navigation.AdminMenu // <-- PERBAIKI IMPORT INI
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.features.admin.dashboard.AdminDashboardScreen
import com.example.project_mobileapps.features.admin.manageSchedule.ManagePracticeScheduleScreen
import com.example.project_mobileapps.features.admin.manageSchedule.ManagePracticeScheduleViewModel
import com.example.project_mobileapps.features.admin.manageSchedule.ManagePracticeScheduleViewModelFactory
import com.example.project_mobileapps.features.admin.manageSchedule.ManageScheduleScreen
import com.example.project_mobileapps.features.admin.manageSchedule.ManageScheduleViewModel
import com.example.project_mobileapps.features.admin.manageSchedule.ManageScheduleViewModelFactory
import com.example.project_mobileapps.features.admin.manualQueue.AddManualQueueScreen
import com.example.project_mobileapps.features.admin.reports.ReportScreen
import com.example.project_mobileapps.features.admin.reports.ReportViewModel
import com.example.project_mobileapps.features.admin.reports.ReportViewModelFactory
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
                TopAppBar(
                    title = {},

                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            floatingActionButton = {
                if (currentRoute == AdminMenu.Monitoring.route) {
                    FloatingActionButton(onClick = {
                        adminNavController.navigate("add_manual_queue")
                    }) {
                        Icon(Icons.Default.Add, "Tambah Antrian Manual")
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = adminNavController,
                startDestination = AdminMenu.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {

                composable(AdminMenu.Dashboard.route) {
                    val dashboardViewModel: AdminDashboardViewModel = viewModel(
                        factory = AdminDashboardViewModelFactory(AppContainer.queueRepository)
                    )
                    AdminDashboardScreen(viewModel = dashboardViewModel)
                }

                composable(AdminMenu.Monitoring.route) {
                    val manageScheduleViewModel: ManageScheduleViewModel = viewModel(
                        factory = ManageScheduleViewModelFactory(
                            AppContainer.queueRepository,
                            AuthRepository
                        )
                    )
                    ManageScheduleScreen(viewModel = manageScheduleViewModel)
                }

                composable(AdminMenu.Management.items[0].route) {
                    val practiceViewModel: ManagePracticeScheduleViewModel = viewModel(
                        factory = ManagePracticeScheduleViewModelFactory(AppContainer.queueRepository)
                    )
                    ManagePracticeScheduleScreen(viewModel = practiceViewModel)
                }

                composable(AdminMenu.Management.items[1].route) { // "reports"
                    val reportViewModel: ReportViewModel = viewModel(
                        factory = ReportViewModelFactory(AppContainer.queueRepository)
                    )
                    ReportScreen(viewModel = reportViewModel)
                }

                composable("add_manual_queue") {
                    AddManualQueueScreen(
                        onNavigateBack = { adminNavController.popBackStack() }
                    )
                }
            }
        }
    }
}