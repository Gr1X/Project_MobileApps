// File: features/admin/manageSchedule/AdminQueueMonitorScreen.kt
package com.example.project_mobileapps.features.admin.manageSchedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.project_mobileapps.R
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.model.Role
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet
import com.example.project_mobileapps.ui.components.QrScannerScreen
import kotlinx.coroutines.delay
import java.util.Date
import java.util.concurrent.TimeUnit

sealed class ConfirmationAction {
    data class Finish(val patient: PatientQueueDetails) : ConfirmationAction()
    data class Arrive(val patient: PatientQueueDetails) : ConfirmationAction()
    object CallNext : ConfirmationAction()
    data class Cancel(val patient: PatientQueueDetails) : ConfirmationAction()
}

@Composable
fun AdminQueueMonitorScreen(
    viewModel: AdminQueueMonitorViewModel,
    currentUserRole: Role?
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showScanner by remember { mutableStateOf(false) }
    // State baru untuk mencegah double scan
    var isProcessingQr by remember { mutableStateOf(false) }

    var activeConfirmation by remember { mutableStateOf<ConfirmationAction?>(null) }
    var patientToView by remember { mutableStateOf<PatientQueueDetails?>(null) }

    // --- DIALOG SCANNER ---
    if (showScanner) {
        Dialog(
            onDismissRequest = { showScanner = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                QrScannerScreen(
                    onQrCodeScanned = { scannedId ->
                        // LOGIKA PENGUNCI (DEBOUNCE)
                        // Hanya proses jika belum ada proses berjalan
                        if (!isProcessingQr) {
                            isProcessingQr = true // Kunci segera!
                            showScanner = false   // Tutup dialog
                            viewModel.processQrCode(scannedId)
                        }
                    }
                )
                IconButton(
                    onClick = { showScanner = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, "Tutup", tint = Color.White)
                }
                Text(
                    text = "Arahkan kamera ke QR Code Pasien",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    // --- BOTTOM SHEET KONFIRMASI ---
    activeConfirmation?.let { action ->
        when (action) {
            is ConfirmationAction.Cancel -> {
                ConfirmationBottomSheet(
                    onDismiss = { activeConfirmation = null },
                    onConfirm = {
                        viewModel.cancelPatientQueue(action.patient)
                        activeConfirmation = null
                    },
                    title = "Batalkan Antrian?",
                    text = "Anda yakin ingin membatalkan antrian untuk pasien '${action.patient.queueItem.userName}'?"
                )
            }
            is ConfirmationAction.CallNext -> {
                ConfirmationBottomSheet(
                    onDismiss = { activeConfirmation = null },
                    onConfirm = {
                        viewModel.callNextPatient()
                        activeConfirmation = null
                    },
                    title = "Panggil Pasien Berikutnya?",
                    text = "Anda akan memanggil pasien berikutnya dalam antrian."
                )
            }
            is ConfirmationAction.Arrive -> {
                ConfirmationBottomSheet(
                    onDismiss = { activeConfirmation = null },
                    onConfirm = {
                        viewModel.confirmPatientArrival(action.patient.queueItem.queueNumber)
                        activeConfirmation = null
                    },
                    title = "Konfirmasi Kehadiran?",
                    text = "Konfirmasi pasien No. ${action.patient.queueItem.queueNumber} telah hadir?"
                )
            }
            is ConfirmationAction.Finish -> {
                ConfirmationBottomSheet(
                    onDismiss = { activeConfirmation = null },
                    onConfirm = {
                        viewModel.finishConsultation(action.patient.queueItem.queueNumber)
                        activeConfirmation = null
                    },
                    title = "Selesaikan Konsultasi?",
                    text = "Selesaikan sesi konsultasi pasien No. ${action.patient.queueItem.queueNumber}?"
                )
            }
        }
    }

    // --- BOTTOM SHEET DETAIL PASIEN ---
    if (patientToView != null) {
        PatientDetailBottomSheet(
            patientDetails = patientToView!!,
            onDismiss = { patientToView = null },
            onCancelClick = {
                activeConfirmation = ConfirmationAction.Cancel(patientToView!!)
                patientToView = null
            }
        )
    }

    // --- MAIN UI ---
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    // Saat membuka scanner, reset status pengunci agar bisa scan lagi
                    isProcessingQr = false
                    showScanner = true
                },
                icon = { Icon(Icons.Outlined.QrCodeScanner, contentDescription = null) },
                text = { Text("Scan Kehadiran") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->

        // HANYA ADA SATU LAZY COLUMN DI SINI
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // 1. Statistik Atas
            item { TopStatsSection(uiState = uiState) }

            // 2. Kartu Aksi Utama (Panggil/Layani)
            item { MainActionSection(uiState = uiState, onAction = { activeConfirmation = it }, currentUserRole = currentUserRole) }

            // 3. Filter & List Header
            item {
                Column {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Daftar Semua Antrian", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    FilterDropdownButton(
                        options = uiState.filterOptions,
                        selectedOption = uiState.selectedFilter,
                        onOptionSelected = { viewModel.filterByStatus(it) }
                    )
                }
            }

            // 4. List Antrian
            if (uiState.isLoading) {
                item { CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)) }
            } else if (uiState.fullQueueList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Text("Tidak ada antrian untuk filter ini.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(uiState.fullQueueList, key = { it.queueItem.id }) { patientDetails ->
                    PatientQueueRow(
                        patientDetails = patientDetails,
                        onClick = { patientToView = patientDetails }
                    )
                }
            }
        }
    }
}

