// File: features/profile/EditProfileScreen.kt
package com.example.project_mobileapps.features.profile

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.project_mobileapps.R
import com.example.project_mobileapps.data.model.Gender
import com.example.project_mobileapps.ui.components.CircularBackButton
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet
import com.example.project_mobileapps.ui.themes.PrimaryPeriwinkle
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // --- STATE LOGIC ---
    var showSaveConfirmation by remember { mutableStateOf(false) }
    var isGenderMenuExpanded by remember { mutableStateOf(false) }

    // State Foto
    var showPhotoOptionDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // --- LAUNCHERS FOTO (Sama seperti ProfileScreen) ---
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) viewModel.updateProfilePicture(uri) }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { isSuccess -> if (isSuccess && tempCameraUri != null) viewModel.updateProfilePicture(tempCameraUri!!) }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val uri = createTempImageUri(context)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Izin kamera diperlukan.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // --- DIALOG DATE PICKER ---
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d ->
            val formattedDate = "$y-${String.format("%02d", m + 1)}-${String.format("%02d", d)}"
            viewModel.onDobChange(formattedDate)
        },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    // --- BOTTOM SHEET KONFIRMASI SIMPAN ---
    if (showSaveConfirmation) {
        ConfirmationBottomSheet(
            onDismiss = { showSaveConfirmation = false },
            onConfirm = {
                showSaveConfirmation = false
                val result = viewModel.updateUser()
                if (result.isSuccess) {
                    Toast.makeText(context, "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                } else {
                    Toast.makeText(context, "Gagal memperbarui: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            },
            title = "Simpan Perubahan?",
            text = "Pastikan data diri Anda sudah benar."
        )
    }

    // --- DIALOG PILIH FOTO ---
    if (showPhotoOptionDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoOptionDialog = false },
            title = { Text("Ubah Foto Profil") },
            text = { Text("Pilih sumber foto:") },
            confirmButton = {
                TextButton(onClick = {
                    showPhotoOptionDialog = false
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(8.dp)); Text("Galeri") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhotoOptionDialog = false
                    permissionLauncher.launch(android.Manifest.permission.CAMERA)
                }) { Icon(Icons.Default.PhotoCamera, null); Spacer(Modifier.width(8.dp)); Text("Kamera") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profil", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { CircularBackButton(onClick = onNavigateBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFFF8FAFC) // Background Abu Modern
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. BAGIAN FOTO PROFIL (EDITABLE)
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = uiState.user?.profilePictureUrl,
                    contentDescription = "Foto Profil",
                    placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                    error = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                        .clickable { showPhotoOptionDialog = true }
                )

                // Tombol Kamera Kecil
                Surface(
                    shape = CircleShape,
                    color = PrimaryPeriwinkle,
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                    modifier = Modifier.size(36.dp).clickable { showPhotoOptionDialog = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (uiState.isImageUploading) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Text("Ketuk foto untuk mengubah", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

            // 2. KARTU INFORMASI AKUN (READ ONLY)
            ProfileSectionCard(title = "Informasi Akun", icon = Icons.Outlined.Lock) {
                ReadOnlyField(
                    label = "Email",
                    value = uiState.user?.email ?: "-",
                    icon = Icons.Outlined.Email
                )
            }

            // 3. KARTU DATA PRIBADI (EDITABLE)
            ProfileSectionCard(title = "Data Pribadi", icon = Icons.Outlined.Person) {
                // NAMA
                EditField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = "Nama Lengkap",
                    icon = Icons.Outlined.Badge,
                    error = uiState.nameError,
                    capitalization = KeyboardCapitalization.Words
                )

                Spacer(Modifier.height(16.dp))

                // NO HP
                EditField(
                    value = uiState.phoneNumber,
                    onValueChange = viewModel::onPhoneChange,
                    label = "Nomor Telepon",
                    icon = Icons.Outlined.Phone,
                    error = uiState.phoneError,
                    keyboardType = KeyboardType.Phone
                )

                Spacer(Modifier.height(16.dp))

                // GENDER & TGL LAHIR (Baris Sejajar)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Gender Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isGenderMenuExpanded,
                        onExpandedChange = { isGenderMenuExpanded = !isGenderMenuExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = uiState.gender.name.lowercase().replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGenderMenuExpanded) },
                            modifier = Modifier.menuAnchor(),
                            colors = InputDefaults()
                        )
                        ExposedDropdownMenu(
                            expanded = isGenderMenuExpanded,
                            onDismissRequest = { isGenderMenuExpanded = false }
                        ) {
                            Gender.values().forEach { genderOption ->
                                DropdownMenuItem(
                                    text = { Text(genderOption.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        viewModel.onGenderChange(genderOption)
                                        isGenderMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Date Picker
                    Box(modifier = Modifier.weight(1f).clickable { datePickerDialog.show() }) {
                        OutlinedTextField(
                            value = uiState.dateOfBirth,
                            onValueChange = {},
                            label = { Text("Tgl Lahir") },
                            enabled = false,
                            trailingIcon = { Icon(Icons.Outlined.CalendarToday, null, tint = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.Black,
                                disabledBorderColor = Color.LightGray,
                                disabledLabelColor = Color.Gray,
                                disabledContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 4. TOMBOL SIMPAN
            Button(
                onClick = { showSaveConfirmation = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPeriwinkle)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Simpan Perubahan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ==========================================
// HELPER COMPONENTS
// ==========================================

@Composable
fun ProfileSectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = PrimaryPeriwinkle, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            }
            Divider(Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
            content()
        }
    }
}

@Composable
fun EditField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = Color.Gray) },
        modifier = Modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = { if (error != null) Text(error, color = MaterialTheme.colorScheme.error) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = InputDefaults()
    )
}

@Composable
fun ReadOnlyField(label: String, value: String, icon: ImageVector) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = Color.Gray.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        enabled = false,
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = Color.DarkGray,
            disabledContainerColor = Color(0xFFF1F5F9), // Sedikit abu
            disabledBorderColor = Color.LightGray.copy(alpha = 0.5f),
            disabledLabelColor = Color.Gray
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun InputDefaults() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryPeriwinkle,
    unfocusedBorderColor = Color.LightGray,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)

// Helper untuk membuat URI File Sementara
fun createTempImageUri(context: Context): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = File(context.cacheDir, "images")
    if (!storageDir.exists()) storageDir.mkdirs()
    val file = File(storageDir, "IMG_$timeStamp.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}