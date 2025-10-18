// File: features/profile/ProfileScreen.kt

package com.example.project_mobileapps.features.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.project_mobileapps.R
import com.example.project_mobileapps.data.model.Role
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet
import com.example.project_mobileapps.ui.themes.TextSecondary
/**
 * Composable untuk layar Profil utama (Tab Profil).
 * Menampilkan data pengguna, menu navigasi, dan tombol logout.
 *
 * @param rootNavController NavController utama aplikasi (untuk navigasi ke flow lain).
 * @param onLogoutClick Callback yang dipanggil saat logout dikonfirmasi.
 * @param onNavigateToHistory Callback untuk navigasi ke layar Riwayat Kunjungan.
 * @param onNavigateToEditProfile Callback untuk navigasi ke layar Edit Profil.
 * @param viewModel ViewModel [ProfileViewModel] yang menyediakan data pengguna.
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

    // State untuk mengontrol visibilitas bottom sheet
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    // Tampilkan bottom sheet jika state-nya true
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
        // Box digunakan untuk menangani semua state UI (loading, success, error)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            } else {
                val user = uiState.user

                // 2. Lakukan pengecekan null pada variabel lokal tersebut.
                if (user != null) {
                    // Di dalam blok ini, compiler tahu 'user' tidak null dan aman digunakan.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // --- BAGIAN HEADER PROFIL ---
                        Spacer(modifier = Modifier.height(16.dp))
                        AsyncImage(
                            model = user.profilePictureUrl, // <-- AMAN diakses di sini
                            contentDescription = "Foto Profil",
                            placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                            error = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = user.name, style = MaterialTheme.typography.titleLarge) // <-- AMAN
                        Text(text = user.email, style = MaterialTheme.typography.bodyMedium, color = TextSecondary) // <-- AMAN
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onNavigateToEditProfile, shape = RoundedCornerShape(16.dp)) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Profil")
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

                        // --- TAMBAHKAN ROLE SWITCHER DI SINI ---
                        Spacer(modifier = Modifier.height(16.dp))
                        RoleSwitcher(
                            currentUserRole = uiState.user?.role,
                            onRoleSelected = { newRole ->
                                viewModel.switchRole(newRole)
                                // Navigasi ke flow yang sesuai setelah role diganti
                                val destination = when (newRole) {
                                    Role.ADMIN -> "admin_flow"
                                    Role.DOKTER -> "doctor_flow"
                                    Role.PASIEN -> "main_flow"
                                }
                                rootNavController.navigate(destination) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = { showLogoutConfirmation = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
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
                    // Tampilan jika data user gagal dimuat (tetap null)
                    Text(
                        text = "Gagal memuat data profil.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
/**
 * Composable helper (private) untuk satu item di menu profil (misal: "Riwayat Kunjungan").
 * @param icon Ikon [ImageVector] di sebelah kiri.
 * @param text Teks utama.
 * @param onClick Aksi saat item diklik.
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
 * Menampilkan [ExposedDropdownMenuBox] untuk memilih role.
 *
 * @param currentUserRole Role [Role] saat ini.
 * @param onRoleSelected Callback yang dipanggil saat role baru dipilih.
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
                modifier = Modifier.fillMaxWidth().menuAnchor()
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