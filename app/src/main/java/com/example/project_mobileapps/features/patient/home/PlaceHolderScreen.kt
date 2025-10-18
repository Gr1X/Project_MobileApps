package com.example.project_mobileapps.features.patient.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
/**
 * Composable sederhana yang berfungsi sebagai layar placeholder (pengganti sementara).
 * Kemungkinan digunakan untuk debugging atau untuk role yang belum memiliki
 * layar khusus (misal: Admin sebelum panel Admin dibuat).
 *
 * @param role Peran (role) pengguna yang sedang login (misal: "ADMIN", "PASIEN").
 * @param onLogoutClick Callback yang dipanggil saat tombol "Logout" diklik.
 */
@Composable
fun PlaceholderScreen(role: String, onLogoutClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Selamat Datang!",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Anda login sebagai $role",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = onLogoutClick) {
                Text("Logout")
            }
        }
    }
}