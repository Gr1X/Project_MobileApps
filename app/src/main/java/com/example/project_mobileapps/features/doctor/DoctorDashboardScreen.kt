package com.example.project_mobileapps.features.doctor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.di.AppContainer
import kotlinx.coroutines.delay
import java.util.Date
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDashboardScreen(
    onLogoutClick: () -> Unit,
    viewModel: DoctorViewModel = viewModel(
        factory = DoctorViewModelFactory(AppContainer.queueRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Dokter") },
                actions = {
                    TextButton(onClick = onLogoutClick) {
                        Text("Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            PracticeStatusCard(
                status = uiState.practiceStatus,
                onTogglePractice = { viewModel.togglePracticeStatus() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.callNextPatient() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.practiceStatus?.isPracticeOpen ?: false
            ) {
                Text("Panggil Pasien Berikutnya")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Daftar Antrian Hari Ini", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (uiState.queueList.isEmpty()) {
                Text("Belum ada pasien dalam antrian.", modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                // ===============================================
                // PERBAIKI BAGIAN INI
                // ===============================================
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.queueList) { queueItem ->
                        PatientQueueCard(
                            item = queueItem,
                            onConfirmArrival = viewModel::confirmArrival,
                            onFinishConsultation = viewModel::finishConsultation
                        )
                    }
                }
                // ===============================================
            }
        }
    }
}

@Composable
fun PracticeStatusCard(status: com.example.project_mobileapps.data.model.PracticeStatus?, onTogglePractice: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Status Praktik", style = MaterialTheme.typography.labelMedium)
                val statusText = if (status?.isPracticeOpen == true) "Buka" else "Tutup"
                val statusColor = if (status?.isPracticeOpen == true) Color(0xFF4CAF50) else Color.Red
                Text(statusText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = statusColor)
                Text("Sedang Dilayani: No. ${status?.currentServingNumber ?: 0}")
                Text("Total Dilayani: ${status?.totalServed ?: 0} pasien")
            }
            Button(onClick = onTogglePractice) {
                val buttonText = if (status?.isPracticeOpen == true) "Tutup Praktik" else "Buka Praktik"
                Text(buttonText)
            }
        }
    }
}

@Composable
fun PatientQueueCard(
    item: QueueItem,
    onConfirmArrival: (Int) -> Unit,
    onFinishConsultation: (Int) -> Unit
) {
    var consultationTime by remember { mutableStateOf("00:00") }
    var isTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(item.status, item.startedAt) {
        isTimerRunning = item.status == QueueStatus.DILAYANI && item.startedAt != null
        if (isTimerRunning) {
            while (isTimerRunning) {
                val diff = Date().time - (item.startedAt?.time ?: 0)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
                consultationTime = String.format("%02d:%02d", minutes, seconds)
                delay(1000L)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (item.status) {
                QueueStatus.DILAYANI -> MaterialTheme.colorScheme.primaryContainer
                QueueStatus.SELESAI -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "No. ${item.queueNumber}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(item.userName, style = MaterialTheme.typography.bodyLarge)
                }
                Text(item.status.name, fontWeight = FontWeight.Medium)
            }

            // Spacer untuk memberi jarak
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
                            onClick = {
                                isTimerRunning = false
                                onFinishConsultation(item.queueNumber)
                            },
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