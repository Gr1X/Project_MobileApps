// File: features/admin/manageSchedule/ManageScheduleScreen.kt
package com.example.project_mobileapps.features.admin.manageSchedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.ui.components.ConfirmationDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScheduleScreen(viewModel: ManageScheduleViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var patientToCancel by remember { mutableStateOf<PatientQueueDetails?>(null) }

    if (patientToCancel != null) {
        ConfirmationDialog(
            onDismiss = { patientToCancel = null },
            onConfirm = {
                patientToCancel?.let {
                    viewModel.cancelPatientQueue(it, context)
                }
                patientToCancel = null
            },
            title = "Batalkan Antrian?",
            text = "Anda yakin ingin membatalkan antrian untuk pasien '${patientToCancel?.user?.name ?: patientToCancel?.queueItem?.userName}'?"
        )
    }

    if (uiState.selectedPatient != null) {
        PatientDetailBottomSheet(
            patientDetails = uiState.selectedPatient!!,
            onDismiss = { viewModel.clearSelectedPatient() }
        )
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Pantauan Antrian",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            FilterDropdownButton(
                options = uiState.filterOptions,
                selectedOption = uiState.selectedFilter,
                onOptionSelected = { status ->
                    viewModel.filterByStatus(status)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (uiState.patientQueueList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada antrian untuk filter ini.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.patientQueueList) { patientDetails ->
                        PatientQueueRow(
                            patientDetails = patientDetails,
                            onClick = { viewModel.selectPatient(patientDetails) },
                            onCancelClick = { patientToCancel = patientDetails }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PatientQueueRow(
    patientDetails: PatientQueueDetails,
    onClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "No. ${patientDetails.queueItem.queueNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    patientDetails.user?.name ?: patientDetails.queueItem.userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            StatusChip(status = patientDetails.queueItem.status)

            if (patientDetails.queueItem.status == QueueStatus.MENUNGGU) {
                IconButton(onClick = onCancelClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Batalkan Antrian",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge
    )
}

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