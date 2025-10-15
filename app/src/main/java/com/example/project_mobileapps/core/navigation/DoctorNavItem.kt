// File BARU: core/navigation/DoctorNavItem.kt
package com.example.project_mobileapps.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.ui.graphics.vector.ImageVector

data class DoctorNavItem(val route: String, val label: String, val icon: ImageVector)

object DoctorMenu {
    val Dashboard = DoctorNavItem(
        route = "doctor_dashboard",
        label = "Dashboard",
        icon = Icons.Outlined.Dashboard
    )

    val allNavItems = listOf(Dashboard)
}