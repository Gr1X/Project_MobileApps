package com.example.project_mobileapps.ui.themes

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// MAPPING WARNA KE MATERIAL 3
private val LightColorScheme = lightColorScheme(
    // Brand Colors
    primary = BrandPrimary,
    onPrimary = SurfaceWhite,             // Teks di atas tombol biru adalah putih
    primaryContainer = BrandLight,
    onPrimaryContainer = BrandSecondary,

    // Backgrounds
    background = BackgroundApp,
    onBackground = TextPrimary,           // Teks di background aplikasi adalah abu gelap
    surface = SurfaceWhite,
    onSurface = TextPrimary,              // Teks di dalam kartu adalah abu gelap
    surfaceVariant = SurfaceSubtle,
    onSurfaceVariant = TextSecondary,

    // Error States
    error = StateError,
    onError = SurfaceWhite,
    errorContainer = StateErrorBg,
    onErrorContainer = StateError // Teks error di dalam container error warnanya merah
)

@Composable
fun ProjectMobileAppsTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar mengikuti warna background aplikasi (Clean look)
            window.statusBarColor = BackgroundApp.toArgb()
            // Ikon status bar menjadi gelap
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography, // Pastikan Type.kt Anda sudah menggunakan font Poppins
        content = content
    )
}