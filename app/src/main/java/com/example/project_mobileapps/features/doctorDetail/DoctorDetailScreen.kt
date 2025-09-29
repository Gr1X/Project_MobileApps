package com.example.project_mobileapps.features.doctorDetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage // <-- 1. TAMBAHKAN IMPORT INI
import com.example.project_mobileapps.R // Untuk placeholder
import com.example.project_mobileapps.data.model.Doctor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDetailScreen(
    doctor: Doctor?,
    onBookingClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(doctor?.name ?: "Detail Dokter") })
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (doctor != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    // 2. GANTI BAGIAN IMAGE DENGAN ASYNCIMAGE
                    AsyncImage(
                        model = doctor.photoUrl,
                        contentDescription = "Foto ${doctor.name}",
                        placeholder = painterResource(id = R.drawable.ic_launcher_background),
                        error = painterResource(id = R.drawable.ic_launcher_background),
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = doctor.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = doctor.specialization, // Pastikan namanya 'specialization' di model Doctor
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = { onBookingClick() }) {
                        Text("Booking Jadwal")
                    }
                }
            } else {
                CircularProgressIndicator()
            }
        }
    }
}