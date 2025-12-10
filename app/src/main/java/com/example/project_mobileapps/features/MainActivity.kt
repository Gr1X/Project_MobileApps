package com.example.project_mobileapps

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.project_mobileapps.features.notifications.GlobalNotificationObserver
import com.example.project_mobileapps.navigation.AppNavigation
import com.example.project_mobileapps.ui.components.CustomToast
import com.example.project_mobileapps.ui.components.ToastManager
import com.example.project_mobileapps.ui.themes.ProjectMobileAppsTheme
import com.example.project_mobileapps.utils.CloudinaryHelper
import com.example.project_mobileapps.utils.NotificationHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

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

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CloudinaryHelper.init(this)
        NotificationHelper.createNotificationChannels(this)
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

                LaunchedEffect(Unit) {
                    val targetRoute = intent.getStringExtra("TARGET_ROUTE")
                    if (!targetRoute.isNullOrBlank()) {
                        // Reset intent agar tidak navigasi berulang saat rotate screen
                        intent.removeExtra("TARGET_ROUTE")

                        // Beri sedikit delay agar NavHost siap
                        kotlinx.coroutines.delay(500)
                        try {
                            navController.navigate(targetRoute)
                        } catch (e: Exception) {
                            e.printStackTrace() // Handle jika rute tidak ditemukan
                        }
                    }
                }

                GlobalNotificationObserver()

                // 2. Request Izin Notifikasi (Khusus Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notificationPermission = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
                    LaunchedEffect(Unit) {
                        if (!notificationPermission.status.isGranted) {
                            notificationPermission.launchPermissionRequest()
                        }
                    }
                }

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