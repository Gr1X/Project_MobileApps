package com.example.project_mobileapps.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.data.model.Booking
import com.example.project_mobileapps.data.model.Doctor

/**
 * Bagian sapaan di paling atas Halaman Home.
 */
@Composable
fun GreetingSection(name: String) {
    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 16.dp)) {
        Text(
            text = "Good Morning, $name 👋",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Check Your Medical",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Data class untuk menampung informasi setiap item di grid.
 */
private data class ActionItem(val title: String, val icon: ImageVector)

/**
 * Grid yang menampilkan 8 ikon menu.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionGrid(
    onActionClick: (String) -> Unit
) {
    val actions = listOf(
        ActionItem("Appointment", Icons.Filled.CalendarMonth),
        ActionItem("Food", Icons.Filled.Fastfood),
        ActionItem("Care", Icons.Filled.Favorite),
        ActionItem("News", Icons.Filled.Newspaper),
    )

    // Gunakan Row untuk tata letak horizontal yang presisi
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween // Jaga jarak antar item
    ) {
        actions.forEach { action ->
            // Setiap item (Column) sekarang akan memiliki lebar yang sama persis
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f) // <-- KUNCI UTAMA: Memberi porsi ruang yang sama
                    .clickable { onActionClick(action.title) }
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.title,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = action.title, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Kartu untuk menampilkan janji temu yang akan datang.
 */

@Composable
fun UpcomingAppointmentCard(appointment: Booking) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Anda bisa tambahkan gambar dokter di sini jika ada di model Booking
            Column(modifier = Modifier.weight(1f)) {
//                Text(text = appointment.serviceName, fontWeight = FontWeight.Bold)
                Text(text = appointment.doctorName, style = MaterialTheme.typography.bodyMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
//                Text(text = appointment.appointmentTimestamp, fontWeight = FontWeight.Bold)
//                Text(text = appointment., style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * Bagian untuk menampilkan judul dan daftar dokter yang bisa di-scroll horizontal.
 */
@Composable
fun RecentAppointmentsSection(
    doctors: List<Doctor>,
    onDoctorClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = "Recent Appointment",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(doctors) { doctor ->
                // Pastikan DoctorCard Anda juga sudah benar menerima objek Doctor
                DoctorCard(doctor = doctor, onClick = { onDoctorClick(doctor.id) })
            }
        }
    }
}