// File: core/navigation/AdminNavItem.kt
package com.example.project_mobileapps.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Interface dasar tertutup (sealed) untuk semua jenis item navigasi di panel Admin.
 * Menggunakan sealed interface memungkinkan kita untuk memiliki beberapa tipe data (seperti item tunggal dan grup)
 * dalam satu daftar navigasi yang sama.
 */
sealed interface AdminNavigationItem {
    val route: String
    val label: String
    val icon: ImageVector
}

/**
 * Merepresentasikan item menu navigasi tunggal yang dapat diklik.
 *
 * @property route String unik yang digunakan oleh Navigation Component untuk menavigasi ke screen.
 * @property label Teks yang akan ditampilkan untuk item menu ini.
 * @property icon Ikon yang akan ditampilkan di samping label.
 */
data class AdminNavItem(
    override val route: String,
    override val label: String,
    override val icon: ImageVector
) : AdminNavigationItem

/**
 * Merepresentasikan grup menu (header dropdown) yang berisi beberapa [AdminNavItem].
 * Item ini sendiri tidak dapat dinavigasi, tetapi berfungsi sebagai header yang bisa di-expand/collapse.
 *
 * @property label Teks yang akan ditampilkan untuk header grup ini.
 * @property icon Ikon yang akan ditampilkan di samping label grup.
 * @property items Daftar [AdminNavItem] yang berada di bawah grup ini.
 * @property route Dibuat kosong karena grup ini tidak memiliki tujuan navigasi.
 */

data class AdminNavGroup(
    override val label: String,
    override val icon: ImageVector,
    val items: List<AdminNavItem>
) : AdminNavigationItem {
    override val route: String = ""
}

/**
 * Object singleton yang mendefinisikan dan mengelola semua item navigasi untuk panel Admin.
 * Menggunakan object ini membuat struktur menu menjadi terpusat dan mudah dikelola.
 */

object AdminMenu {
    /** Navigasi ke layar Dashboard utama Admin. */
    val Dashboard = AdminNavItem(
        route = "admin_dashboard",
        label = "Dashboard",
        icon = Icons.Outlined.Dashboard
    )

    /** Navigasi ke layar pemantauan antrian real-time. */
    val Monitoring = AdminNavItem(
        route = "monitoring_queue",
        label = "Pantauan Antrian",
        icon = Icons.Outlined.ListAlt
    )

    /** Grup menu untuk semua fitur manajemen. */
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

    /** Daftar lengkap semua item navigasi untuk membangun UI di Navigation Drawer. */
    val allNavItems = listOf(Dashboard, Monitoring, Management)
}