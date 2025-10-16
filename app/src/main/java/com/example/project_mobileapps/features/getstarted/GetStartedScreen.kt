package com.example.project_mobileapps.features.getstarted

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.R // Pastikan Anda punya gambar ilustrasi di drawable
import com.example.project_mobileapps.ui.themes.PoppinsFamily
import com.example.project_mobileapps.ui.themes.TextSecondary

@Composable
fun GetStartedScreen(
    onGetStartedClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background // Menggunakan warna AppBackground dari tema
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Spacer untuk mendorong konten ke tengah, sisakan ruang untuk tombol di bawah
            Spacer(modifier = Modifier.weight(1f))

            // Ilustrasi (Ganti R.drawable.doctor_illustration dengan gambar Anda sendiri)
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground), // GANTI DENGAN GAMBAR ILUSTRASI ANDA
                contentDescription = "Ilustrasi Dokter",
                modifier = Modifier.size(250.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Judul Utama
            Text(
                text = "Solusi Kesehatan dalam Genggaman Anda",
                style = MaterialTheme.typography.headlineMedium, // Menggunakan font Poppins Bold, 28sp
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Teks Deskripsi
            Text(
                text = "Atur janji temu dan pantau antrian praktik dokter dengan mudah dan cepat.",
                style = MaterialTheme.typography.bodyMedium, // Menggunakan font Poppins Regular, 14sp
                color = TextSecondary, // Menggunakan warna abu-abu dari tema
                textAlign = TextAlign.Center
            )

            // Spacer untuk mendorong tombol ke bawah
            Spacer(modifier = Modifier.weight(1f))

            // Tombol Aksi
            Button(
                onClick = onGetStartedClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Mulai Sekarang",
                    style = MaterialTheme.typography.labelLarge // Menggunakan Poppins SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onLoginClick) {
                Text(
                    text = "Saya Sudah Punya Akun",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}