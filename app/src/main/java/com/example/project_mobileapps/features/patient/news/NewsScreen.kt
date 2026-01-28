// file: features/news/NewsScreen.kt
package com.example.project_mobileapps.features.patient.news

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.project_mobileapps.R
/**
 * Model data UI lokal untuk satu artikel berita.
 * Dipakai oleh [NewsScreen] dan [NewsViewModel].
 *
 * @property title Judul artikel.
 * @property source Nama sumber berita (misal: "Detik Health").
 * @property imageUrl URL ke gambar thumbnail artikel.
 * @property articleUrl URL asli ke artikel lengkap.
 */
data class NewsArticleUI(
    val title: String,
    val source: String,
    val imageUrl: String?,
    val articleUrl: String?
)
/**
 * Composable untuk layar Daftar Berita Kesehatan.
 * Menampilkan [NewsUiState] dari [NewsViewModel].
 *
 * @param onNewsClick Callback yang dipanggil saat artikel diklik.
 * Membawa URL artikel yang sudah di-encode.
 * @param viewModel ViewModel [NewsViewModel] yang menyediakan data.
 * @param onNavigateBack Callback untuk kembali ke layar sebelumnya.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    onNewsClick: (encodedUrl: String) -> Unit,
    viewModel: NewsViewModel = (viewModel()),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health News") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                }
            )
        }) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.articles) { article ->
                    NewsItem(article = article, onClick = {
                        article.articleUrl?.let { url ->
                            val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                            onNewsClick(encodedUrl)
                        }
                    })
                }
            }
        }
    }
}

// ...

// =======================================================
// GANTI SELURUH FUNGSI INI
// =======================================================
/**
 * Composable helper (private) untuk menampilkan satu kartu item berita.
 * @param article Data [NewsArticleUI] yang akan ditampilkan.
 * @param onClick Callback yang akan dipanggil saat kartu ini diklik.
 */
@Composable
fun NewsItem(article: NewsArticleUI, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Gambar Berita
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(article.imageUrl)
                    .crossfade(true) // Animasi fade-in halus (Default: false)
                    .crossfade(500)  // Durasi 0.5 detik
                    .diskCachePolicy(CachePolicy.ENABLED) // Simpan di memori HP
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = article.title,
                contentScale = ContentScale.Crop, // Crop agar gambar mengisi area
                // Placeholder jika gambar gagal dimuat
                placeholder = painterResource(id = R.drawable.ic_launcher_background),
                error = painterResource(id = R.drawable.ic_launcher_background),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f) // Rasio 16:9 untuk konsistensi
                    .clip(MaterialTheme.shapes.medium) // Membulatkan sudut gambar
            )

            // Konten Teks
            Column(Modifier.padding(16.dp)) {
                // Sumber Berita (dibuat lebih kecil)
                Text(
                    text = article.source,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Judul Berita (dibuat lebih menonjol)
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2 // Batasi judul agar tidak terlalu panjang
                )
            }
        }
    }
}
// =======================================================