package com.example.project_mobileapps.ui.themes

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 1. Definisikan LightColorScheme dengan palet warna baru kita
private val LightColorScheme = lightColorScheme(
    primary = PrimaryPeriwinkle,
    onPrimary = OnPrimary,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    secondaryContainer = LightPurpleGray,
    onSecondaryContainer = PrimaryPeriwinkle
)

@Composable
fun ProjectMobileAppsTheme(
    // 2. Kunci tema ke Light Mode dengan menghapus parameter darkTheme
    content: @Composable () -> Unit
) {
    // 3. Langsung gunakan LightColorScheme tanpa logika pengecekan tema gelap
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}