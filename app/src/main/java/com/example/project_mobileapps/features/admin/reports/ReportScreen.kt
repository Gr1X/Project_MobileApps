// Salin dan ganti seluruh isi file: features/admin/reports/ReportScreen.kt

package com.example.project_mobileapps.features.admin.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.ui.components.PatientStatsChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onPatientClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp) // Jarak konsisten antar item utama
        ) {
            item {
                Text("Laporan & Analitik", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }

            item {
                FilterSection(
                    uiState = uiState,
                    onPeriodSelect = viewModel::setPeriod,
                    onYearSelect = viewModel::setYear,
                    onMonthSelect = viewModel::setMonth
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KpiCard("Total Pasien Selesai", uiState.totalPatientsServed.toString(), Icons.Outlined.CheckCircle, Modifier.weight(1f))
                        KpiCard("Rata-rata Pasien/Hari", String.format("%.1f", uiState.avgPatientsPerDay), Icons.Outlined.CalendarToday, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KpiCard("Rata-rata Waktu Layan", "~${uiState.avgServiceTimeMinutes} min", Icons.Outlined.AccessTime, Modifier.weight(1f))
                        KpiCard("Tingkat Pembatalan", "${uiState.cancellationRate.toInt()}%", Icons.Outlined.Cancel, Modifier.weight(1f))
                    }
                }
            }

            item {
                PatientStatsChart(reportData = uiState.chartData)
            }

            item {
                Text("Riwayat Pasien di Periode Ini", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (uiState.uniquePatients.isEmpty()) {
                item { Text("Tidak ada riwayat pasien di periode ini.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(uiState.uniquePatients) { patient ->
                    PatientReportListItem(patient = patient, onClick = { onPatientClick(patient.uid) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSection(
    uiState: ReportUiState,
    onPeriodSelect: (ReportPeriod) -> Unit,
    onYearSelect: (Int) -> Unit,
    onMonthSelect: (Int) -> Unit
) {
    var periodExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Filter Utama (Dropdown)
        ExposedDropdownMenuBox(
            expanded = periodExpanded,
            onExpandedChange = { periodExpanded = !periodExpanded }
        ) {
            OutlinedTextField(
                value = uiState.selectedPeriod.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Pilih Periode Laporan") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = periodExpanded,
                onDismissRequest = { periodExpanded = false }
            ) {
                ReportPeriod.values().forEach { period ->
                    DropdownMenuItem(
                        text = { Text(period.displayName) },
                        onClick = {
                            onPeriodSelect(period)
                            periodExpanded = false
                        }
                    )
                }
            }
        }

        // Filter Sekunder (Dropdown Tahun & Bulan)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (uiState.selectedPeriod == ReportPeriod.MINGGUAN || uiState.selectedPeriod == ReportPeriod.BULANAN) {
                ExposedDropdownMenuBox(
                    expanded = yearExpanded,
                    onExpandedChange = { yearExpanded = !yearExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = uiState.selectedYear.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tahun") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                        uiState.availableYears.forEach { year ->
                            DropdownMenuItem(text = { Text(year.toString()) }, onClick = { onYearSelect(year); yearExpanded = false })
                        }
                    }
                }
            }

            if (uiState.selectedPeriod == ReportPeriod.MINGGUAN) {
                ExposedDropdownMenuBox(
                    expanded = monthExpanded,
                    onExpandedChange = { monthExpanded = !monthExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = uiState.availableMonths[uiState.selectedMonth],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Bulan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = monthExpanded, onDismissRequest = { monthExpanded = false }) {
                        uiState.availableMonths.forEachIndexed { index, monthName ->
                            DropdownMenuItem(text = { Text(monthName) }, onClick = { onMonthSelect(index); monthExpanded = false })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(IntrinsicSize.Min),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        // PERBAIKAN: Tambahkan .fillMaxSize() agar Column mengisi seluruh kartu
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween // Gunakan SpaceBetween agar ikon di atas dan teks di bawah
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PatientReportListItem(patient: User, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        ListItem(
            headlineContent = { Text(patient.name, fontWeight = FontWeight.Bold) },
            supportingContent = { Text(patient.email) },
            leadingContent = { Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.primary) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
        )
    }
}