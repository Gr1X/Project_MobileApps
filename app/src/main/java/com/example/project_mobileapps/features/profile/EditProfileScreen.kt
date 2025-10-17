package com.example.project_mobileapps.features.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.model.Gender
import com.example.project_mobileapps.ui.components.CircularBackButton
import android.widget.Toast
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel() // Kita akan gunakan ViewModel yang sama
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // State lokal untuk menampung perubahan sebelum disimpan
    var name by remember { mutableStateOf(uiState.user?.name ?: "") }
    var phoneNumber by remember { mutableStateOf(uiState.user?.phoneNumber ?: "") }
    var dateOfBirth by remember { mutableStateOf(uiState.user?.dateOfBirth ?: "") }
    var gender by remember { mutableStateOf(uiState.user?.gender ?: Gender.PRIA) }

    // State untuk dropdown gender
    var isGenderMenuExpanded by remember { mutableStateOf(false) }


    var showSaveConfirmation by remember { mutableStateOf(false) }

    //  Tampilkan bottom sheet jika state-nya true
    if (showSaveConfirmation) {
        ConfirmationBottomSheet(
            onDismiss = { showSaveConfirmation = false },
            onConfirm = {
                showSaveConfirmation = false // Tutup sheet terlebih dahulu
                viewModel.updateUser(name, phoneNumber, gender, dateOfBirth)
                Toast.makeText(context, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
                onNavigateBack() // Kembali ke halaman profil setelah simpan
            },
            title = "Simpan Perubahan?",
            text = "Apakah Anda yakin ingin menyimpan perubahan pada profil Anda?"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profil") },
                navigationIcon = { CircularBackButton(onClick = onNavigateBack) },
                actions = {
                    TextButton(onClick = { showSaveConfirmation = true }) {
                        Text("Simpan", style = MaterialTheme.typography.labelLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Lengkap") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Nomor Telepon") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = dateOfBirth,
                onValueChange = { dateOfBirth = it },
                label = { Text("Tanggal Lahir (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Dropdown untuk Gender
            ExposedDropdownMenuBox(
                expanded = isGenderMenuExpanded,
                onExpandedChange = { isGenderMenuExpanded = !isGenderMenuExpanded }
            ) {
                OutlinedTextField(
                    value = gender.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Jenis Kelamin") },
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
                                gender = genderOption
                                isGenderMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}