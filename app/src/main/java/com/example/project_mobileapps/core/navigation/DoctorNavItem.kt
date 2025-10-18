// File: core/navigation/DoctorNavItem.kt
package com.example.project_mobileapps.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.ui.graphics.vector.ImageVector


/**
 * Merepresentasikan satu item menu navigasi yang dapat diklik di dalam panel Dokter.
 *
 * @property route String unik yang digunakan oleh NavController untuk menavigasi ke screen tujuan.
 * @property label Teks yang akan ditampilkan untuk item menu ini di UI.
 * @property icon Ikon yang akan ditampilkan di samping label.
 */
data class DoctorNavItem(val route: String, val label: String, val icon: ImageVector)
/**
 * Object singleton yang mendefinisikan semua item navigasi yang tersedia untuk panel Dokter.
 * Menggunakan object ini memastikan bahwa semua definisi rute dan label terpusat dan konsisten.
 */
object DoctorMenu {
    /** Navigasi ke layar Dashboard utama Dokter. */
    val Dashboard = DoctorNavItem(
        route = "doctor_dashboard",
        label = "Dashboard",
        icon = Icons.Outlined.Dashboard
    )
    /** Navigasi ke layar untuk memantau dan mengelola antrian pasien. */
    val Queue = DoctorNavItem(
        route = "doctor_queue",
        label = "Antrian",
        icon = Icons.Outlined.ListAlt
    )

    /** Navigasi ke layar untuk mengatur jadwal praktik. */
    val ManageSchedule = DoctorNavItem(
        route = "doctor_manage_schedule",
        label = "Atur Jadwal",
        icon = Icons.Outlined.EditCalendar
    )

    /** Daftar lengkap semua item navigasi untuk membangun UI di Navigation Drawer Dokter. */
    val allNavItems = listOf(Dashboard, Queue, ManageSchedule)
}