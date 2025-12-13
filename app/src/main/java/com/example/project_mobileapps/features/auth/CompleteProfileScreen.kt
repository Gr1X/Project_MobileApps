// File: features/auth/CompleteProfileScreen.kt
package com.example.project_mobileapps.features.auth

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.PhoneAndroid // Icon HP lebih relevan
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_mobileapps.data.model.Gender
import com.example.project_mobileapps.ui.themes.PrimaryPeriwinkle
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    viewModel: AuthViewModel
) {
    val state by viewModel.authState.collectAsState()
    val context = LocalContext.current
    var isGenderExpanded by remember { mutableStateOf(false) }

    // Date Picker (Logic tetap sama)
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d ->
            val date = "$y-${String.format("%02d", m + 1)}-${String.format("%02d", d)}"
            viewModel.onCpDobChange(date)
        },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )
    // Batasi DatePicker: User harus minimal 5 tahun (contoh validasi logika)
    datePickerDialog.datePicker.maxDate = System.currentTimeMillis() - (5L * 365 * 24 * 60 * 60 * 1000)

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Lengkapi Data Diri",
                style = MaterialTheme.typography.headlineMedium,
                color = PrimaryPeriwinkle,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Data ini digunakan untuk rekam medis Anda.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // --- 1. NAMA LENGKAP (Validasi Huruf Only) ---
            OutlinedTextField(
                value = state.cpFullName,
                onValueChange = viewModel::onCpNameChange,
                label = { Text("Nama Lengkap (Sesuai KTP)") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                // UX: Memberi tahu user jika input kosong atau valid
                supportingText = {
                    if (state.cpFullName.isNotEmpty() && state.cpFullName.length < 3) {
                        Text("Minimal 3 huruf", color = MaterialTheme.colorScheme.error)
                    }
                },
                isError = state.cpFullName.isNotEmpty() && state.cpFullName.length < 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPeriwinkle,
                    focusedLabelColor = PrimaryPeriwinkle
                )
            )

            Spacer(Modifier.height(16.dp))

            // --- 2. NOMOR TELEPON (Professional Prefix +62) ---
            OutlinedTextField(
                value = state.cpPhone,
                onValueChange = viewModel::onCpPhoneChange,
                label = { Text("Nomor WhatsApp") },
                // PREFIX: Menampilkan +62 secara permanen di dalam box
                prefix = {
                    Text(
                        "+62 ",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                leadingIcon = { Icon(Icons.Outlined.PhoneAndroid, null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                placeholder = { Text("812xxxxxxx") }, // Contoh tanpa angka 0
                supportingText = {
                    val len = state.cpPhone.length
                    if (len > 0) {
                        if (len < 9) Text("Terlalu pendek (${len}/13)", color = MaterialTheme.colorScheme.error)
                        else Text("${len}/13 Digit", color = Color.Gray)
                    }
                },
                isError = state.cpPhone.isNotEmpty() && state.cpPhone.length < 9,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPeriwinkle,
                    focusedLabelColor = PrimaryPeriwinkle
                )
            )

            Spacer(Modifier.height(16.dp))

            // --- 3. JENIS KELAMIN ---
            ExposedDropdownMenuBox(
                expanded = isGenderExpanded,
                onExpandedChange = { isGenderExpanded = !isGenderExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state.cpGender.name.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Jenis Kelamin") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGenderExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPeriwinkle,
                        focusedLabelColor = PrimaryPeriwinkle
                    )
                )
                ExposedDropdownMenu(
                    expanded = isGenderExpanded,
                    onDismissRequest = { isGenderExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    Gender.values().forEach { gender ->
                        DropdownMenuItem(
                            text = { Text(gender.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                viewModel.onCpGenderChange(gender)
                                isGenderExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- 4. TANGGAL LAHIR ---
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.cpDob,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tanggal Lahir") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color.Black,
                        disabledBorderColor = Color.Gray,
                        disabledLeadingIconColor = Color.Gray,
                        disabledLabelColor = Color.Gray,
                        // Agar terlihat seperti input aktif meski disabled
                        disabledContainerColor = Color.Transparent
                    ),
                    enabled = false
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { datePickerDialog.show() }
                )
            }

            Spacer(Modifier.height(32.dp))

            // --- TOMBOL SIMPAN ---
            // Disable tombol jika data belum valid secara visual
            val isFormValid = state.cpFullName.length >= 3 &&
                    state.cpPhone.length >= 9 &&
                    state.cpDob.isNotBlank()

            Button(
                onClick = { viewModel.submitCompleteProfile() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryPeriwinkle,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                ),
                enabled = !state.isLoading && isFormValid
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Simpan & Lanjutkan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}