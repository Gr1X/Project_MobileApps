package com.example.project_mobileapps.features.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.ui.components.CircularBackButton
import com.example.project_mobileapps.ui.themes.TextSecondary

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
            // Kartu Status Besar
            StatusCard(status = status)

            // Kartu Informasi Detail
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailRow(label = "ID Kunjungan", value = visitId) // Bisa disembunyikan/dikecilkan jika mau
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailRow(label = "Tanggal Kunjungan", value = visitDate)
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailRow(label = "Dokter", value = doctorName)
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailRow(label = "Keluhan Awal", value = complaint)
                }
            }
        }
    }
}

@Composable
private fun StatusCard(status: QueueStatus) {
    val (statusText, statusColor, statusContentColor) = when (status) {
        QueueStatus.SELESAI -> Triple("Selesai", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        QueueStatus.DIBATALKAN -> Triple("Dibatalkan", Color(0xFFFFEBEE), Color(0xFFC62828))
        else -> Triple(status.name, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = statusContentColor
            )
            Text(
                text = if(status == QueueStatus.SELESAI) "Kunjungan telah selesai dilakukan." else "Kunjungan dibatalkan.",
                style = MaterialTheme.typography.bodyMedium,
                color = statusContentColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}