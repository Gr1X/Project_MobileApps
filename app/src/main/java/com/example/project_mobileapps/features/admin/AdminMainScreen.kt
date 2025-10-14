package com.example.project_mobileapps.features.admin

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // <-- TAMBAHKAN IMPORT INI
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.project_mobileapps.core.navigation.AdminNavItem
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.di.AppContainer
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

    // =======================================================
    // DAPATKAN CONTEXT DI SINI
    // =======================================================
    val context = LocalContext.current
    // =======================================================

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
                    title = {
                        val title = when (currentRoute) {
                            AdminNavItem.ManageSchedule.route -> "Atur Jadwal"
                            AdminNavItem.Reports.route -> "Laporan"
                            else -> "Dashboard Admin"
                        }
                        Text(title)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            floatingActionButton = {
                if (currentRoute == AdminNavItem.Dashboard.route) {
                    FloatingActionButton(onClick = {
                        adminNavController.navigate(AdminNavItem.AddManualQueue.route)
                    }) {
                        Icon(Icons.Default.Add, "Tambah Antrian Manual")
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = adminNavController,
                startDestination = AdminNavItem.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(AdminNavItem.Dashboard.route) {
                    AdminDashboardScreen(onLogoutClick = {})
                }
                composable(AdminNavItem.ManageSchedule.route) {
                    Text("Halaman untuk Mengatur Jadwal Dokter")
                }
                composable(AdminNavItem.Reports.route) {
                    Text("Halaman untuk Melihat Laporan Pasien")
                }
                composable(AdminNavItem.AddManualQueue.route) {
                    AddManualQueueScreen(
                        onNavigateBack = { adminNavController.popBackStack() },
                        onAddQueue = { patientName, complaint ->
                            scope.launch {
                                val result = AppContainer.queueRepository.addManualQueue(patientName, complaint)
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Pasien berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                                    adminNavController.popBackStack()
                                } else {
                                    Toast.makeText(context, result.exceptionOrNull()?.message ?: "Gagal", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}