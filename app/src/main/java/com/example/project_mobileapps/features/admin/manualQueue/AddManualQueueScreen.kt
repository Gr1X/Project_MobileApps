package com.example.project_mobileapps.features.admin.manualQueue

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.model.Gender
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet
import java.util.*

/**
 * Composable untuk layar penambahan antrian manual oleh Admin.
 * Layar ini memiliki dua mode utama:
 * 1. Mencari dan memilih pasien yang sudah terdaftar.
 * 2. Mendaftarkan pasien baru jika tidak ditemukan dalam pencarian.
 *
 * @param onNavigateBack Callback untuk kembali ke layar sebelumnya.
 * @param viewModel ViewModel [AddManualQueueViewModel] yang mengelola state dan logika layar ini.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualQueueScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddManualQueueViewModel = viewModel(
        factory = AddManualQueueViewModelFactory(AuthRepository, AppContainer.queueRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var complaint by remember { mutableStateOf("") }
    val context = LocalContext.current
    var showConfirmationSheet by remember { mutableStateOf(false) }

    // Logika untuk menentukan mode UI saat ini.
    // Mode "Pasien Baru" aktif jika user sudah mengetik query, tidak ada hasil, dan proses pencarian sudah selesai.
    val isNewPatientMode = uiState.selectedUser == null && uiState.searchQuery.isNotBlank() && uiState.searchResults.isEmpty() && !uiState.isSearching
    // Mode "Pasien Dipilih" aktif jika user sudah memilih pasien dari hasil pencarian.
    val isPatientSelected = uiState.selectedUser != null

    // Tombol utama diaktifkan hanya jika pasien sudah dipilih (atau dalam mode baru) DAN keluhan sudah diisi.
    val isButtonEnabled = (isPatientSelected && complaint.isNotBlank()) || (isNewPatientMode && complaint.isNotBlank())

    if (showConfirmationSheet) {
        ConfirmationBottomSheet(
            onDismiss = { showConfirmationSheet = false },
            onConfirm = {
                showConfirmationSheet = false
                // Callback generik untuk menangani hasil dari ViewModel.
                val onResultCallback: (Result<*>) -> Unit = { result ->
                    if (result.isSuccess) {
                        Toast.makeText(context, "Pasien berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    } else {
                        Toast.makeText(context, result.exceptionOrNull()?.message ?: "Gagal", Toast.LENGTH_LONG).show()
                    }
                }

                if (isNewPatientMode) {
                    viewModel.registerNewPatientAndAddQueue(complaint, onResultCallback)
                } else {
                    viewModel.addQueueForSelectedUser(complaint, onResultCallback)
                }
            },
            title = "Konfirmasi Tambah Antrian?",
            text = "Pastikan semua data yang dimasukkan sudah benar sebelum melanjutkan."
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Antrian Manual") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") } }
            )
        },
        bottomBar = {
            // Bottom bar untuk tombol aksi utama agar selalu terlihat.
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                Button(
                    onClick = { showConfirmationSheet = true },
                    enabled = isButtonEnabled,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(50.dp)
                ) {
                    Text(if (isNewPatientMode) "Daftar & Tambah Antrian" else "Tambah ke Antrian")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Kartu untuk input data pasien.
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("1. Data Pasien", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            label = { Text("Ketik Nama Pasien...") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Outlined.Search, "Cari") },
                            singleLine = true,
                            enabled = !isPatientSelected, // Nonaktifkan search bar jika pasien sudah dipilih.
                            trailingIcon = { if(isPatientSelected) { IconButton(onClick = viewModel::clearSelection) { Icon(Icons.Outlined.Clear, "Hapus Pilihan") } } }
                        )
                        // Menampilkan form/hasil pencarian secara dinamis dengan animasi.
                        AnimatedVisibility(visible = !isPatientSelected && uiState.searchQuery.isNotBlank()) {
                            if (isNewPatientMode) NewPatientForm(uiState = uiState, viewModel = viewModel)
                            else SearchResults(results = uiState.searchResults, onUserClick = viewModel::onUserSelected)
                        }
                        // Menampilkan info pasien yang dipilih.
                        AnimatedVisibility(visible = isPatientSelected) {
                            uiState.selectedUser?.let { SelectedPatientInfo(user = it) }
                        }
                    }
                }
            }

            // Kartu untuk input keluhan.
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("2. Detail Keluhan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = complaint,
                            onValueChange = { complaint = it },
                            label = { Text("Keluhan Awal Pasien*") },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Menampilkan daftar hasil pencarian pasien.
 */
