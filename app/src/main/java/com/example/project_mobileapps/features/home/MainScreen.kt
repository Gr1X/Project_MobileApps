package com.example.project_mobileapps.features.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.features.profile.ProfileScreen
import com.example.project_mobileapps.features.queue.QueueScreen
import com.example.project_mobileapps.features.queue.QueueViewModel
import com.example.project_mobileapps.features.queue.QueueViewModelFactory
import com.example.project_mobileapps.navigation.BottomNavItem
import com.example.project_mobileapps.ui.components.BottomNavBar

@Composable
fun MainScreen(rootNavController: NavHostController) {
    val mainNavController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController = mainNavController) }
    ) { innerPadding ->
        NavHost(
            navController = mainNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(BottomNavItem.Home.route) {
                val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory())
                val uiState by homeViewModel.uiState.collectAsState()
                val onlyDoctorId = uiState.doctor?.id ?: "doc_123"

                // =======================================================
                // PERBAIKI PEMANGGILAN FUNGSI DI BAWAH INI
                // =======================================================
                HomeScreen(
                    uiState = uiState,
                    onDoctorClick = { doctorId ->
                        rootNavController.navigate("doctorDetail/$doctorId")
                    },
                    onNavigateToQueue = { mainNavController.navigate(BottomNavItem.Queue.route) },
                    onProfileClick = { mainNavController.navigate(BottomNavItem.Profile.route) },
                    onTakeQueueClick = { rootNavController.navigate("doctorDetail/$onlyDoctorId") },
                    onNewsClick = { rootNavController.navigate("news")}
                )
                // =======================================================
            }

            composable(BottomNavItem.Queue.route) {
                val queueViewModel: QueueViewModel = viewModel(
                    factory = QueueViewModelFactory(
                        AppContainer.queueRepository,
                        AuthRepository
                    )
                )
                QueueScreen(
                    queueViewModel = queueViewModel,
                    onBackToHome = { mainNavController.navigate(BottomNavItem.Home.route) }
                )
            }

            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    onLogoutClick = {
                        AuthRepository.logout()
                        rootNavController.navigate("auth_flow") {
                            popUpTo("main_flow") { inclusive = true }
                        }
                    },
                    onNavigateToHistory = { rootNavController.navigate("history") }
                )
            }
        }
    }
}