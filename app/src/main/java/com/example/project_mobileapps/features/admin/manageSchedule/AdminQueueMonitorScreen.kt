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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Groups
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
import coil.compose.AsyncImage
import com.example.project_mobileapps.R
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.model.Role
import com.example.project_mobileapps.features.doctor.DetailRow
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet
import com.example.project_mobileapps.ui.components.ConfirmationDialog
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
    viewModel: AdminQueueMonitorViewModel, // <-- Tipe ViewModel diubah
    currentUserRole: Role?
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var patientToCancel by remember { mutableStateOf<PatientQueueDetails?>(null) }
    var activeConfirmation by remember { mutableStateOf<ConfirmationAction?>(null) }
    var patientToView by remember { mutableStateOf<PatientQueueDetails?>(null) }

    activeConfirmation?.let { action ->
        when (action) {
            is ConfirmationAction.Cancel -> {
                ConfirmationBottomSheet(
                    onDismiss = { activeConfirmation = null },
                    onConfirm = {
                        viewModel.cancelPatientQueue(action.patient, context)
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
                        viewModel.callNextPatient(context)
                        activeConfirmation = null
                    },
                    title = "Panggil Pasien Berikutnya?",
                    text = "Anda akan memanggil pasien berikutnya dalam antrian. Pastikan ruang periksa sudah siap."
                )
            }
            is ConfirmationAction.Arrive -> {
                ConfirmationBottomSheet(
                    onDismiss = { activeConfirmation = null },
                    onConfirm = {
                        viewModel.confirmPatientArrival(action.patient.queueItem.queueNumber, context)
                        activeConfirmation = null
                    },
                    title = "Konfirmasi Kehadiran?",
                    text = "Konfirmasi bahwa pasien No. ${action.patient.queueItem.queueNumber} (${action.patient.queueItem.userName}) telah hadir di ruang periksa."
                )
            }
            is ConfirmationAction.Finish -> {
                ConfirmationBottomSheet(
                    onDismiss = { activeConfirmation = null },
                    onConfirm = {
                        viewModel.finishConsultation(action.patient.queueItem.queueNumber, context)
                        activeConfirmation = null
                    },
                    title = "Selesaikan Konsultasi?",
                    text = "Anda akan menyelesaikan sesi konsultasi untuk pasien No. ${action.patient.queueItem.queueNumber}. Tindakan ini tidak dapat diurungkan."
                )
            }
        }
    }

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

    if (patientToCancel != null) {
        ConfirmationDialog(
            onDismiss = { patientToCancel = null },
            onConfirm = {
                patientToCancel?.let { viewModel.cancelPatientQueue(it, context) }
                patientToCancel = null
            },
            title = "Batalkan Antrian?",
            text = "Anda yakin ingin membatalkan antrian untuk pasien '${patientToCancel?.queueItem?.userName}'?"
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { TopStatsSection(uiState = uiState) }
        item { MainActionSection(uiState = uiState, onAction = { activeConfirmation = it }, currentUserRole = currentUserRole) }

        item {
            Column {
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Daftar Semua Antrian", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                FilterDropdownButton(
                    options = uiState.filterOptions,
                    selectedOption = uiState.selectedFilter,
                    onOptionSelected = { viewModel.filterByStatus(it) }
                )
            }
        }

        if (uiState.isLoading) {
            item { CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)) }
        } else if (uiState.fullQueueList.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxSize().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    Text("Tidak ada antrian untuk filter ini.")
                }
            }
        } else {
            items(uiState.fullQueueList, key = { it.queueItem.queueNumber }) { patientDetails ->
                PatientQueueRow(
                    patientDetails = patientDetails,
                    onClick = { patientToView = patientDetails }
                )
            }
        }
    }
}

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
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
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
                        ) {
                            Text("Selesai Konsultasi")
                        }
                    }
                }
            )
        }
        // KONDISI 2: Sudah memanggil, menunggu pasien datang
        uiState.patientCalled != null -> {
            PatientActionProfileCard(
                title = "Telah Dipanggil",
                patientDetails = uiState.patientCalled,
                actionContent = {
                    var timeRemaining by remember { mutableStateOf("01:00") }
                    // LaunchedEffect ini sudah benar, tidak perlu diubah
                    LaunchedEffect(uiState.patientCalled.queueItem.calledAt) {
                        val deadline = (uiState.patientCalled.queueItem.calledAt?.time ?: 0) + (1 * 60 * 1000)
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
                    Text(timeRemaining, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    // ✅ PERBAIKAN DI SINI
                    Button(
                        onClick = { onAction(ConfirmationAction.Arrive(uiState.patientCalled)) }, // Gunakan onAction
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pasien Hadir")
                    }
                }
            )
        }
        // KONDISI 3: Siap memanggil pasien berikutnya
        uiState.nextInLine != null -> {
            PatientActionProfileCard(
                title = "Pasien Berikutnya",
                patientDetails = uiState.nextInLine,
                actionContent = {
                    // ✅ PERBAIKAN DI SINI
                    Button(
                        onClick = { onAction(ConfirmationAction.CallNext) }, // Gunakan onAction
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Panggil Pasien")
                    }
                }
            )
        }
        // KONDISI 4: Tidak ada antrian sama sekali
        else -> {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Semua antrian telah selesai.",
                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))

            ListItem(
                headlineContent = { Text(patientDetails.queueItem.userName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                supportingContent = { Text("Keluhan: ${patientDetails.queueItem.keluhan}") },
                leadingContent = {
                    AsyncImage(
                        model = patientDetails.user?.profilePictureUrl,
                        contentDescription = "Foto Profil",
                        placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                        error = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(CircleShape)
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = actionContent
            )
        }
    }
}

@Composable
fun PatientQueueRow(
    patientDetails: PatientQueueDetails,
    onClick: () -> Unit // Hanya butuh satu aksi klik
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // Seluruh kartu bisa diklik
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "No. ${patientDetails.queueItem.queueNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = patientDetails.user?.name ?: patientDetails.queueItem.userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            StatusChip(status = patientDetails.queueItem.status)
            // Tombol hapus sudah tidak ada di sini
        }
    }
}