// --- BAGIAN BAWAH TETAP SAMA (Components) ---

@Composable
fun TopStatsSection(uiState: DoctorQueueUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val servingNumber = uiState.currentlyServing?.queueItem?.queueNumber?.toString() ?: "-"
        val nextNumber = uiState.nextInLine?.queueItem?.queueNumber?.toString() ?: "-"

        SmallStatCard(label = "Sedang Dilayani", value = servingNumber, icon = Icons.Outlined.AccessTime, modifier = Modifier.weight(1f))
        SmallStatCard(label = "Total Antrian", value = uiState.totalWaitingCount.toString(), icon = Icons.Outlined.Groups, modifier = Modifier.weight(1f))
        SmallStatCard(label = "Antrian Berikutnya", value = nextNumber, icon = Icons.Outlined.ConfirmationNumber, modifier = Modifier.weight(1f))
    }
}

@Composable
fun SmallStatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, lineHeight = 12.sp)
        }
    }
}

@Composable
fun MainActionSection(
    uiState: DoctorQueueUiState,
    onAction: (ConfirmationAction) -> Unit,
    currentUserRole: Role?
) {
    when {
        uiState.currentlyServing != null -> {
            PatientActionProfileCard(
                title = "Sedang Melayani",
                patientDetails = uiState.currentlyServing,
                actionContent = {
                    var consultationTime by remember { mutableStateOf("00:00") }
                    LaunchedEffect(uiState.currentlyServing.queueItem.startedAt) {
                        while (true) {
                            val diff = Date().time - (uiState.currentlyServing.queueItem.startedAt?.time ?: 0)
                            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
                            consultationTime = String.format("%02d:%02d", minutes, seconds)
                            delay(1000L)
                        }
                    }
                    Text("Waktu Konsultasi:", style = MaterialTheme.typography.labelMedium)
                    Text(consultationTime, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (currentUserRole == Role.DOKTER) {
                        Button(
                            onClick = { onAction(ConfirmationAction.Finish(uiState.currentlyServing)) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Selesai Konsultasi") }
                    }
                }
            )
        }
        uiState.patientCalled != null -> {
            PatientActionProfileCard(
                title = "Telah Dipanggil",
                patientDetails = uiState.patientCalled,
                actionContent = {
                    var timeRemaining by remember { mutableStateOf("01:00") }
                    LaunchedEffect(uiState.patientCalled.queueItem.calledAt) {
                        val deadline = (uiState.patientCalled.queueItem.calledAt?.time ?: 0) + (15 * 60 * 1000) // 15 menit
                        while (true) {
                            val remaining = deadline - Date().time
                            if (remaining > 0) {
                                val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining)
                                val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
                                timeRemaining = String.format("%02d:%02d", minutes, seconds)
                            } else {
                                timeRemaining = "Waktu Habis"
                                break
                            }
                            delay(1000L)
                        }
                    }
                    Text("Sisa Waktu Kedatangan:", style = MaterialTheme.typography.labelMedium)
                    Text(timeRemaining, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onAction(ConfirmationAction.Arrive(uiState.patientCalled)) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Pasien Hadir") }
                }
            )
        }
        uiState.nextInLine != null -> {
            PatientActionProfileCard(
                title = "Pasien Berikutnya",
                patientDetails = uiState.nextInLine,
                actionContent = {
                    Button(
                        onClick = { onAction(ConfirmationAction.CallNext) },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) { Text("Panggil Pasien") }
                }
            )
        }
        else -> {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Text("Tidak ada antrian menunggu.", modifier = Modifier.padding(24.dp).fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun PatientActionProfileCard(
    title: String,
    patientDetails: PatientQueueDetails,
    actionContent: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            ListItem(
                headlineContent = { Text(patientDetails.queueItem.userName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                supportingContent = { Text("Keluhan: ${patientDetails.queueItem.keluhan}") },
                leadingContent = {
                    AsyncImage(
                        model = patientDetails.user?.profilePictureUrl,
                        contentDescription = null,
                        placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                        error = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(CircleShape)
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, content = actionContent)
        }
    }
}

@Composable
fun PatientQueueRow(patientDetails: PatientQueueDetails, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("No. ${patientDetails.queueItem.queueNumber}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(text = patientDetails.user?.name ?: patientDetails.queueItem.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            StatusChip(status = patientDetails.queueItem.status)
        }
    }
}

@Composable
fun StatusChip(status: QueueStatus) {
    val (text, color, textColor) = when (status) {
        QueueStatus.MENUNGGU -> Triple("Menunggu", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        QueueStatus.DIPANGGIL -> Triple("Dipanggil", Color(0xFFFFF9C4), Color.Black)
        QueueStatus.DILAYANI -> Triple("Dilayani", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        QueueStatus.SELESAI -> Triple("Selesai", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        QueueStatus.DIBATALKAN -> Triple("Batal", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    }
    Text(text = text, modifier = Modifier.clip(CircleShape).background(color).padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge, color = textColor)
}

@Composable
fun FilterDropdownButton(options: List<QueueStatus>, selectedOption: QueueStatus?, onOptionSelected: (QueueStatus?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = selectedOption?.name?.lowercase()?.replaceFirstChar { it.titlecase() } ?: "Semua"
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Filter: $selectedText")
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth()) {
            DropdownMenuItem(text = { Text("Semua") }, onClick = { onOptionSelected(null); expanded = false })
            options.forEach { status ->
                DropdownMenuItem(text = { Text(status.name.lowercase().replaceFirstChar { it.titlecase() }) }, onClick = { onOptionSelected(status); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailBottomSheet(patientDetails: PatientQueueDetails, onDismiss: () -> Unit, onCancelClick: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Detail Pasien", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            DetailRow(label = "Nama Lengkap", value = patientDetails.user?.name ?: patientDetails.queueItem.userName)
            DetailRow(label = "Nomor Antrian", value = "#${patientDetails.queueItem.queueNumber}")
            DetailRow(label = "Keluhan Awal", value = patientDetails.queueItem.keluhan)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            DetailRow(label = "Nomor Telepon", value = patientDetails.user?.phoneNumber ?: "N/A")

            Spacer(modifier = Modifier.height(24.dp))
            if (patientDetails.queueItem.status == QueueStatus.MENUNGGU || patientDetails.queueItem.status == QueueStatus.DIPANGGIL) {
                OutlinedButton(onClick = onCancelClick, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error), border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Batalkan Antrian")
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}