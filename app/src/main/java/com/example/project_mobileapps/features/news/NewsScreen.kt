// file: features/news/NewsScreen.kt
package com.example.project_mobileapps.features.news

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.example.project_mobileapps.R

data class NewsArticleUI(
    val title: String,
    val source: String,
    val imageUrl: String?,
    val articleUrl: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    onNewsClick: (encodedUrl: String) -> Unit,
    viewModel: NewsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Health News") }) }) { padding ->
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
                model = article.imageUrl,
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