@Composable
fun StatusChip(status: QueueStatus) {
    val (text, color) = when (status) {
        QueueStatus.MENUNGGU -> "Menunggu" to MaterialTheme.colorScheme.tertiaryContainer
        QueueStatus.DIPANGGIL -> "Dipanggil" to Color(0xFFFFF9C4) // Kuning
        QueueStatus.DILAYANI -> "Dilayani" to MaterialTheme.colorScheme.primaryContainer
        QueueStatus.SELESAI -> "Selesai" to MaterialTheme.colorScheme.secondaryContainer
        QueueStatus.DIBATALKAN -> "Batal" to MaterialTheme.colorScheme.errorContainer
    }
    Text(
        text = text,
        modifier = Modifier
            .clip(CircleShape)
            .background(color) // Memberi warna latar belakang pada chip
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge
    )
}


@Composable
fun FilterDropdownButton(
    options: List<QueueStatus>,
    selectedOption: QueueStatus?,
    onOptionSelected: (QueueStatus?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = selectedOption?.name?.lowercase()?.replaceFirstChar { it.titlecase() } ?: "Semua"

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Filter: $selectedText")
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Buka Filter")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            // Pilihan "Semua"
            DropdownMenuItem(
                text = { Text("Semua") },
                onClick = {
                    onOptionSelected(null)
                    expanded = false
                }
            )
            // Pilihan filter lainnya
            options.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.name.lowercase().replaceFirstChar { it.titlecase() }) },
                    onClick = {
                        onOptionSelected(status)
                        expanded = false
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailBottomSheet(
    patientDetails: PatientQueueDetails,
    onDismiss: () -> Unit,
    onCancelClick: () -> Unit // <-- Parameter baru
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            // Bagian Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Detail Pasien",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Detail Info
            DetailRow(label = "Nama Lengkap", value = patientDetails.user?.name ?: patientDetails.queueItem.userName)
            DetailRow(label = "Nomor Antrian", value = "#${patientDetails.queueItem.queueNumber}")
            DetailRow(label = "Keluhan Awal", value = patientDetails.queueItem.keluhan)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            DetailRow(label = "Nomor Telepon", value = patientDetails.user?.phoneNumber ?: "N/A")
            DetailRow(label = "Email", value = patientDetails.user?.email ?: "N/A")

            Spacer(modifier = Modifier.height(24.dp))

            // Tombol Aksi
            // Hanya tampilkan tombol batal jika statusnya masih MENUNGGU atau DIPANGGIL
            if (patientDetails.queueItem.status == QueueStatus.MENUNGGU || patientDetails.queueItem.status == QueueStatus.DIPANGGIL) {
                OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Batalkan Antrian")
                }
            }
        }
    }
}

@Composable
private fun ServiceTimeSelector(
    currentTime: Int,
    onTimeChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Estimasi Waktu Layanan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Per Pasien", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { onTimeChange(-1) }, enabled = currentTime > 5) {
                Icon(Icons.Default.Remove, "Kurangi")
            }
            Text("$currentTime min", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = { onTimeChange(1) }, enabled = currentTime < 60) {
                Icon(Icons.Default.Add, "Tambah")
            }
        }
    }
}