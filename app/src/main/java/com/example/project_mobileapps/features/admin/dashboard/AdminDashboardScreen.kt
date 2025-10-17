// Salin dan ganti seluruh isi file: features/admin/dashboard/AdminDashboardScreen.kt

package com.example.project_mobileapps.features.admin.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.local.DailyScheduleData
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.ui.components.PatientStatsChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdminDashboardScreen(
    // Tambahkan parameter NavController untuk navigasi
    onNavigateToSchedule: () -> Unit,
    onNavigateToMonitoring: () -> Unit,
    viewModel: AdminDashboardViewModel = viewModel(
        factory = AdminDashboardViewModelFactory(AppContainer.queueRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 }) // Pager dengan 2 halaman

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Header Sambutan (Tidak Berubah)
        item {
            Text(
                text = "Hello, Admin 👋",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "How are you feeling today?",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Light
            )
        }

        // 2. Kartu Interaktif Utama (Geser Kiri-Kanan)
        item {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> PracticeStatusCard(
                        schedule = uiState.doctorScheduleToday,
                        onNavigateToSchedule = onNavigateToSchedule
                    )
                    1 -> ReportSummaryCard(
                        patientsFinished = uiState.patientsFinished,
                        onNavigateToReports = { /* TODO: Navigasi ke Halaman Laporan */ }
                    )
                }
            }
            // Indikator Pager (titik di bawah kartu)
            Row(
                Modifier
                    .height(20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(8.dp)
                    )
                }
            }
        }

        // 3. Statistik Kunci Harian
        item {
            StatsHeader(
                total = uiState.totalPatientsToday,
                waiting = uiState.patientsWaiting,
                finished = uiState.patientsFinished
            )
        }

        // 4. Ringkasan Antrian & Navigasi Detail
        item {
            Column {
                Text(
                    "Daftar Antrian Aktif",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally))
                } else if (uiState.top5ActiveQueue.isEmpty()) {
                    Text(
                        "Tidak ada pasien aktif dalam antrian.",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else {
                    // Daftar pasien yang sudah difilter dan dibatasi
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.top5ActiveQueue.forEach { queueItem ->
                            AdminPatientInfoCard(item = queueItem)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Tombol "Lihat Semua Detail"
                OutlinedButton(
                    onClick = onNavigateToMonitoring,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Lihat Semua Detail Antrian")
                }
            }
        }
    }
}

// --- KUMPULAN COMPOSABLE BARU DAN YANG DIMODIFIKASI ---

@Composable
fun PracticeStatusCard(
    schedule: DailyScheduleData?,
    onNavigateToSchedule: () -> Unit
) {
    val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date())
    val statusText = if (schedule?.isOpen == true) "BUKA" else "TUTUP"
    val statusColor = if (schedule?.isOpen == true) Color(0xFF4CAF50) else Color.Red
    val scheduleText = if (schedule?.isOpen == true) "${schedule.startTime} - ${schedule.endTime}" else "Praktik Libur"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(currentDate, style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    statusText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
            Text("Jam Praktik: $scheduleText", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onNavigateToSchedule,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.EditCalendar, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Atur Jadwal")
            }
        }
    }
}

@Composable
fun ReportSummaryCard(
    patientsFinished: Int,
    onNavigateToReports: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Ringkasan Laporan", style = MaterialTheme.typography.titleMedium)
            Text(
                patientsFinished.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text("Total Pasien Selesai Hari Ini", style = MaterialTheme.typography.bodyLarge)

            // TODO: Tambahkan filter jika diperlukan di sini

            OutlinedButton(onClick = onNavigateToReports) {
                Icon(Icons.Outlined.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lihat Laporan Lengkap")
            }
        }
    }
}


@Composable
fun StatsHeader(total: Int, waiting: Int, finished: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            label = "Total Pasien",
            value = total.toString(),
            icon = Icons.Outlined.Groups,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Menunggu",
            value = waiting.toString(),
            icon = Icons.Outlined.HourglassTop,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Selesai",
            value = finished.toString(),
            icon = Icons.Outlined.CheckCircle,
            color = Color(0xFF00C853),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun AdminPatientInfoCard(item: QueueItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (item.status) {
                QueueStatus.DILAYANI -> MaterialTheme.colorScheme.primaryContainer
                QueueStatus.DIPANGGIL -> Color(0xFFFFF9C4) // Kuning
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "No. ${item.queueNumber}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(item.userName, style = MaterialTheme.typography.bodyLarge)
            }
            Text(item.status.name, fontWeight = FontWeight.Medium)
        }
    }
}