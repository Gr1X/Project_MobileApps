package com.example.project_mobileapps.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.project_mobileapps.navigation.BottomNavItem

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
                // --- PERBAIKAN UTAMA ADA DI BLOK onClick DI BAWAH INI ---
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up ke destinasi awal dari graph untuk menghindari penumpukan back stack
                        // saat memilih item yang sama berulang kali.
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