// File: core/navigation/AdminNavItem.kt
package com.example.project_mobileapps.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.ui.graphics.vector.ImageVector

// Interface dasar untuk semua item navigasi
sealed interface AdminNavigationItem {
    val route: String
    val label: String
    val icon: ImageVector
}

// Untuk item menu biasa yang bisa diklik
data class AdminNavItem(
    override val route: String,
    override val label: String,
    override val icon: ImageVector
) : AdminNavigationItem

// Untuk grup menu (header dropdown)
data class AdminNavGroup(
    override val label: String,
    override val icon: ImageVector,
    val items: List<AdminNavItem>
) : AdminNavigationItem {
    // Grup tidak punya route sendiri, jadi kita buat default
    override val route: String = ""
}


// Definisikan semua item dan grup menu kita di sini
object AdminMenu {
    val Dashboard = AdminNavItem(
        route = "admin_dashboard",
        label = "Dashboard",
        icon = Icons.Outlined.Dashboard
    )
    val Monitoring = AdminNavItem(
        route = "monitoring_queue",
        label = "Pantauan Antrian",
        icon = Icons.Outlined.ListAlt
    )
    val Management = AdminNavGroup(
        label = "Manajemen",
        icon = Icons.Outlined.EditCalendar,
        items = listOf(
            AdminNavItem(
                route = "manage_schedule",
                label = "Atur Jadwal",
                icon = Icons.Outlined.EditCalendar
            ),
            AdminNavItem(
                route = "reports",
                label = "Laporan Pasien",
                icon = Icons.Outlined.Analytics
            )
        )
    )

    // Daftar lengkap untuk ditampilkan di drawer
    val allNavItems = listOf(Dashboard, Monitoring, Management)
}