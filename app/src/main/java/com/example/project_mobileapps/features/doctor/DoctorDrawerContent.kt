// File BARU: features/doctor/DoctorDrawerContent.kt
package com.example.project_mobileapps.features.doctor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.core.navigation.DoctorMenu
/**
 * Composable yang mendefinisikan tampilan isi dari Navigation Drawer (menu samping)
 * untuk panel dokter.
 *
 * @param currentRoute Route (rute) yang sedang aktif. Digunakan untuk menyorot (highlight)
 * item menu yang sedang dipilih.
 * @param onNavigate Callback (fungsi) yang dipanggil saat item menu diklik.
 * Membawa String [route] tujuan.
 * @param onLogoutClick Callback yang dipanggil saat tombol "Logout" diklik.
 */
@Composable
fun DoctorDrawerContent(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onLogoutClick: () -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Dokter Panel",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Divider()
        Spacer(modifier = Modifier.height(12.dp))

        DoctorMenu.allNavItems.forEach { item ->
            NavigationDrawerItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Medication, null) },
            label = { Text("Kelola Data Obat") },
            selected = false,
            onClick = { onNavigate("manage_medicine") },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        Spacer(modifier = Modifier.height(12.dp))

        NavigationDrawerItem(
            label = { Text("Logout", color = MaterialTheme.colorScheme.error) },
            selected = false,
            onClick = onLogoutClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}