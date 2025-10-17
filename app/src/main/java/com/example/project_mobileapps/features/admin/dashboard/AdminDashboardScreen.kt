// Salin dan ganti seluruh isi file: features/admin/dashboard/AdminDashboardScreen.kt

package com.example.project_mobileapps.features.admin.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.local.DailyScheduleData
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.di.AppContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    onNavigateToSchedule: () -> Unit,
    onNavigateToMonitoring: () -> Unit,
    onNavigateToReports: () -> Unit,
    viewModel: AdminDashboardViewModel = viewModel(
        factory = AdminDashboardViewModelFactory(AppContainer.queueRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header Sambutan
        item {
            Text( "Hello, Admin 👋", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("How are you feeling today?", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Light)
        }

        item {
            PracticeStatusCard(
                practiceStatus = uiState.practiceStatus,
                schedule = uiState.doctorScheduleToday,
                onNavigateToSchedule = onNavigateToSchedule
            )
        }

        // Statistik Harian
        item {
            StatsHeader(
                total = uiState.totalPatientsToday,
                waiting = uiState.patientsWaiting,
                finished = uiState.patientsFinished
            )
        }

        // Daftar Antrian Aktif
        item {
            Column {
                Text("Daftar Antrian Aktif", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally))
                } else if (uiState.top5ActiveQueue.isEmpty()) {
                    Text("Tidak ada pasien aktif dalam antrian.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.top5ActiveQueue.forEach { queueItem -> AdminPatientInfoCard(item = queueItem) }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onNavigateToMonitoring, modifier = Modifier.fillMaxWidth()) {
                    Text("Lihat Semua Detail Antrian")
                }
            }
        }
    }
}

@Composable
fun PracticeStatusCard(
    practiceStatus: PracticeStatus?,
    schedule: DailyScheduleData?,
    onNavigateToSchedule: () -> Unit
) {
    val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date())
    val isPracticeOpen = practiceStatus?.isPracticeOpen ?: false
    val statusText = if (isPracticeOpen) "BUKA" else "TUTUP"
    val statusColor = if (isPracticeOpen) Color(0xFF00C853) else Color.Red
    val scheduleText = if (schedule != null && schedule.isOpen) "${schedule.startTime} - ${schedule.endTime}" else "Praktik Libur"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(currentDate, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(statusText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = statusColor)
            }
            Text("Jam Praktik: $scheduleText", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onNavigateToSchedule, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.EditCalendar, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Atur Jadwal")
            }
        }
    }
}

@Composable
fun StatsHeader(total: Int, waiting: Int, finished: Int) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            label = "Total Pasien",
            value = total.toString(),
            icon = Icons.Outlined.Groups,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        StatCard(
            label = "Menunggu",
            value = waiting.toString(),
            icon = Icons.Outlined.HourglassTop,
            color = Color(0xFFFFA000), // Oranye
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        StatCard(
            label = "Selesai",
            value = finished.toString(),
            icon = Icons.Outlined.CheckCircle,
            color = Color(0xFF00C853), // Hijau
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            // INI KODE YANG BENAR
            horizontalAlignment = Alignment.CenterHorizontally, // Untuk perataan kiri-kanan
            verticalArrangement = Arrangement.Center // Untuk perataan atas-bawah
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
// File: features/admin/dashboard/AdminDashboardScreen.kt

@Composable
fun AdminPatientInfoCard(item: QueueItem) {
    // Tentukan warna dan border berdasarkan status
    val cardColors: CardColors
    val cardBorder: BorderStroke?

    when (item.status) {
        QueueStatus.DILAYANI -> {
            cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            cardBorder = null
        }
        QueueStatus.DIPANGGIL -> {
            cardColors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4))
            cardBorder = null
        }
        else -> { // Status MENUNGGU dan lainnya
            cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        }
    }

    // Tentukan warna teks agar selalu kontras
    val contentColor = when (item.status) {
        QueueStatus.DILAYANI -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val secondaryContentColor = when (item.status) {
        QueueStatus.DILAYANI -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }


    Card(
        elevation = CardDefaults.cardElevation(if (cardBorder == null) 2.dp else 0.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors,
        border = cardBorder // Terapkan border jika ada
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kolom untuk Nomor dan Nama (diberi weight agar fleksibel)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "No. ${item.queueNumber}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    item.userName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = secondaryContentColor
                )
            }

            // Teks status di luar kolom agar tidak terdorong
            Text(
                text = item.status.name,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1 // Pastikan tidak akan pernah turun baris
            )
        }
    }
}