package com.example.project_mobileapps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.project_mobileapps.navigation.AppNavigation
import com.example.project_mobileapps.ui.components.CustomToast
import com.example.project_mobileapps.ui.components.ToastManager
import com.example.project_mobileapps.ui.themes.ProjectMobileAppsTheme
/**
 * [MainActivity] adalah satu-satunya Activity dalam aplikasi ini (Single-Activity Architecture).
 * Ini berfungsi sebagai titik masuk (entry point) utama yang akan
 * menghost (menampung) seluruh UI Jetpack Compose.
 */
class MainActivity : ComponentActivity() {
    /**
     * [onCreate] adalah metode siklus hidup (lifecycle) Android yang pertama kali dipanggil
     * saat Activity dibuat.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProjectMobileAppsTheme {
                val navController = rememberNavController()
                val toastMessage by ToastManager.toastMessage.collectAsState()
                /**
                 * [Box] digunakan sebagai layout root.
                 * Tujuannya adalah untuk menumpuk (overlay) [CustomToast]
                 * di atas [AppNavigation].
                 */
                // Bungkus navigasi dengan Box agar Toast bisa tampil di atasnya
                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(navController = navController)

                    // Letakkan CustomToast di sini
                    CustomToast(
                        modifier = Modifier.align(Alignment.TopCenter),
                        toastMessage = toastMessage,
                        onDismiss = { ToastManager.hideToast() }
                    )
                }
            }
        }
    }
}