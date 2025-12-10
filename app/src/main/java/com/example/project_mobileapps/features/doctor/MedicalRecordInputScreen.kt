// File: features/doctor/MedicalRecordInputScreen.kt
package com.example.project_mobileapps.features.doctor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.features.admin.manageSchedule.AdminQueueMonitorViewModel
import com.example.project_mobileapps.features.admin.manageSchedule.AdminQueueMonitorViewModelFactory
// [PERBAIKAN] Ganti ConfirmationDialog dengan ConfirmationBottomSheet
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet
import com.example.project_mobileapps.ui.components.ToastManager
import com.example.project_mobileapps.ui.components.ToastType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordInputScreen(
    queueId: String,
    patientName: String,
    queueNumber: String,
    onNavigateBack: () -> Unit,
    onRecordSaved: () -> Unit,
    viewModel: AdminQueueMonitorViewModel = viewModel(
        factory = AdminQueueMonitorViewModelFactory(AppContainer.queueRepository, AuthRepository)
    )
) {
    // --- STATE FORM DATA ---
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var bloodPressure by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }

    var physicalExam by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var treatment by remember { mutableStateOf("") }
    var prescription by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // --- STATE UI ---
    var isSubmitting by remember { mutableStateOf(false) }
    var showConfirmSheet by remember { mutableStateOf(false) } // Ubah nama variabel

    // Validasi Error State
    var isDiagnosisError by remember { mutableStateOf(false) }
    var isTreatmentError by remember { mutableStateOf(false) }

    fun validateAndSubmit() {
        isDiagnosisError = diagnosis.isBlank()
        isTreatmentError = treatment.isBlank()

        if (isDiagnosisError || isTreatmentError) {
            ToastManager.showToast("Mohon lengkapi Diagnosa dan Tindakan", ToastType.ERROR)
            return
        }
        showConfirmSheet = true // Trigger BottomSheet
    }

    // [PERBAIKAN] Gunakan ConfirmationBottomSheet
    if (showConfirmSheet) {
        ConfirmationBottomSheet(
            onDismiss = { showConfirmSheet = false },
            onConfirm = {
                showConfirmSheet = false
                isSubmitting = true
                viewModel.submitMedicalRecord(
                    queueId = queueId,
                    weight = weight.toDoubleOrNull() ?: 0.0,
                    height = height.toDoubleOrNull() ?: 0.0,
                    bp = bloodPressure,
                    temp = temperature.toDoubleOrNull() ?: 0.0,
                    physical = physicalExam,
                    diagnosis = diagnosis,
                    treatment = treatment,
                    prescription = prescription,
                    notes = notes,
                    onSuccess = {
                        isSubmitting = false
                        onRecordSaved()
                    }
                )
            },
            title = "Simpan Rekam Medis?",
            text = "Pastikan data medis untuk pasien $patientName sudah benar. Data tidak dapat diubah setelah disimpan."
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Input Rekam Medis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("#$queueNumber - $patientName", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 16.dp) {
                Button(
                    onClick = { validateAndSubmit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Simpan & Selesai")
                    }
                }
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        // ... (SISA KODE UI CARD INPUT TETAP SAMA SEPERTI SEBELUMNYA) ...
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ... (Copy Paste bagian InputSectionCard dari jawaban sebelumnya di sini) ...

            // --- SECTION 1: OBJECTIVE (VITAL SIGNS) ---
            InputSectionCard(title = "Tanda Vital (Objective)", icon = Icons.Default.MonitorWeight, color = Color(0xFF2196F3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MedicalInput(value = weight, onValueChange = { weight = it }, label = "Berat", suffix = "kg", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    MedicalInput(value = height, onValueChange = { height = it }, label = "Tinggi", suffix = "cm", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MedicalInput(value = bloodPressure, onValueChange = { bloodPressure = it }, label = "Tensi", suffix = "mmHg", modifier = Modifier.weight(1f))
                    MedicalInput(value = temperature, onValueChange = { temperature = it }, label = "Suhu", suffix = "°C", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                }
            }

            // --- SECTION 2: ASSESSMENT (DIAGNOSIS) ---
            InputSectionCard(title = "Pemeriksaan & Diagnosa (Assessment)", icon = Icons.Outlined.AssignmentInd, color = Color(0xFFE91E63)) {
                MedicalInput(value = physicalExam, onValueChange = { physicalExam = it }, label = "Pemeriksaan Fisik", placeholder = "Misal: Tenggorokan merah...", singleLine = false, minLines = 2)
                Spacer(Modifier.height(12.dp))
                MedicalInput(
                    value = diagnosis, onValueChange = { diagnosis = it; isDiagnosisError = false },
                    label = "Diagnosa Utama (Wajib)", placeholder = "Misal: ISPA, Hipertensi...",
                    isError = isDiagnosisError, errorMessage = "Diagnosa tidak boleh kosong", singleLine = false
                )
            }

            // --- SECTION 3: PLAN (TREATMENT & DRUGS) ---
            InputSectionCard(title = "Rencana Pengobatan (Plan)", icon = Icons.Outlined.Medication, color = Color(0xFF4CAF50)) {
                MedicalInput(
                    value = treatment, onValueChange = { treatment = it; isTreatmentError = false },
                    label = "Tindakan Medis (Wajib)", placeholder = "Misal: Nebulizer, Jahit luka...",
                    isError = isTreatmentError, errorMessage = "Tindakan harus diisi", singleLine = false
                )
                Spacer(Modifier.height(12.dp))
                MedicalInput(value = prescription, onValueChange = { prescription = it }, label = "Resep Obat", placeholder = "R/ Paracetamol...", singleLine = false, minLines = 3)
                Spacer(Modifier.height(12.dp))
                MedicalInput(value = notes, onValueChange = { notes = it }, label = "Saran / Catatan Dokter", placeholder = "Istirahat cukup...", singleLine = false, minLines = 2)
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ... (Helper Composable InputSectionCard dan MedicalInput tetap sama) ...
@Composable
fun InputSectionCard(title: String, icon: ImageVector, color: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
                }
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            }
            Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFEEF2F6))
            content()
        }
    }
}

@Composable
fun MedicalInput(
    value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    suffix: String? = null, placeholder: String? = null, keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true, minLines: Int = 1, isError: Boolean = false, errorMessage: String? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            label = { Text(label) },
            suffix = if (suffix != null) { { Text(suffix) } } else null,
            placeholder = if (placeholder != null) { { Text(placeholder, color = Color.LightGray) } } else null,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            singleLine = singleLine, minLines = minLines, isError = isError,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color(0xFFE0E0E0), focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
        )
        if (isError && errorMessage != null) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }
    }
}