package com.example.project_mobileapps.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.project_mobileapps.navigation.BottomNavItem
import com.google.ar.sceneform.rendering.Light


/**
 * Bottom Navigation Bar yang dikustomisasi dengan gaya Modern & Premium.
 * Fitur: Animasi ikon, Haptic Feedback, Shadow Elevation, dan Custom Colors.
 */
@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Queue,
        BottomNavItem.Profile
    )

    // Mengambil Haptic Feedback (Getaran)
    val haptic = LocalHapticFeedback.current

    // Container dengan Shadow agar terlihat "Floating" / Melayang
    NavigationBar(
        modifier = Modifier
            .height(80.dp) // Sedikit lebih tinggi agar lega
            .shadow(
                elevation = 15.dp, // Bayangan lembut ke atas
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                spotColor = Color.Black.copy(alpha = 0.1f) // Warna bayangan tidak terlalu pekat
            )
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)), // Sudut atas membulat
        containerColor = Color.White, // Background Putih Bersih
        tonalElevation = 0.dp // Matikan elevasi bawaan Material 3 agar warna tetap putih murni
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            val isSelected = currentRoute == item.route

            // Animasi Scale: Ikon membesar sedikit saat dipilih
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.2f else 1.0f,
                animationSpec = tween(durationMillis = 300),
                label = "iconScale"
            )

            // Animasi Weight Font: Label menebal saat dipilih
            val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale) // Terapkan animasi
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = fontWeight,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.offset(y = 2.dp) // Sedikit geser ke bawah agar rapi
                    )
                },
                selected = isSelected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                // Kustomisasi Warna
                colors = NavigationBarItemDefaults.colors(
                    // Ikon Aktif: Warna Primary
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    // Teks Aktif: Warna Primary
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    // Indicator (Pil): Warna Primary tapi transparan sekali (sangat soft)
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    // Ikon Tidak Aktif: Abu-abu soft
                    unselectedIconColor = Color.Gray,
                    // Teks Tidak Aktif: Abu-abu soft
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}