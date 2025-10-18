package com.example.project_mobileapps.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
/**
 * Mendefinisikan item-item navigasi yang akan ditampilkan di Bottom Navigation Bar.
 * Menggunakan `sealed class` memastikan bahwa semua item navigasi bawah terdefinisi di satu tempat
 * dan kita tidak bisa membuat instance baru di luar file ini, menjaga konsistensi.
 *
 * @property route String unik yang digunakan oleh NavController untuk navigasi.
 * @property icon Ikon yang akan ditampilkan untuk item ini.
 * @property label Teks label yang akan ditampilkan di bawah ikon.
 */
sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Home")
    object Queue : BottomNavItem("queue", Icons.Default.List, "Antrian")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profile")
}