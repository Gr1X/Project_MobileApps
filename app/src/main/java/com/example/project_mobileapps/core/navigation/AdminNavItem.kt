package com.example.project_mobileapps.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AdminNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : AdminNavItem("admin_dashboard", "Dashboard", Icons.Outlined.Dashboard)
    object ManageSchedule : AdminNavItem("manage_schedule", "Atur Jadwal", Icons.Outlined.EditCalendar)
    object Reports : AdminNavItem("reports", "Laporan", Icons.Outlined.Analytics)
    object AddManualQueue : AdminNavItem("add_manual_queue", "Tambah Antrian", Icons.Outlined.Add)
}