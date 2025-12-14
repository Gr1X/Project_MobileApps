package com.example.project_mobileapps.features.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.core.navigation.AdminMenu
import com.example.project_mobileapps.core.navigation.AdminNavGroup
import com.example.project_mobileapps.core.navigation.AdminNavItem

/**
 * Composable yang membangun konten UI untuk Navigation Drawer (menu samping) Admin.
 *
 * @param currentRoute Rute dari layar yang sedang aktif, digunakan untuk menyorot item menu yang sesuai.
 * @param onNavigate Callback yang dipanggil ketika item navigasi di-klik, membawa rute tujuan.
 * @param onLogoutClick Callback yang dipanggil ketika tombol logout di-klik.
 */
@Composable
fun AdminDrawerContent(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onLogoutClick: () -> Unit
) {
    // State untuk mengontrol visibilitas (expand/collapse) dari sub-menu "Manajemen".
    var managementExpanded by remember { mutableStateOf(false) }

    ModalDrawerSheet {
        // Bagian Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Admin Panel",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Divider()
        Spacer(modifier = Modifier.height(12.dp))

        // Iterasi melalui semua item navigasi yang didefinisikan di AdminMenu.
        AdminMenu.allNavItems.forEach { navItem ->
            when (navItem) {
                // Menggunakan 'when' untuk menangani tipe item yang berbeda (item tunggal vs grup).
                is AdminNavItem -> {
                    NavigationDrawerItem(
                        icon = { Icon(navItem.icon, contentDescription = navItem.label) },
                        label = { Text(navItem.label) },
                        selected = currentRoute == navItem.route,
                        onClick = { onNavigate(navItem.route) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                // Kasus untuk grup menu (dropdown)
                is AdminNavGroup -> {
                    // Header untuk grup dropdown yang bisa diklik
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { managementExpanded = !managementExpanded }
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(navItem.icon, contentDescription = navItem.label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(navItem.label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(
                            imageVector = if (managementExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Expand"
                        )
                    }
                    // Tampilkan sub-menu jika 'managementExpanded' bernilai true
                    if (managementExpanded) {
                        navItem.items.forEach { subItem ->
                            NavigationDrawerItem(
                                icon = { /* Ikon sub-menu bisa dikosongkan agar rapi */ },
                                label = { Text(subItem.label) },
                                selected = currentRoute == subItem.route,
                                onClick = { onNavigate(subItem.route) },
                                modifier = Modifier
                                    .padding(start = 32.dp) // Beri indentasi agar terlihat seperti sub-menu
                                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f)) // Pendorong logout ke bawah

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Medication, null) },
            label = { Text("Kelola Data Obat") },
            selected = false,
            onClick = { onNavigate("manage_medicine") },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        // Tombol Logout
        NavigationDrawerItem(
            icon = { /* Ikon bisa dikosongkan */ },
            label = { Text("Logout", color = MaterialTheme.colorScheme.error) },
            selected = false,
            onClick = onLogoutClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}