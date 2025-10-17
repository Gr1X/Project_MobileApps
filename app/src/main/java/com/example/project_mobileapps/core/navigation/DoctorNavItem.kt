// File: core/navigation/DoctorNavItem.kt
package com.example.project_mobileapps.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.ui.graphics.vector.ImageVector

data class DoctorNavItem(val route: String, val label: String, val icon: ImageVector)

object DoctorMenu {
    val Dashboard = DoctorNavItem(
        route = "doctor_dashboard",
        label = "Dashboard",
        icon = Icons.Outlined.Dashboard
    )

    val Queue = DoctorNavItem(
        route = "doctor_queue",
        label = "Antrian",
        icon = Icons.Outlined.ListAlt
    )

    val ManageSchedule = DoctorNavItem(
        route = "doctor_manage_schedule",
        label = "Atur Jadwal",
        icon = Icons.Outlined.EditCalendar
    )

    val allNavItems = listOf(Dashboard, Queue, ManageSchedule)
}