package com.example.project_mobileapps.features.patient.queue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.ui.components.CircularBackButton
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet
import com.example.project_mobileapps.ui.components.QueueChip
import com.example.project_mobileapps.ui.themes.TextSecondary
import kotlinx.coroutines.delay
import java.util.Date
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    queueViewModel: QueueViewModel = viewModel(factory = QueueViewModelFactory(AppContainer.queueRepository, AuthRepository)),
    onBackToHome: () -> Unit,
    onNavigateToTakeQueue: () -> Unit
) {
    val uiState by queueViewModel.uiState.collectAsState()
    var showCancelSheet by remember { mutableStateOf(false) }

    // ✅ 1. State variable to hold the real-time timer value
    var displayedWaitTime by remember { mutableStateOf(uiState.estimatedWaitTime) }

    // ✅ 2. Effect to run the timer
    LaunchedEffect(key1 = uiState.estimatedWaitTime) {
        displayedWaitTime = uiState.estimatedWaitTime
        if (displayedWaitTime > 0) {
            while (displayedWaitTime > 0) {
                delay(60_000L) // Wait for 1 minute
                displayedWaitTime--
            }
        }
    }

    if (showCancelSheet) {
        ConfirmationBottomSheet(
            onDismiss = { showCancelSheet = false },
            onConfirm = {
                queueViewModel.cancelMyQueue()
                showCancelSheet = false
            },
            title = "Konfirmasi Pembatalan",
            text = "Apakah Anda yakin ingin membatalkan antrian ini? Tindakan ini tidak dapat diurungkan."
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Status Antrian") },
                navigationIcon = { CircularBackButton(onClick = onBackToHome) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QueueInfoCard(
                uiState = uiState,
                onTakeQueue = onNavigateToTakeQueue,
                onCancelQueue = { showCancelSheet = true }
            )

            if (uiState.myQueueItem?.status == QueueStatus.MENUNGGU) {
                StatCard(uiState = uiState, displayedWaitTime = displayedWaitTime)
            }
            QueueChipList(uiState = uiState)
        }
    }
}

// --- PERBAIKAN UTAMA DI FUNGSI INI ---
@Composable
private fun QueueInfoCard(
    uiState: QueueUiState,
    onTakeQueue: () -> Unit,
    onCancelQueue: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bagian Atas Kartu (Info Buka/Tutup & Nomor Antrian)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Info Jadwal
                Column(
                    modifier = Modifier.weight(1f).width(IntrinsicSize.Max),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = if (uiState.practiceStatus?.isPracticeOpen == true) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f),
                    ) {
                        Text(
                            text = if (uiState.practiceStatus?.isPracticeOpen == true) "Buka" else "Tutup",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFC107).copy(alpha = 0.2f),
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = uiState.todaySchedule?.startTime ?: "--:--", fontWeight = FontWeight.Bold)
                            Text("-", fontWeight = FontWeight.Bold, modifier = Modifier.height(10.dp))
                            Text(text = uiState.todaySchedule?.endTime ?: "--:--", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Info Nomor Antrian
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (uiState.myQueueItem != null) "Nomor Antrian Anda" else "Total Antrian Hari Ini",
                        style = MaterialTheme.typography.bodyLarge, color = TextSecondary,
                    )
                    Text(
                        text = if (uiState.myQueueItem != null) "${uiState.myQueueItem?.queueNumber}" else "${uiState.activeQueueCount}",
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Sisa ${uiState.availableSlots} slot",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextSecondary
                    )
                }
            }

            // Bagian Bawah Kartu (Tombol Aksi atau Timer)
            when (uiState.myQueueItem?.status) {
                QueueStatus.MENUNGGU -> {
                    OutlinedButton(
                        onClick = onCancelQueue,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) { Text("Batalkan") }
                }
                QueueStatus.DIPANGGIL -> {
                    val calledAt = uiState.myQueueItem?.calledAt
                    if (calledAt != null) {
                        CountdownTimer(calledAt = calledAt)
                    }
                }
                QueueStatus.DILAYANI -> {
                    val startedAt = uiState.myQueueItem?.startedAt
                    if (startedAt != null) {
                        StopwatchTimer(startedAt = startedAt)
                    }
                }
                else -> { // null atau status lain
                    Button(
                        onClick = onTakeQueue,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = uiState.practiceStatus?.isPracticeOpen == true && uiState.availableSlots > 0
                    ) { Text("Ambil Nomor Antrian") }
                }
            }
        }
    }
}

@Composable
private fun CountdownTimer(calledAt: Date) {
    val COUNTDOWN_DURATION_MS = 15 * 60 * 1000 // 15 menit
    var timeRemainingString by remember { mutableStateOf("--:--") }

    LaunchedEffect(calledAt) {
        val deadline = calledAt.time + COUNTDOWN_DURATION_MS
        while (true) {
            val remaining = deadline - System.currentTimeMillis()
            if (remaining > 0) {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
                timeRemainingString = String.format("%02d:%02d", minutes, seconds)
            } else {
                timeRemainingString = "Waktu Habis"
                break
            }
            delay(1000L)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Sisa Waktu Kedatangan:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Text(timeRemainingString, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun StopwatchTimer(startedAt: Date) {
    var elapsedTimeString by remember { mutableStateOf("00:00") }

    LaunchedEffect(startedAt) {
        while (true) {
            val diff = System.currentTimeMillis() - startedAt.time
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
            elapsedTimeString = String.format("%02d:%02d", minutes, seconds)
            delay(1000L)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Durasi Konsultasi Berjalan:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Text(elapsedTimeString, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
    }
}

@Composable
private fun StatCard(
    uiState: QueueUiState,
    displayedWaitTime: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val totalWaiting = uiState.queuesAhead
            StatItem(value = "$totalWaiting Orang", label = "Di Depan Anda")
            StatItem(value = "$displayedWaitTime Menit", label = "Estimasi")
        }
    }
}

@Composable
private fun QueueChipList(uiState: QueueUiState) {
    if (uiState.upcomingQueues.isEmpty()) {
        Text(
            "Belum ada antrian untuk hari ini.",
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            textAlign = TextAlign.Center,
            color = TextSecondary
        )
    } else {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            itemsIndexed(uiState.upcomingQueues) { index, queueItem ->
                QueueChip(
                    queueItem = queueItem,
                    isFirstInLine = (index == 0)
                )
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}