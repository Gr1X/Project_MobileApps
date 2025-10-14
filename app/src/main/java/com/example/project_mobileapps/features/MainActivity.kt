package com.example.project_mobileapps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.project_mobileapps.navigation.AppNavigation
import com.example.project_mobileapps.ui.themes.ProjectMobileAppsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProjectMobileAppsTheme {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }
}