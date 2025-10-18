package com.example.project_mobileapps.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.project_mobileapps.navigation.BottomNavItem
/**
 * Composable untuk menampilkan Bottom Navigation Bar.
 * Menggunakan [NavigationBar] dan [NavigationBarItem] dari Material 3.
 * Mengambil daftar item dari [BottomNavItem] dan menangani navigasi antar tab utama.
 *
 * @param navController [NavController] yang digunakan untuk navigasi antar tab.
 * NavController ini biasanya adalah NavController *internal* untuk [MainScreen].
 */
@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Queue,
        BottomNavItem.Profile
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Hindari membuat salinan destinasi yang sama di atas tumpukan.
                        launchSingleTop = true
                        // Kembalikan state saat memilih kembali item yang sebelumnya dipilih.
                        restoreState = true
                    }
                }
            )
        }
    }
}