// File BARU: features/admin/reports/ReportScreen.kt
package com.example.project_mobileapps.features.admin.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.ui.components.PatientStatsChart
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: ReportViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- Judul ---
            item {
                Text(
                    "Analitik & Laporan",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // --- Filter Rentang Waktu ---
            item {
                FilterDropdownButton(
                    options = ReportRange.values().toList(),
                    selectedOption = uiState.selectedRange,
                    onOptionSelected = { viewModel.setReportRange(it) }
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KpiCard(
                            label = "Total Pasien Dilayani",
                            value = uiState.totalPatientsServed.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            label = "Pasien Rata-rata/Hari",
                            value = String.format(Locale.US, "%.1f", uiState.avgPatientsPerDay),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KpiCard(
                            label = "Waktu Layan Rata-rata",
                            value = "~${uiState.avgServiceTimeMinutes} min",
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            label = "Tingkat Pembatalan",
                            value = "${uiState.cancellationRate.toInt()}%",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                PatientStatsChart(reportData = uiState.dailyPatientTrend)
            }

            item {
                Text(
                    "Detail Antrian",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState.detailedQueues.isEmpty()) {
                item {
                    Text("Tidak ada data antrian pada rentang waktu ini.")
                }
            } else {
                items(uiState.detailedQueues) { queueItem ->
                    ReportQueueItemRow(item = queueItem)
                }
            }
        }
    }
}

@Composable
fun FilterDropdownButton(
    options: List<ReportRange>,
    selectedOption: ReportRange,
    onOptionSelected: (ReportRange) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Filter: ${selectedOption.displayName}")
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Buka Filter")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { range ->
                DropdownMenuItem(
                    text = { Text(range.displayName) },
                    onClick = {
                        onOptionSelected(range)
                        expanded = false
                    }
                )
            }
        }
    }
}

// Composable untuk setiap baris di daftar detail
@Composable
fun ReportQueueItemRow(item: QueueItem) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "No. ${item.queueNumber} - ${item.userName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Daftar: ${dateFormat.format(item.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = item.status.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Divider() // Garis bawah pemisah
    }
}

@Composable
fun KpiCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}