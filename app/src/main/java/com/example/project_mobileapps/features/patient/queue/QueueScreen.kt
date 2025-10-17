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
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.ui.components.CircularBackButton
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet
import com.example.project_mobileapps.ui.components.QueueChip
import com.example.project_mobileapps.ui.themes.TextSecondary
import kotlinx.coroutines.delay

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
            StatCard(uiState = uiState, displayedWaitTime = displayedWaitTime)
            QueueChipList(uiState = uiState)
        }
    }
}

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .width(IntrinsicSize.Max),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = if (uiState.practiceStatus?.isPracticeOpen == true) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f),
                    ) {
                        val statusText = if (uiState.practiceStatus?.isPracticeOpen == true) "Buka" else "Tutup"
                        Text(
                            text = statusText,
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
                            // --- PERBAIKAN DI SINI ---
                            Text(
                                text = uiState.todaySchedule?.startTime ?: "--:--",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "-",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.height(10.dp)
                            )
                            Text(
                                text = uiState.todaySchedule?.endTime ?: "--:--",
                                fontWeight = FontWeight.Bold
                            )
                            // -------------------------
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (uiState.myQueueItem != null) "Nomor Antrian Anda" else "Total Antrian Hari Ini",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                    )
                    Text(
                        text = if (uiState.myQueueItem != null) "${uiState.myQueueItem?.queueNumber}" else "${uiState.activeQueueCount}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Sisa ${uiState.availableSlots} slot",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }

            if (uiState.myQueueItem != null) {
                OutlinedButton(
                    onClick = onCancelQueue,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Text("Batalkan")
                }
            } else {
                Button(
                    onClick = onTakeQueue,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = uiState.practiceStatus?.isPracticeOpen == true && uiState.availableSlots > 0
                ) {
                    Text("Ambil Nomor Antrian")
                }
            }
        }
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