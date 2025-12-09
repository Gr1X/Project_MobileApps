// File: features/profile/ProfileScreen.kt

package com.example.project_mobileapps.features.profile

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.project_mobileapps.R
import com.example.project_mobileapps.data.model.Role
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet
import com.example.project_mobileapps.ui.themes.TextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Composable untuk layar Profil utama (Tab Profil).
 * Menampilkan data pengguna, menu navigasi, tombol logout, dan fitur Ganti Foto Profil.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    rootNavController: NavHostController,
    onLogoutClick: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // State untuk mengontrol visibilitas bottom sheet logout
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    // State untuk Dialog Pilihan (Kamera vs Galeri)
    var showPhotoOptionDialog by remember { mutableStateOf(false) }

    // State untuk menyimpan URI sementara hasil jepretan kamera
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // --- LAUNCHER 1: GALERI (Photo Picker) ---
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.updateProfilePicture(uri)
            }
        }
    )

    // --- LAUNCHER 2: KAMERA ---
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { isSuccess ->
            if (isSuccess && tempCameraUri != null) {
                // Foto berhasil diambil, kirim URI ke ViewModel untuk upload
                viewModel.updateProfilePicture(tempCameraUri!!)
            }
        }
    )

    // --- LAUNCHER 3: IZIN KAMERA ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Jika izin diberikan, buat file temp dan buka kamera
                val uri = createImageUri(context)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Tampilkan bottom sheet logout jika state-nya true
    if (showLogoutConfirmation) {
        ConfirmationBottomSheet(
            onDismiss = { showLogoutConfirmation = false },
            onConfirm = {
                showLogoutConfirmation = false
                onLogoutClick()
            },
            title = "Konfirmasi Logout",
            text = "Apakah Anda yakin ingin keluar dari akun Anda?"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil Saya") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val user = uiState.user

                if (user != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // --- BAGIAN HEADER PROFIL (FOTO & NAMA) ---
                        Spacer(modifier = Modifier.height(16.dp))

                        // Box untuk Foto Profil + Tombol Edit Kecil
                        Box(contentAlignment = Alignment.BottomEnd) {
                            // 1. Gambar Profil
                            AsyncImage(
                                model = user.profilePictureUrl,
                                contentDescription = "Foto Profil",
                                placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                                error = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            )

                            // 2. Tombol Edit (Kamera) Kecil
                            IconButton(
                                onClick = { showPhotoOptionDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .border(2.dp, Color.White, CircleShape)
                            ) {
                                if (uiState.isImageUploading) {
                                    // Tampilkan loading kecil saat upload
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Ganti Foto",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = user.name, style = MaterialTheme.typography.titleLarge)
                        Text(text = user.email, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(onClick = onNavigateToEditProfile, shape = RoundedCornerShape(16.dp)) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Data Diri")
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // --- BAGIAN MENU OPSI ---
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                ProfileOptionItem(icon = Icons.Outlined.History, text = "Riwayat Kunjungan", onClick = onNavigateToHistory)
                                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                                ProfileOptionItem(icon = Icons.Outlined.Settings, text = "Pengaturan", onClick = { /* TODO */ })
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = { showLogoutConfirmation = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Outlined.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Logout", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else {
                    Text(
                        text = "Gagal memuat data profil.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        // --- DIALOG PILIHAN SUMBER FOTO ---
        if (showPhotoOptionDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoOptionDialog = false },
                title = { Text("Ganti Foto Profil") },
                text = { Text("Pilih sumber foto:") },
                confirmButton = {
                    TextButton(onClick = {
                        showPhotoOptionDialog = false
                        // Buka Galeri
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Default.Image, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Galeri")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPhotoOptionDialog = false
                        // Cek Izin Kamera -> Buka Kamera
                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }) {
                        Icon(Icons.Default.PhotoCamera, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Kamera")
                    }
                }
            )
        }
    }
}

/**
 * Fungsi Helper untuk membuat URI file sementara untuk kamera.
 * Membutuhkan FileProvider yang sudah disetup di AndroidManifest.xml.
 */
fun createImageUri(context: Context): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = File(context.cacheDir, "images")
    if (!storageDir.exists()) storageDir.mkdirs()

    val file = File(storageDir, "IMG_$timeStamp.jpg")

    // Pastikan authority sama dengan yang ada di AndroidManifest.xml
    // android:authorities="${applicationId}.provider"
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
}

/**
 * Composable helper (private) untuk satu item di menu profil.
 */
@Composable
private fun ProfileOptionItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(text, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Lanjutkan")
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

/**
 * Composable helper (private) untuk 'Role Switcher' (Fitur Development).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSwitcher(
    currentUserRole: Role?,
    onRoleSelected: (Role) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text("Ganti Akses (Untuk Development)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = "Login sebagai: ${currentUserRole?.name ?: "..."}",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Role.values().forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role.name) },
                        onClick = {
                            onRoleSelected(role)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}