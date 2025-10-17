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

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

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