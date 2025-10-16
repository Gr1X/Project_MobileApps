package com.example.project_mobileapps.features.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.features.admin.manageSchedule.PatientDetailBottomSheet
import com.example.project_mobileapps.features.admin.manageSchedule.PatientQueueDetails
import kotlinx.coroutines.delay
import java.util.Date
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDashboardScreen(
    viewModel: DoctorViewModel,
    navController: NavHostController
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Tampilkan BottomSheet jika ada pasien yang dipilih di ViewModel
    if (uiState.selectedPatient != null) {
        PatientDetailBottomSheet(
            patientDetails = uiState.selectedPatient!!,
            onDismiss = { viewModel.clearSelectedPatient() }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- BAGIAN HEADER ---
        item {
            DoctorDashboardHeader(
                uiState = uiState,
                // Beri aksi kosong karena logout sudah ada di drawer
                onLogoutClick = {}
            )
        }

        // --- BAGIAN AKSI UTAMA ---
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Button(
                    onClick = { viewModel.callNextPatient(context) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.practiceStatus?.isPracticeOpen ?: false && uiState.queueList.any { it.queueItem.status == QueueStatus.MENUNGGU }
                ) {
                    Text("Panggil Pasien Berikutnya")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.togglePracticeStatus() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val buttonText = if (uiState.practiceStatus?.isPracticeOpen == true) "Tutup Praktik" else "Buka Praktik"
                    Text(buttonText)
                }
            }
        }

        // --- BAGIAN DAFTAR ANTRIAN ---
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Divider(modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(16.dp))
                Text("Daftar Antrian Hari Ini", style = MaterialTheme.typography.titleMedium)
            }
        }

        if (uiState.isLoading) {
            item { CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)) }
        } else if (uiState.queueList.isEmpty()) {
            item {
                Text(
                    "Belum ada pasien dalam antrian.",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(uiState.queueList, key = { it.queueItem.queueNumber }) { patientDetails ->
                PatientQueueCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    item = patientDetails.queueItem,
                    onClick = { viewModel.selectPatient(patientDetails) }, // <-- Tambahkan aksi klik
                    onConfirmArrival = { viewModel.confirmArrival(it) },
                    onFinishConsultation = { viewModel.finishConsultation(it) }
                )
            }
        }
    }
}

@Composable
fun DoctorDashboardHeader(
    uiState: DoctorUiState,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${uiState.greeting},\n${uiState.doctorName} 👋",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Tombol logout ini sekarang tidak terlihat, tapi kita biarkan untuk masa depan
            // Jika Anda ingin menampilkannya lagi, cukup hapus komentar di bawah
            // TextButton(onClick = onLogoutClick) {
            //     Text("Logout")
            // }
        }
        Spacer(Modifier.height(16.dp))
        StatsHeader(
            total = uiState.totalPatientsToday,
            waiting = uiState.patientsWaiting,
            finished = uiState.patientsFinished
        )
    }
}

// =======================================================
// PASTE FUNGSI STATSHEADER DAN STATCARD YANG HILANG DI SINI
// =======================================================
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
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Menunggu",
            value = waiting.toString(),
            icon = Icons.Outlined.HourglassTop,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Selesai",
            value = finished.toString(),
            icon = Icons.Outlined.CheckCircle,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall)
        }
    }
}
// =======================================================

// =======================================================
// PERBAIKI PARAMETER PATIENTQUEUECARD
// =======================================================
@Composable
fun PatientQueueCard(
    item: QueueItem,
    onClick: () -> Unit,
    onConfirmArrival: (Int) -> Unit,
    onFinishConsultation: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // ... (Isi fungsi ini tidak berubah, hanya signature/parameternya)
    var consultationTime by remember { mutableStateOf("00:00") }

    LaunchedEffect(item.status, item.startedAt) {
        if (item.status == QueueStatus.DILAYANI && item.startedAt != null) {
            while (true) {
                val diff = Date().time - (item.startedAt?.time ?: 0)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
                consultationTime = String.format("%02d:%02d", minutes, seconds)
                delay(1000L)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "No. ${item.queueNumber}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Keluhan: ${item.keluhan}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(status = item.status)
            }

            if (item.status == QueueStatus.DIPANGGIL || item.status == QueueStatus.DILAYANI) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            when (item.status) {
                QueueStatus.DIPANGGIL -> {
                    Button(
                        onClick = { onConfirmArrival(item.queueNumber) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pasien Hadir")
                    }
                }
                QueueStatus.DILAYANI -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Waktu Konsultasi:", style = MaterialTheme.typography.labelMedium)
                        Text(
                            consultationTime,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onFinishConsultation(item.queueNumber) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Selesai Konsultasi")
                        }
                    }
                }
                else -> { /* Tidak ada aksi untuk status lain */ }
            }
        }
    }
}

@Composable
fun StatusChip(status: QueueStatus) {
    val (text, backgroundColor, contentColor) = when (status) {
        QueueStatus.MENUNGGU -> Triple("Menunggu", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        QueueStatus.DIPANGGIL -> Triple("Dipanggil", Color(0xFFFFF9C4), Color.Black) // Kuning
        QueueStatus.DILAYANI -> Triple("Dilayani", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        QueueStatus.SELESAI -> Triple("Selesai", Color(0xFFC8E6C9), Color.Black) // Hijau muda
        QueueStatus.DIBATALKAN -> Triple("Batal", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    }
    Text(
        text = text,
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        color = contentColor
    )
}