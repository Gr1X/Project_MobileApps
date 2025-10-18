// Salin dan ganti seluruh isi file: features/profile/EditProfileScreen.kt

package com.example.project_mobileapps.features.profile

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.model.Gender
import com.example.project_mobileapps.ui.components.CircularBackButton
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet
import java.util.*
/**
 * Composable untuk layar Edit Profil.
 * Menampilkan form yang diisi dengan data dari [ProfileViewModel] dan
 * memungkinkan pengguna untuk mengubahnya.
 *
 * @param onNavigateBack Callback untuk kembali ke layar [ProfileScreen].
 * @param viewModel ViewModel [ProfileViewModel] yang sama dengan [ProfileScreen],
 * menyediakan state untuk form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSaveConfirmation by remember { mutableStateOf(false) }
    var isGenderMenuExpanded by remember { mutableStateOf(false) }
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = "$selectedYear-${String.format("%02d", selectedMonth + 1)}-${String.format("%02d", selectedDay)}"
            viewModel.onDobChange(formattedDate)
        }, year, month, day
    )

    if (showSaveConfirmation) {
        ConfirmationBottomSheet(
            onDismiss = { showSaveConfirmation = false },
            onConfirm = {
                showSaveConfirmation = false
                val result = viewModel.updateUser()
                if (result.isSuccess) {
                    Toast.makeText(context, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
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
            // --- INPUT NAMA ---
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nama Lengkap") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.nameError != null,
                supportingText = { if (uiState.nameError != null) Text(uiState.nameError!!) }
            )

            // --- INPUT TELEPON ---
            OutlinedTextField(
                value = uiState.phoneNumber,
                onValueChange = viewModel::onPhoneChange,
                label = { Text("Nomor Telepon") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                isError = uiState.phoneError != null,
                supportingText = { if (uiState.phoneError != null) Text(uiState.phoneError!!) }
            )

            // --- INPUT TANGGAL LAHIR ---
            Box(modifier = Modifier.clickable { datePickerDialog.show() }) {
                OutlinedTextField(
                    value = uiState.dateOfBirth,
                    onValueChange = {}, // Kosongkan karena dihandle oleh dialog
                    label = { Text("Tanggal Lahir") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false, // Matikan input keyboard
                    trailingIcon = { Icon(Icons.Outlined.CalendarToday, "Pilih Tanggal") },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    isError = uiState.dateOfBirthError != null,
                    supportingText = { if (uiState.dateOfBirthError != null) Text(uiState.dateOfBirthError!!) }
                )
            }

            ExposedDropdownMenuBox(
                expanded = isGenderMenuExpanded,
                onExpandedChange = { isGenderMenuExpanded = !isGenderMenuExpanded }
            ) {
                // --- INPUT GENDER ---
                OutlinedTextField(
                    value = uiState.gender.name,
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
                                viewModel.onGenderChange(genderOption)
                                isGenderMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}