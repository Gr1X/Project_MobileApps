package com.example.project_mobileapps.features.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.features.admin.manageSchedule.AdminQueueMonitorViewModel
import com.example.project_mobileapps.features.admin.manageSchedule.AdminQueueMonitorViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordInputScreen(
    queueId: String,
    patientName: String,
    queueNumber: String,
    onNavigateBack: () -> Unit,
    onRecordSaved: () -> Unit, // Callback saat sukses
    viewModel: AdminQueueMonitorViewModel = viewModel(
        factory = AdminQueueMonitorViewModelFactory(AppContainer.queueRepository, AuthRepository)
    )
) {
    // State Form
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var bloodPressure by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var physicalExam by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var prescription by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Rekam Medis Pasien", style = MaterialTheme.typography.titleMedium)
                        Text("#$queueNumber - $patientName", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                }
            )
        },
        bottomBar = {
            // Tombol Simpan di Bawah
            Button(
                onClick = {
                    isSubmitting = true
                    viewModel.submitMedicalRecord(
                        queueId = queueId,
                        weight = weight.toDoubleOrNull() ?: 0.0,
                        height = height.toDoubleOrNull() ?: 0.0,
                        bp = bloodPressure,
                        temp = temperature.toDoubleOrNull() ?: 0.0,
                        physical = physicalExam,
                        diagnosis = diagnosis,
                        prescription = prescription,
                        notes = notes,
                        onSuccess = {
                            isSubmitting = false
                            onRecordSaved()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSubmitting && diagnosis.isNotEmpty()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Simpan & Selesai Konsultasi")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // SECTION 1: VITAL SIGNS (Card Putih Bersih)
            MedicalSectionCard(title = "Tanda Vital (Objective)", icon = Icons.Default.MonitorWeight) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = weight, onValueChange = { weight = it },
                        label = { Text("Berat (kg)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = height, onValueChange = { height = it },
                        label = { Text("Tinggi (cm)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = bloodPressure, onValueChange = { bloodPressure = it },
                        label = { Text("Tensi (mmHg)") }, // misal 120/80
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = temperature, onValueChange = { temperature = it },
                        label = { Text("Suhu (°C)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }

            // SECTION 2: PEMERIKSAAN & DIAGNOSA
            MedicalSectionCard(title = "Pemeriksaan & Diagnosa (Assessment)", icon = Icons.Outlined.AssignmentInd) {
                OutlinedTextField(
                    value = physicalExam, onValueChange = { physicalExam = it },
                    label = { Text("Hasil Pemeriksaan Fisik") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("Contoh: Tenggorokan merah, suara parau...") }
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = diagnosis, onValueChange = { diagnosis = it },
                    label = { Text("Diagnosa Utama (Wajib)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = diagnosis.isEmpty(),
                    supportingText = { if(diagnosis.isEmpty()) Text("Diagnosa tidak boleh kosong", color = MaterialTheme.colorScheme.error) }
                )
            }

            // SECTION 3: RESEP & SARAN
            MedicalSectionCard(title = "Rencana Pengobatan (Plan)", icon = Icons.Outlined.Medication) {
                OutlinedTextField(
                    value = prescription, onValueChange = { prescription = it },
                    label = { Text("Resep Obat") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    placeholder = { Text("Contoh:\n- Paracetamol 500mg (3x1)\n- Amoxicillin 500mg (3x1)\n- Vitamin C (1x1)") }
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Catatan / Saran Dokter") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    placeholder = { Text("Contoh: Istirahat cukup, banyak minum air putih, kontrol 3 hari lagi.") }
                )
            }

            // Spacer untuk memberi ruang agar tidak tertutup tombol bottom bar
            Spacer(Modifier.height(40.dp))
        }
    }
}

// Helper Composable untuk Card Section
@Composable
fun MedicalSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        border = null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            content()
        }
    }
}