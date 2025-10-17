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
import com.example.project_mobileapps.R
import com.example.project_mobileapps.ui.themes.PoppinsFamily
import com.example.project_mobileapps.ui.themes.TextSecondary

@Composable
fun GetStartedScreen(
    onGetStartedClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(id = R.drawable.bg_get_started),
                contentDescription = "Ilustrasi Dokter",
                modifier = Modifier.size(250.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Judul Utama
            Text(
                text = "Solusi Kesehatan dalam Genggaman Anda",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Atur janji temu dan pantau antrian praktik dokter dengan mudah dan cepat.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onGetStartedClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Mulai Sekarang",
                    style = MaterialTheme.typography.labelLarge
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