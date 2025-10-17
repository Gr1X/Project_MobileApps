package com.example.project_mobileapps.features.admin.manualQueue

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.model.Gender
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.features.admin.manualQueue.AddManualQueueUiState
import com.example.project_mobileapps.features.admin.manualQueue.AddManualQueueViewModel
import com.example.project_mobileapps.features.admin.manualQueue.AddManualQueueViewModelFactory
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()

    val isNewPatientMode = uiState.selectedUser == null && uiState.searchQuery.isNotBlank() && uiState.searchResults.isEmpty() && !uiState.isSearching
    val isSelectedPatientMode = uiState.selectedUser != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Antrian Manual") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        val isNewPatientMode = uiState.selectedUser == null && uiState.searchQuery.isNotBlank() && uiState.searchResults.isEmpty()
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
                    enabled = (uiState.selectedUser != null) || (uiState.searchQuery.isNotBlank() && uiState.searchResults.isEmpty() && uiState.newPatientName.isNotBlank()),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(if (uiState.searchQuery.isNotBlank() && uiState.searchResults.isEmpty() && uiState.selectedUser == null) "Daftar & Tambah Antrian" else "Tambah ke Antrian")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Step 1: Search
            StepIndicator(
                stepNumber = "1",
                title = "Cari Pasien",
                isComplete = isSelectedPatientMode
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    label = { Text("Ketik Nama Pasien...") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Outlined.Search, "Cari") },
                    singleLine = true,
                    enabled = !isSelectedPatientMode // Disable search after selecting a user
                )
                AnimatedVisibility(visible = uiState.isSearching) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }

            // Animated container for search results or new patient form
            AnimatedContent(
                targetState = when {
                    isSelectedPatientMode -> "SELECTED"
                    isNewPatientMode -> "NEW_FORM"
                    uiState.searchResults.isNotEmpty() -> "RESULTS"
                    else -> "EMPTY"
                },
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                }, label = ""
            ) { state ->
                when (state) {
                    "SELECTED" -> SelectedPatientInfo(user = uiState.selectedUser!!)
                    "RESULTS" -> SearchResults(
                        results = uiState.searchResults,
                        onUserClick = viewModel::onUserSelected
                    )
                    "NEW_FORM" -> NewPatientForm(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    "EMPTY" -> {
                        // Show nothing or an instructional text
                    }
                }
            }

            // Step 2: Complaint
            StepIndicator(
                stepNumber = "2",
                title = "Detail Antrian",
                isComplete = complaint.isNotBlank()
            )
            OutlinedTextField(
                value = complaint,
                onValueChange = { complaint = it },
                label = { Text("Keluhan Awal (Opsional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(horizontal = 16.dp),
                leadingIcon = { Icon(Icons.Outlined.Notes, "Keluhan") }
            )
            Spacer(modifier = Modifier.height(100.dp)) // Extra space at the bottom
        }
    }
}

@Composable
private fun StepIndicator(stepNumber: String, title: String, isComplete: Boolean) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    if (isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isComplete) {
                Icon(Icons.Default.Check, contentDescription = "Selesai", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            } else {
                Text(stepNumber, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SearchResults(results: List<com.example.project_mobileapps.data.model.User>, onUserClick: (com.example.project_mobileapps.data.model.User) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column {
            results.forEach { user ->
                ListItem(
                    headlineContent = { Text(user.name) },
                    supportingContent = { Text(user.email) },
                    leadingContent = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    modifier = Modifier.clickable { onUserClick(user) }
                )
                if (results.last() != user) {
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

// ... (import yang ada) ...

// GANTI Composable NewPatientForm dengan yang ini
// GANTI Composable NewPatientForm dengan yang ini
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewPatientForm(uiState: AddManualQueueUiState, viewModel: AddManualQueueViewModel) {
    var isGenderMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pasien tidak ditemukan. Silakan daftarkan sebagai pasien baru.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = uiState.newPatientName,
            onValueChange = viewModel::onNewPatientNameChange,
            label = { Text("Nama Lengkap") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.Person, "Nama Pasien") },
            singleLine = true
        )
        OutlinedTextField(
            value = uiState.newPatientEmail,
            onValueChange = viewModel::onNewPatientEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.Email, "Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        // --- TAMBAHKAN INPUT BARU DI SINI ---
        OutlinedTextField(
            value = uiState.newPatientDob,
            onValueChange = viewModel::onNewPatientDobChange,
            label = { Text("Tgl. Lahir (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.Cake, "Tanggal Lahir") },
            singleLine = true
        )
        ExposedDropdownMenuBox(
            expanded = isGenderMenuExpanded,
            onExpandedChange = { isGenderMenuExpanded = !isGenderMenuExpanded }
        ) {
            OutlinedTextField(
                value = uiState.newPatientGender.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Jenis Kelamin") },
                leadingIcon = { Icon(Icons.Outlined.Wc, "Jenis Kelamin") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGenderMenuExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = isGenderMenuExpanded,
                onDismissRequest = { isGenderMenuExpanded = false }
            ) {
                Gender.values().forEach { genderOption ->
                    DropdownMenuItem(
                        text = { Text(genderOption.name) },
                        onClick = {
                            viewModel.onNewPatientGenderChange(genderOption)
                            isGenderMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedPatientInfo(user: com.example.project_mobileapps.data.model.User) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Check, contentDescription = "Pasien Terpilih", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    "Pasien Terpilih",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(user.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}