package com.example.project_mobileapps.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.project_mobileapps.R // Pastikan import ini sesuai package Anda

// Model Data Banner dengan Image Resource ID (Int)
data class BannerModel(
    val title: String,
    val subtitle: String,
    @DrawableRes val imageResId: Int // ID Drawable Lokal
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeBanner() {
    // DATA DUMMY (Pastikan Anda punya file gambar ini di folder res/drawable)
    // Jika error merah, ganti R.drawable.banner_1 dengan R.drawable.ic_launcher_background
    val banners = listOf(
        BannerModel(
            title = "Booking Mudah",
            subtitle = "Ambil antrian dokter dari rumah, hemat waktu.",
            imageResId = R.drawable.background1 // GANTI DENGAN GAMBAR ANDA (misal: R.drawable.banner_1)
        ),
        BannerModel(
            title = "Jadwal Dokter",
            subtitle = "Cek jam praktik terbaru secara real-time.",
            imageResId = R.drawable.background2 // GANTI DENGAN GAMBAR ANDA (misal: R.drawable.banner_2)
        ),
        BannerModel(
            title = "Layanan Prima",
            subtitle = "Komitmen kami untuk kesehatan Anda sekeluarga.",
            imageResId = R.drawable.background3 // GANTI DENGAN GAMBAR ANDA (misal: R.drawable.banner_3)
        )
    )

    val pagerState = rememberPagerState(pageCount = { banners.size })

    // Efek Auto-Scroll
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            if (!pagerState.isScrollInProgress) {
                val nextPage = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. CAROUSEL BANNER
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp), // Sedikit jarak kiri-kanan
            pageSpacing = 12.dp, // Jarak antar kartu
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            BannerCardItem(banner = banners[page])
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. INDIKATOR TITIK
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(banners.size) { iteration ->
                val isSelected = pagerState.currentPage == iteration
                // Animasi lebar titik
                val width by animateDpAsState(if (isSelected) 20.dp else 8.dp, label = "dot_width")
                val color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f)

                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun BannerCardItem(banner: BannerModel, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp), // Shadow halus
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // A. GAMBAR BACKGROUND
            Image(
                painter = painterResource(id = banner.imageResId),
                contentDescription = null,
                contentScale = ContentScale.Crop, // Crop agar gambar memenuhi kartu
                modifier = Modifier.fillMaxSize()
            )

            // B. GRADIENT OVERLAY (PENTING!)
            // Membuat teks putih terbaca jelas di atas gambar apa pun
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f), // Hitam pekat di kiri
                                Color.Black.copy(alpha = 0.4f), // Transparan di tengah
                                Color.Transparent               // Bening di kanan
                            ),
                            startX = 0f,
                            endX = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // C. KONTEN TEKS
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(20.dp)
                    .fillMaxWidth(0.65f), // Batasi teks hanya 65% lebar kartu
                verticalArrangement = Arrangement.Center
            ) {
                // Judul
                Text(
                    text = banner.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Deskripsi
                Text(
                    text = banner.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 2,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tombol "Lihat Detail"
                Surface(
                    color = MaterialTheme.colorScheme.primary, // Warna tombol aksen
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lihat Detail",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}