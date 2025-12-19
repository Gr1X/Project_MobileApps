package com.example.project_mobileapps.features.doctor

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
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
import com.example.project_mobileapps.data.repo.FirestoreQueueRepository
import com.example.project_mobileapps.features.doctor.components.PatientHistorySheet
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet
import com.example.project_mobileapps.ui.components.ToastManager
import com.example.project_mobileapps.ui.components.ToastType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordInputScreen(
    queueId: String,
    patientName: String,
    queueNumber: String,
    patientId: String, // [PENTING] Pastikan ID Pasien (userId) dikirim ke sini untuk load history
    onNavigateBack: () -> Unit,
    onRecordSaved: () -> Unit,
    // [PERBAIKAN] Gunakan MedicalRecordViewModel yang BARU
    viewModel: MedicalRecordViewModel = viewModel(
        factory = MedicalRecordViewModelFactory(FirestoreQueueRepository)
    )
) {
    // START DEBUG CHECK
    LaunchedEffect(Unit) {
        if (queueId.isBlank()) {
            // Jika ID kosong, tampilkan error dan kembalikan.
            Log.e("MedRecordInput", "FATAL CRASH: queueId is blank or empty!")
            ToastManager.showToast("Kesalahan data: ID antrian tidak ditemukan.", ToastType.ERROR)
            onNavigateBack() // Kembali agar tidak crash
        } else {
            Log.d("MedRecordInput", "Queue ID yang diterima: $queueId")
        }
    }

    // Ambil UI State dari ViewModel Baru
    val uiState by viewModel.uiState.collectAsState()

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
    var showConfirmSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) } // State untuk History

    // Dropdown Obat
    var expandedObat by remember { mutableStateOf(false) }
    var searchObat by remember { mutableStateOf("") }

    // Validasi Error State
    var isDiagnosisError by remember { mutableStateOf(false) }
    var isTreatmentError by remember { mutableStateOf(false) }

    // Efek Error dari ViewModel
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            ToastManager.showToast(it, ToastType.ERROR)
        }
    }

    fun validateAndSubmit() {
        isDiagnosisError = diagnosis.isBlank()
        isTreatmentError = treatment.isBlank()

        if (isDiagnosisError || isTreatmentError) {
            ToastManager.showToast("Mohon lengkapi Diagnosa dan Tindakan", ToastType.ERROR)
            return
        }
        showConfirmSheet = true
    }

    // KONFIRMASI SIMPAN
    if (showConfirmSheet) {
        ConfirmationBottomSheet(
            onDismiss = { showConfirmSheet = false },
            onConfirm = {
                showConfirmSheet = false

                // Siapkan Data Map
                val dataMap = mapOf(
                    "weightKg" to (weight.toDoubleOrNull() ?: 0.0),
                    "heightCm" to (height.toDoubleOrNull() ?: 0.0),
                    "bloodPressure" to bloodPressure,
                    "temperature" to (temperature.toDoubleOrNull() ?: 0.0),
                    "physicalExam" to physicalExam,
                    "diagnosis" to diagnosis,
                    "treatment" to treatment,
                    "prescription" to prescription,
                    "doctorNotes" to notes
                )

                // Panggil ViewModel Submit
                viewModel.submitMedicalRecord(queueId, dataMap) {
                    ToastManager.showToast("✅ Rekam Medis Tersimpan", ToastType.SUCCESS)
                    onRecordSaved()
                }
            },
            title = "Simpan Rekam Medis?",
            text = "Pastikan data medis untuk pasien $patientName sudah benar."
        )
    }

    // SHEET HISTORY PASIEN
    if (showHistorySheet) {
        PatientHistorySheet(
            history = uiState.patientHistory,
            isLoading = uiState.isHistoryLoading,
            onDismiss = { showHistorySheet = false }
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    // TOMBOL LIHAT RIWAYAT (BARU)
                    IconButton(onClick = {
                        viewModel.loadPatientHistory(patientId) // Load data history
                        showHistorySheet = true
                    }) {
                        Icon(Icons.Default.History, "Riwayat Pasien")
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
                    enabled = !uiState.isSubmitting
                ) {
                    if (uiState.isSubmitting) {
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
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

                // --- DROPDOWN OBAT (MENGGUNAKAN DATA DARI VIEWMODEL) ---
                ExposedDropdownMenuBox(
                    expanded = expandedObat,
                    onExpandedChange = { expandedObat = !expandedObat }
                ) {
                    OutlinedTextField(
                        value = searchObat,
                        onValueChange = { searchObat = it; expandedObat = true },
                        label = { Text("Cari & Tambah Obat") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedObat) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    // Filter Obat dari UI State ViewModel
                    val filteredOptions = uiState.medicines.filter {
                        it.name.contains(searchObat, ignoreCase = true)
                    }

                    if (filteredOptions.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expandedObat,
                            onDismissRequest = { expandedObat = false }
                        ) {
                            filteredOptions.forEach { obat ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(obat.name, fontWeight = FontWeight.Bold)
                                            Text("${obat.category} • ${obat.form}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    },
                                    onClick = {
                                        // Auto-Fill Text
                                        val textToAdd = "- ${obat.name} (..x..)"
                                        prescription = if (prescription.isBlank()) textToAdd else "$prescription\n$textToAdd"

                                        searchObat = ""
                                        expandedObat = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // TextField Resep Utama
                OutlinedTextField(
                    value = prescription,
                    onValueChange = { prescription = it },
                    label = { Text("Daftar Resep Obat (Final)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("Pilih obat di atas atau ketik manual...") },
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                )

                Spacer(Modifier.height(12.dp))
                MedicalInput(value = notes, onValueChange = { notes = it }, label = "Saran / Catatan Dokter", placeholder = "Istirahat cukup...", singleLine = false, minLines = 2)
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ... (InputSectionCard dan MedicalInput Helper tetap sama seperti sebelumnya) ...
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
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFEEF2F6))
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