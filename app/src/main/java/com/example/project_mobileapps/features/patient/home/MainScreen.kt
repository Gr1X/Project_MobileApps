// File: features/patient/home/MainScreen.kt
package com.example.project_mobileapps.features.patient.home

import android.app.Application // Pastikan import ini ada
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // Pastikan import ini ada
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.features.patient.queue.QueueScreen
import com.example.project_mobileapps.features.profile.ProfileScreen
import com.example.project_mobileapps.navigation.BottomNavItem
import com.example.project_mobileapps.ui.components.BottomNavBar
import kotlinx.coroutines.launch

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

            // --- TAB HOME ---
            composable(BottomNavItem.Home.route) {
                // 1. Ambil Context & Application
                val context = LocalContext.current
                val application = context.applicationContext as Application

                // 2. Pass 'application' ke Factory
                val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(application))
                val uiState by homeViewModel.uiState.collectAsState()

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

            // --- TAB QUEUE (ANTRIAN) ---
            composable(BottomNavItem.Queue.route) {
                // PERBAIKAN DI SINI: Kita juga butuh HomeViewModel untuk mengambil ID Dokter
                // Jadi kita harus ambil context & application lagi
                val context = LocalContext.current
                val application = context.applicationContext as Application

                // Pass 'application' ke Factory (JANGAN KOSONG)
                val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(application))
                val homeUiState by homeViewModel.uiState.collectAsState()
                val onlyDoctorId = homeUiState.doctor?.id ?: "doc_123"

                QueueScreen(
                    onBackToHome = { mainNavController.navigate(BottomNavItem.Home.route) },
                    onNavigateToTakeQueue = { rootNavController.navigate("doctorDetail/$onlyDoctorId") }
                )
            }

            // --- TAB PROFILE ---
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