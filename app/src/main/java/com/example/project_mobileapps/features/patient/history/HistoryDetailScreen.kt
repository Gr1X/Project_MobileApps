package com.example.project_mobileapps.features.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.ui.components.CircularBackButton
import com.example.project_mobileapps.ui.themes.TextSecondary
/**
 * Composable untuk layar Detail Riwayat Kunjungan.
 * Ini adalah layar "dumb" (stateless) yang hanya menerima data sebagai parameter
 * dan menampilkannya. Data ini diteruskan dari [HistoryScreen] melalui navigasi.
 *
 * @param visitId ID unik kunjungan (saat ini tidak ditampilkan, tapi bagus untuk dimiliki).
 * @param visitDate String tanggal kunjungan.
 * @param doctorName Nama dokter yang menangani.
 * @param complaint Keluhan awal yang dicatat.
 * @param status Status akhir kunjungan (misal: SELESAI, DIBATALKAN).
 * @param onNavigateBack Callback untuk kembali ke layar sebelumnya.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    visitId: String,
    visitDate: String,
    doctorName: String,
    complaint: String,
    status: QueueStatus,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Kunjungan") },
                navigationIcon = { CircularBackButton(onClick = onNavigateBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Kartu Informasi Utama
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailRow(label = "Tanggal Kunjungan", value = visitDate)
                    Divider()
                    DetailRow(label = "Dokter", value = doctorName)
                    Divider()
                    DetailRow(label = "Keluhan Awal", value = complaint)
                }
            }

            // Kartu Status
            StatusCard(status = status)
        }
    }
}
/**
 * Composable helper (private) untuk menampilkan satu baris detail (Label + Value).
 * @param label Teks label di bagian atas (misal: "Dokter").
 * @param value Teks nilai di bagian bawah (misal: "Dr. Budi Santoso").
 */
@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
/**
 * Composable helper (private) untuk menampilkan kartu status di bagian bawah.
 * @param status Enum [QueueStatus] untuk menentukan teks dan warna.
 */
@Composable
private fun StatusCard(status: QueueStatus) {
    val (statusText, statusColor, statusTextColor) = when (status) {
        QueueStatus.SELESAI -> Triple("Selesai", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        QueueStatus.DIBATALKAN -> Triple("Dibatalkan", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        else -> Triple(status.name, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Status: $statusText",
                style = MaterialTheme.typography.titleMedium,
                color = statusTextColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}