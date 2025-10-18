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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProjectMobileAppsTheme {
                val navController = rememberNavController()
                val toastMessage by ToastManager.toastMessage.collectAsState()

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