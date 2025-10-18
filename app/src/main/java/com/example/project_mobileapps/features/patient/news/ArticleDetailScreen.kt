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
/**
 * Composable untuk layar Detail Artikel Berita.
 * Layar ini menggunakan [AndroidView] untuk menyematkan (embed)
 * [WebView] Android asli untuk menampilkan konten web.
 *
 * @param url URL artikel (sudah di-decode) yang akan dimuat di WebView.
 * @param onNavigateBack Callback untuk kembali ke layar sebelumnya.
 */
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
        /**
         * [AndroidView] adalah jembatan untuk menggunakan View Android (non-Compose)
         * di dalam UI Compose.
         *
         * @param factory Lambda yang membuat instance [WebView].
         * @param modifier Modifier untuk [AndroidView], termasuk padding dari Scaffold.
         */
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    /**
                     * Mengatur [WebViewClient] sangat penting.
                     * Tanpa ini, tautan akan dibuka di browser eksternal (misal: Chrome),
                     * yang akan mengeluarkan pengguna dari aplikasi.
                     */
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