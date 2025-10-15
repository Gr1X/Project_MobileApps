package com.example.project_mobileapps.features.patient.news

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    url: String,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Berita") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        // Menggunakan AndroidView untuk menampilkan WebView dari sistem Android
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    // WebViewClient memastikan link dibuka di dalam aplikasi, bukan di browser eksternal
                    webViewClient = WebViewClient()
                    // Memuat URL berita yang dikirimkan
                    loadUrl(url)
                }
            },
            modifier = Modifier.padding(innerPadding)
        )
    }
}