@Composable
private fun SearchResults(results: List<User>, onUserClick: (User) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(results) { user ->
            ListItem(
                headlineContent = { Text(user.name, fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text(user.email, style = MaterialTheme.typography.bodySmall) },
                leadingContent = { Icon(Icons.Outlined.Person, contentDescription = null) },
                modifier = Modifier.clickable { onUserClick(user) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

/**
 * Menampilkan formulir pendaftaran untuk pasien baru.
 * Muncul secara dinamis ketika hasil pencarian kosong.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewPatientForm(uiState: AddManualQueueUiState, viewModel: AddManualQueueViewModel) {
    val context = LocalContext.current
    var isGenderMenuExpanded by remember { mutableStateOf(false) }

    // Konfigurasi DatePickerDialog.
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            val formattedDate = "$year-${String.format("%02d", month + 1)}-${String.format("%02d", day)}"
            viewModel.onNewPatientDobChange(formattedDate)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pasien tidak ditemukan. Daftarkan sebagai pasien baru:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Input fields dengan penanganan error dari ViewModel.
        OutlinedTextField(
            value = uiState.newPatientName, onValueChange = viewModel::onNewPatientNameChange,
            label = { Text("Nama Lengkap*") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            isError = uiState.nameError != null,
            supportingText = { if (uiState.nameError != null) Text(uiState.nameError) }
        )
        OutlinedTextField(
            value = uiState.newPatientEmail, onValueChange = viewModel::onNewPatientEmailChange,
            label = { Text("Email*") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = uiState.emailError != null,
            supportingText = { if (uiState.emailError != null) Text(uiState.emailError) }
        )
        OutlinedTextField(
            value = uiState.newPatientPhone, onValueChange = viewModel::onNewPatientPhoneChange,
            label = { Text("Nomor Telepon*") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = uiState.phoneError != null,
            supportingText = { if (uiState.phoneError != null) Text(uiState.phoneError) }
        )
        // Input tanggal lahir yang menampilkan dialog saat diklik.
        Box(modifier = Modifier.clickable { datePickerDialog.show() }) {
            OutlinedTextField(
                value = uiState.newPatientDob, onValueChange = {},
                label = { Text("Tanggal Lahir (Opsional)") }, modifier = Modifier.fillMaxWidth(),
                enabled = false, trailingIcon = { Icon(Icons.Outlined.CalendarToday, "Pilih Tanggal") }, // Dinonaktifkan agar hanya bisa diisi melalui dialog.
                colors = OutlinedTextFieldDefaults.colors( // Kustomisasi warna untuk state disabled.
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // Dropdown untuk memilih jenis kelamin.
        ExposedDropdownMenuBox(expanded = isGenderMenuExpanded, onExpandedChange = { isGenderMenuExpanded = !isGenderMenuExpanded }) {
            OutlinedTextField(
                value = uiState.newPatientGender.name, onValueChange = {}, readOnly = true,
                label = { Text("Jenis Kelamin") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGenderMenuExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = isGenderMenuExpanded, onDismissRequest = { isGenderMenuExpanded = false }) {
                Gender.values().forEach { genderOption ->
                    DropdownMenuItem(text = { Text(genderOption.name) }, onClick = { viewModel.onNewPatientGenderChange(genderOption); isGenderMenuExpanded = false })
                }
            }
        }
    }
}

/**
 * Menampilkan informasi ringkas dari pasien yang telah dipilih dari hasil pencarian.
 */
@Composable
private fun SelectedPatientInfo(user: User) {
    ListItem(
        headlineContent = { Text(user.name, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(user.email, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { Icon(Icons.Outlined.CheckCircle, contentDescription = "Pasien Terpilih", tint = MaterialTheme.colorScheme.primary) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    )
}