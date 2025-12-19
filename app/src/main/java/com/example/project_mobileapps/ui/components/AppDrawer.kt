package com.example.project_mobileapps.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_mobileapps.R
import com.example.project_mobileapps.ui.themes.* // Pastikan import Theme warna Anda

// Model Data untuk Item Menu
data class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun AppDrawerContent(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    menuItems: List<NavigationItem>,
    userProfileUrl: String? = null,
    userName: String = "User",
    userRole: String = "Role"
) {
    ModalDrawerSheet(
        drawerContainerColor = Color.White, // Background Putih Bersih
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp), // Sudut membulat di kanan
        modifier = Modifier.width(300.dp) // Lebar fix agar proporsional
    ) {
        // 1. HEADER PROFILE (Modern Gradient Style)
        DrawerHeader(
            profileUrl = userProfileUrl,
            name = userName,
            role = userRole
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. MENU ITEMS
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = "Menu Utama",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )

            menuItems.forEach { item ->
                val isSelected = currentRoute == item.route

                NavigationDrawerItem(
                    label = {
                        Text(
                            text = item.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            // Icon berwarna jika selected, abu jika tidak
                            tint = if (isSelected) BrandPrimary else Color.Gray
                        )
                    },
                    selected = isSelected,
                    onClick = { onNavigate(item.route) },
                    // Styling Selection (Highlight Biru Muda)
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = BrandPrimary.copy(alpha = 0.1f),
                        selectedIconColor = BrandPrimary,
                        selectedTextColor = BrandPrimary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(12.dp), // Item membulat
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // 3. FOOTER (LOGOUT)
        Divider(color = Color.LightGray.copy(alpha = 0.3f))

        NavigationDrawerItem(
            label = { Text("Keluar", fontWeight = FontWeight.SemiBold) },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
            selected = false,
            onClick = onLogout,
            colors = NavigationDrawerItemDefaults.colors(
                unselectedTextColor = MaterialTheme.colorScheme.error,
                unselectedIconColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.padding(12.dp)
        )

        // Version Text
        Text(
            text = "Versi Aplikasi 1.0.0",
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp)
        )
    }
}

@Composable
fun DrawerHeader(
    profileUrl: String?,
    name: String,
    role: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp) // Tinggi Header
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BrandPrimary, BrandPrimary.copy(alpha = 0.8f))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            // Avatar dengan Border Putih
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.size(70.dp).padding(2.dp) // Border effect
            ) {
                // Gunakan Coil AsyncImage jika ada URL, atau Icon default
                if (profileUrl != null) {
                    // Placeholder Image (Ganti dengan AsyncImage Coil Anda)
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground), // Ganti resource Anda
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.background(Color.LightGray)) {
                        Text(name.take(1), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = role,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        // Hiasan visual (lingkaran transparan di pojok)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-30).dp)
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        )
    }
}