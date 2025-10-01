package com.example.project_mobileapps.features.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.R
import com.example.project_mobileapps.data.model.Doctor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onDoctorClick: (String) -> Unit,
    onAppointmentClick: () -> Unit,
    onNewsClick: () -> Unit,
    onLogOutClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Good Morning, ${uiState.userName ?: "User"} 👋",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    )
                },

                actions = {
                    IconButton(onClick = onLogOutClick) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Check Your Medical",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
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

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Appointment Title
            Text(
                text = "Recent Appointment",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Daftar dokter yang bisa di-scroll horizontal (LazyRow)
            if (uiState.recentDoctors.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(uiState.recentDoctors) { doctor ->
                        DoctorCard(
                            doctor = doctor,
                            onClick = onDoctorClick,
                            doctorImageRes = R.drawable.dokter_cadangan
                        )
                    }
                }
            } else {
                Text(
                    text = "No recent doctors available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
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
            .width(300.dp)
            .height(450.dp)
            .clickable { onClick(doctor.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = doctor.name, // Perbaikan: Langsung ambil dari objek Doctor
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = doctor.specialization,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Gunakan weight agar gambar mengisi sisa ruang
            ) {
                Image(
                    painter = painterResource(id = doctorImageRes),
                    contentDescription = doctor.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}