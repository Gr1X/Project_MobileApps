// File BARU: features/doctor/DoctorMainScreen.kt
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
                    title = { /* Biarkan kosong agar judul ada di dalam konten */ },
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
                            com.example.project_mobileapps.di.AppContainer.queueRepository,
                            com.example.project_mobileapps.data.repo.AuthRepository
                        )
                    )

                    DoctorDashboardScreen(
                        viewModel = doctorViewModel,
                        navController = doctorNavController
                    )
                }
            }
        }
    }
}