package com.example.project_mobileapps.features.doctor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.features.admin.manageSchedule.AdminQueueMonitorViewModel
import com.example.project_mobileapps.features.admin.manageSchedule.AdminQueueMonitorViewModelFactory
import com.example.project_mobileapps.ui.components.ToastManager
import com.example.project_mobileapps.ui.components.ToastType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationInputScreen(
    queueNumber: Int,
    patientName: String,
    onNavigateBack: () -> Unit,
    onConsultationFinished: () -> Unit,
    viewModel: AdminQueueMonitorViewModel = viewModel(
        factory = AdminQueueMonitorViewModelFactory(AppContainer.queueRepository, AuthRepository)
    )
) {
    var diagnosis by remember { mutableStateOf("") }
    var treatment by remember { mutableStateOf("") }
    var prescription by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Validasi sederhana
    val isFormValid = diagnosis.isNotBlank() && treatment.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rekam Medis Pasien") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Pasien
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Pasien No. $queueNumber", style = MaterialTheme.typography.labelMedium)
                    Text(patientName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }

            Text("Silakan lengkapi data medis sebelum menyelesaikan sesi.", style = MaterialTheme.typography.bodyMedium)

            // Form Input
            OutlinedTextField(
                value = diagnosis, onValueChange = { diagnosis = it },
                label = { Text("Diagnosa (Wajib)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = treatment, onValueChange = { treatment = it },
                label = { Text("Tindakan Medis (Wajib)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = prescription, onValueChange = { prescription = it },
                label = { Text("Resep Obat") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = { Text("Contoh: Paracetamol 500mg 3x1\nAmoxicillin 500mg 3x1") }
            )

            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Catatan Tambahan / Saran") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    isSubmitting = true
                    viewModel.finishConsultationWithData(
                        queueNumber, diagnosis, treatment, prescription, notes,
                        onSuccess = {
                            isSubmitting = false
                            onConsultationFinished()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = isFormValid && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Simpan & Selesai")
                }
            }
        }
    }
}