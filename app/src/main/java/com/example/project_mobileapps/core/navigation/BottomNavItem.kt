package com.example.project_mobileapps.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Home")
    // UBAH BARIS DI BAWAH INI
    object Queue : BottomNavItem("queue", Icons.Default.List, "Antrian")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profile")
}