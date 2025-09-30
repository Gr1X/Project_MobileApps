// file: features/getstarted/GetStartedScreen.kt
package com.example.project_mobileapps.features.getstarted

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.ui.themes.ProjectMobileAppsTheme

@Composable
fun GetStartedScreen(
    onGetStartedClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Selamat Datang di HealthyApp",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Solusi kesehatan Anda dalam satu genggaman. Buat janji temu dengan dokter pilihan Anda dengan mudah.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onGetStartedClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Get Started")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GetStartedScreenPreview() {
    ProjectMobileAppsTheme {
        GetStartedScreen({}, {})
    }
}