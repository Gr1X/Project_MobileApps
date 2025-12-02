// File: features/doctor/DoctorDashboardScreen.kt
package com.example.project_mobileapps.features.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.navigation.NavHostController
import com.example.project_mobileapps.data.model.DailyScheduleData
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.features.admin.manageSchedule.PatientQueueDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
/**
 * Composable utama untuk layar Dashboard Dokter.
 * Ini adalah layar stateful yang mengamati [DoctorViewModel].
 *
 * @param viewModel ViewModel [DoctorViewModel] yang menyediakan [DoctorUiState].
 * @param navController Kontroler navigasi (saat ini tidak digunakan di layar ini,
 * tapi bagus untuk dimiliki jika ada navigasi keluar dari dashboard).
 */
@Composable
fun DoctorDashboardScreen(
    viewModel: DoctorViewModel,
    navController: NavHostController
) {
    val uiState by viewModel.uiState.collectAsState()
    /**
     * Logika untuk menampilkan [PatientDetailBottomSheet].
     * Jika [uiState.selectedPatient] tidak null, BottomSheet akan ditampilkan.
     * `onDismiss` akan memanggil ViewModel untuk meng-clear state.
     */
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
        item {
            DoctorDashboardHeader(uiState = uiState)
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("3 Antrian Teratas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        if (uiState.isLoading) {
            item { CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)) }
        } else if (uiState.topQueueList.isEmpty()) {
            item {
                Text(
                    "Belum ada pasien dalam antrian.",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(uiState.topQueueList, key = { it.queueItem.queueNumber }) { patientDetails ->
                SimplePatientInfoCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    patientDetails = patientDetails,
                    onClick = { viewModel.selectPatient(patientDetails) }
                )
            }
        }
    }
}
/**
 * Composable helper untuk bagian header dashboard.
 * @param uiState State UI saat ini.
 */

@Composable
fun DoctorDashboardHeader(uiState: DoctorUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp)
    ) {
        Text(
            text = "${uiState.greeting},",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "${uiState.doctorName} 👋",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(24.dp))

        PracticeStatusCard(practiceStatus = uiState.practiceStatus, schedule = uiState.todaySchedule)

        Spacer(Modifier.height(24.dp))

        StatsHeader(
            nextQueue = uiState.nextQueueNumber,
            waiting = uiState.waitingInQueue
        )
    }
}
/**
 * Composable helper untuk menampilkan dua kartu statistik.
 * @param nextQueue Nomor antrian berikutnya (String, bisa "-").
 * @param waiting Jumlah total antrian (Int).
 */
@Composable
fun StatsHeader(nextQueue: String, waiting: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(
            label = "Pasien Selanjutnya",
            value = nextQueue,
            icon = Icons.Outlined.ConfirmationNumber,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Total Antrian",
            value = waiting.toString(),
            icon = Icons.Outlined.Groups,
            modifier = Modifier.weight(1f)
        )
    }
}
/**
 * Composable helper (reusable) untuk satu kartu statistik.
 * @param label Teks label (misal: "Total Antrian").
 * @param value Teks nilai (misal: "5").
 * @param icon Ikon untuk ditampilkan.
 * @param modifier Modifier.
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    // Menggunakan Card standar dengan warna Surface (putih)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Beri sedikit bayangan
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ikon menggunakan warna Primary (biru) sebagai aksen
            Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            // Teks menggunakan warna teks standar
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
/**
 * Composable helper (reusable) untuk menampilkan info pasien di daftar antrian.
 * @param patientDetails Data detail pasien dan antriannya.
 * @param onClick Aksi yang dijalankan saat kartu diklik.
 * @param modifier Modifier.
 */
@Composable
fun SimplePatientInfoCard(
    patientDetails: PatientQueueDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "No. ${patientDetails.queueItem.queueNumber}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(patientDetails.queueItem.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Keluhan: ${patientDetails.queueItem.keluhan}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            StatusChip(status = patientDetails.queueItem.status)
        }
    }
}
/**
 * Composable helper (reusable) untuk menampilkan chip status berwarna.
 * @param status Enum [QueueStatus] yang akan ditampilkan.
 */
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

// Tambahkan kode ini di bagian bawah file DoctorDashboardScreen.kt
/**
 * Composable untuk Bottom Sheet yang menampilkan detail info pasien.
 * @param patientDetails Data lengkap pasien yang dipilih.
 * @param onDismiss Callback saat bottom sheet ditutup.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailBottomSheet(
    patientDetails: PatientQueueDetails,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile Picture",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                patientDetails.user?.name ?: "N/A",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Detail Info
            DetailRow(label = "Nomor Telepon", value = patientDetails.user?.phoneNumber ?: "N/A")
            DetailRow(label = "Email", value = patientDetails.user?.email ?: "N/A")
            DetailRow(label = "Jenis Kelamin", value = patientDetails.user?.gender?.name ?: "N/A")
            DetailRow(label = "Tanggal Lahir", value = patientDetails.user?.dateOfBirth ?: "N/A")
        }
    }
}
/**
 * Composable helper (reusable) untuk baris detail di bottom sheet.
 * @param label Label (misal: "Email").
 * @param value Nilai (misal: "pasien@gmail.com").
 */
@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
/**
 * Composable helper (reusable) untuk kartu status praktik.
 * Menampilkan BUKA/TUTUP, tanggal, dan jam praktik.
 * @param practiceStatus Objek [PracticeStatus] dari state.
 * @param schedule Objek [DailyScheduleData] dari state.
 */
@Composable
fun PracticeStatusCard(practiceStatus: PracticeStatus?, schedule: DailyScheduleData?) {
    val isPracticeOpen = practiceStatus?.isPracticeOpen ?: false
    val statusText = if (isPracticeOpen) "BUKA" else "TUTUP"
    val statusColor = if (isPracticeOpen) Color(0xFF00C853) else MaterialTheme.colorScheme.error
    val jamPraktik = if (schedule != null && schedule.isOpen) "${schedule.startTime} - ${schedule.endTime}" else "Tidak ada jadwal hari ini"
    val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(currentDate, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status: ", style = MaterialTheme.typography.titleMedium)
                Text(statusText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = statusColor)
            }
            Text("Jam Praktik: $jamPraktik", style = MaterialTheme.typography.bodyMedium)
        }
    }
}