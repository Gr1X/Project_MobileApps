package com.example.project_mobileapps.features.patient.home

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
import com.example.project_mobileapps.features.patient.queue.QueueScreen
import com.example.project_mobileapps.features.patient.queue.QueueViewModel
import com.example.project_mobileapps.features.patient.queue.QueueViewModelFactory
import com.example.project_mobileapps.navigation.BottomNavItem
import com.example.project_mobileapps.ui.components.BottomNavBar
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
/**
 * Composable root untuk alur (flow) utama Pasien setelah login.
 * Layar ini mengatur [Scaffold] dengan [BottomNavBar] dan [NavHost]
 * untuk 3 tab utama: Home, Queue, dan Profile.
 *
 * @param rootNavController [NavHostController] utama dari aplikasi. Digunakan untuk
 * navigasi ke alur lain (seperti "doctorDetail", "news", atau "auth_flow" saat logout).
 */
@Composable
fun MainScreen(rootNavController: NavHostController) {
    val mainNavController = rememberNavController()
    val scope = rememberCoroutineScope()

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

                HomeScreen(
                    uiState = uiState,
                    onDoctorClick = { doctorId ->
                        rootNavController.navigate("doctorDetail/$doctorId")
                    },
                    onNavigateToQueue = { mainNavController.navigate(BottomNavItem.Queue.route) },
                    onTakeQueueClick = { mainNavController.navigate(BottomNavItem.Queue.route) },
                    onProfileClick = { mainNavController.navigate(BottomNavItem.Profile.route) },
                    onNewsClick = { rootNavController.navigate("news") }
                )
            }
            /**
             * Rute untuk Tab Antrian [BottomNavItem.Queue.route]
             */
            composable(BottomNavItem.Queue.route) {
                val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory())
                val homeUiState by homeViewModel.uiState.collectAsState()
                val onlyDoctorId = homeUiState.doctor?.id ?: "doc_123"

                QueueScreen(
                    onBackToHome = { mainNavController.navigate(BottomNavItem.Home.route) },
                    onNavigateToTakeQueue = { rootNavController.navigate("doctorDetail/$onlyDoctorId") }
                )
            }

            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    rootNavController = rootNavController,
                    onLogoutClick = {
                        scope.launch {
                            AuthRepository.logout()
                            rootNavController.navigate("auth_flow") {
                                popUpTo("main_flow") { inclusive = true }
                            }
                        }
                    },
                    onNavigateToHistory = { rootNavController.navigate("history") },
                    onNavigateToEditProfile = { rootNavController.navigate("editProfile") }
                )
            }
        }
    }
}