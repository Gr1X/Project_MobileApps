package com.example.project_mobileapps.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.core.navigation.AdminNavItem

@Composable
fun AdminDrawerContent(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onLogoutClick: () -> Unit
) {
    val items = listOf(
        AdminNavItem.Dashboard,
        AdminNavItem.ManageSchedule,
        AdminNavItem.Reports
    )

    ModalDrawerSheet {
        // Header
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

        // Daftar Menu
        items.forEach { item ->
            NavigationDrawerItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        Spacer(modifier = Modifier.weight(1f)) // Mendorong tombol logout ke bawah

        // Tombol Logout
        NavigationDrawerItem(
            icon = { /* Ikon logout jika perlu */ },
            label = { Text("Logout", color = MaterialTheme.colorScheme.error) },
            selected = false,
            onClick = onLogoutClick,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}