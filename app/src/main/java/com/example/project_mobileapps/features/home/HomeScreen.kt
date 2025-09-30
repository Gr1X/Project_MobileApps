package com.example.project_mobileapps.features.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.project_mobileapps.R
import com.example.project_mobileapps.data.model.Doctor

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onDoctorClick: (String) -> Unit,
    onAppointmentClick: () -> Unit,
    onNewsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Greeting
        Text(
            text = "Good Morning, ${uiState.userName ?: "User"} 👋",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32) // hijau
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Card Check Your Medical
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Check Your Medical",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Divider(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth(),
                    color = Color.LightGray,
                    thickness = 1.dp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MedicalIcon("Appointment", R.drawable.ic_calendar, onAppointmentClick)
                    MedicalIcon("Food", R.drawable.ic_food) { /*TODO*/ }
                    MedicalIcon("Care", R.drawable.ic_care) { /*TODO*/ }
                    MedicalIcon("News", R.drawable.ic_news, onNewsClick)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Appointment Title (warna hijau)
        Text(
            text = "Recent Appointment",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32) // hijau
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Card dokter tunggal
        uiState.recentDoctors.firstOrNull()?.let { doctor ->
            DoctorCard(
                doctor = doctor,
                onClick = onDoctorClick,
                doctorImageRes = R.drawable.dokter_cadangan // gunakan drawable baru
            )
        } ?: Text(
            text = "No doctor available",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun MedicalIcon(label: String, iconRes: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = Color(0xFF2E7D32),
            modifier = Modifier.size(40.dp)
        )
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun DoctorCard(
    doctor: Doctor,
    onClick: (String) -> Unit,
    doctorImageRes: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp) // card lebih tinggi supaya foto tidak kepotong
            .padding(8.dp)
            .clickable { onClick(doctor.id ?: "") },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = doctor.name ?: "Doctor",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = doctor.specialization ?: "Specialization",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp) // tinggi gambar disesuaikan supaya proporsional
            ) {
                Image(
                    painter = painterResource(id = doctorImageRes),
                    contentDescription = doctor.name ?: "Doctor",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}


