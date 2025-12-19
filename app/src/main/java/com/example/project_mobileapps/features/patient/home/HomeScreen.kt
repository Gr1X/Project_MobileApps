package com.example.project_mobileapps.features.patient.home

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.repo.NotificationRepository
import com.example.project_mobileapps.features.patient.news.NewsArticleUI
import kotlinx.coroutines.delay
import java.util.Date
import java.util.concurrent.TimeUnit
import com.example.project_mobileapps.ui.components.FeaturedDoctorCard
import com.example.project_mobileapps.ui.components.HomeBanner
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.example.project_mobileapps.R

/**
 * Data class internal untuk merepresentasikan item pada [ActionButtonsRow].
 * @param label Teks yang ditampilkan di bawah ikon.
 * @param icon Ikon [ImageVector] yang akan ditampilkan.
 */
private data class ActionItem(val label: String, val icon: ImageVector)
private data class DayData(val day: String, val date: String, val isSelected: Boolean = false)
/**
 * Composable utama untuk layar Home Pasien.
 * Menampilkan [HomeUiState] dari [HomeViewModel].
 *
 * @param uiState State UI saat ini dari [HomeViewModel].
 * @param onDoctorClick Callback saat kartu dokter diklik, membawa ID dokter.
 * @param onNavigateToQueue Callback untuk navigasi ke layar antrian (saat ini tidak dipakai langsung).
 * @param onProfileClick Callback saat ikon profil di TopAppBar diklik.
 * @param onTakeQueueClick Callback saat tombol 'Ambil Antrian' di [FeaturedDoctorCard] diklik.
 * @param onNewsClick Callback saat tombol aksi "Berita Kesehatan" diklik.
 * @param modifier Modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onDoctorClick: (String) -> Unit,
    onNavigateToQueue: () -> Unit,
    onProfileClick: () -> Unit,
    onTakeQueueClick: () -> Unit,
    onNewsItemClick: (String) -> Unit, // Untuk klik kartu berita (butuh URL)
    onSeeAllNewsClick: () -> Unit,     // Untuk klik "Lihat Semua"
    onSmartMealPlanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showNotificationMenu by remember { mutableStateOf(false) }
    val notifications by NotificationRepository.notificationsFlow.collectAsState()
    val hasUnreadNotifications = notifications.any { !it.isRead }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        containerColor = Color.White,

        topBar = {
            TopAppBar(
                title = {  },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color.White
                ),

                navigationIcon = {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Profil",
                            modifier = Modifier.size(34.dp), // Sedikit diperbesar ala iOS
                            tint = Color.Black // Ikon Hitam Pekat
                        )
                    }
                },

                actions = {
                    Box {
                        IconButton(onClick = {
                            showNotificationMenu = true
                            NotificationRepository.markAllAsRead()
                        }) {
                            BadgedBox(
                                badge = {
                                    if (hasUnreadNotifications) {
                                        Badge(
                                            // [MODIFIKASI 2]: Mengecilkan ukuran Badge
                                            modifier = Modifier
                                                .size(8.dp) // Ukuran badge (bisa diganti 6.dp jika ingin lebih kecil)
                                                .offset(x = (-4).dp, y = (2).dp), // (Opsional) Menggeser posisi badge agar pas di dalam lingkaran
                                            containerColor = MaterialTheme.colorScheme.error // Warna merah
                                        ) {
                                            // Kosongkan blok ini agar hanya berupa titik (dot)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "Notifikasi"
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showNotificationMenu,
                            onDismissRequest = { showNotificationMenu = false }
                        ) {
                            if (notifications.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Tidak ada notifikasi baru.") },
                                    onClick = { showNotificationMenu = false }
                                )
                            } else {
                                notifications.forEach { notification ->
                                    val dismissState = rememberSwipeToDismissBoxState(
                                        confirmValueChange = {
                                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                                NotificationRepository.dismissNotification(notification.id)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                true
                                            } else false
                                        }
                                    )

                                    SwipeToDismissBox(
                                        state = dismissState,
                                        enableDismissFromStartToEnd = false,
                                        backgroundContent = {
                                            val color by animateColorAsState(
                                                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                } else {
                                                    Color.Transparent
                                                }, label = "background color"
                                            )
                                        }
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = notification.message,

                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            },
                                            onClick = { /* Aksi saat notif di-klik */ }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier.padding(horizontal = 20.dp)
            ){
                Text(
                    text = "${uiState.greeting}, ${uiState.userName} 👋",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "How are you feeling today?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Light
                )
            }

            // --- 2. POSISI BANNER (DISINI) ---
            // Beri sedikit jarak dari teks sapaan
            Spacer(Modifier.height(16.dp))

            // Panggil Komponen Banner Anda
            HomeBanner()

            Spacer(Modifier.height(16.dp))

            // 3. QUICK ACTIONS (iOS Style Grid)
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "Health Updates",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color(0xFF1A1C24)
                )

                Spacer(Modifier.height(10.dp))

                // Definisi Item dengan Label Asli
                val actionItems = listOf(
                    ActionItem("Booking", Icons.Outlined.LocalPharmacy),
                    ActionItem("News", Icons.Outlined.Newspaper),
                    ActionItem("Smart Meal\nPlan", Icons.Outlined.RestaurantMenu)
                )

                ActionButtonsRow(
                    actions = actionItems,
                    onActionClick = { label ->
                        // Hapus newline untuk pengecekan logic
                        val normalizedLabel = label.replace("\n", " ")

                        when (normalizedLabel) {
                            "Booking" -> onNavigateToQueue()
                            "News" -> onSeeAllNewsClick()
                            "Smart Meal Plan" -> onSmartMealPlanClick()
                            else -> Toast.makeText(context, "$normalizedLabel diklik!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            if (uiState.doctor != null) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp)
                ){
                    Text(
                        text = "Appointment ",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color(0xFF1A1C24)
                    )

                    Spacer(Modifier.height(10.dp))

                    FeaturedDoctorCard(
                        doctor = uiState.doctor,
                        practiceStatus = uiState.practiceStatus,
                        upcomingQueue = uiState.upcomingQueue,
                        availableSlots = uiState.availableSlots,
                        onTakeQueueClick = onTakeQueueClick
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // [START: NEWS SECTION]
            if (uiState.topNews.isNotEmpty()) {
                NewsSection(
                    articles = uiState.topNews,
                    onNewsClick = onNewsItemClick,      // Mengirim (String) -> Unit
                    onViewMoreClick = onSeeAllNewsClick // Mengirim () -> Unit
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
/**
 * Composable (saat ini tidak terpakai di HomeScreen) untuk menampilkan
 * status antrian pengguna.
 * @param queue Data [QueueItem] milik pengguna.
 * @param onClick Aksi saat kartu diklik.
 */
@Composable
fun QueueStatusCard(queue: QueueItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Nomor Antrian : ${queue.queueNumber}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Klik untuk melihat detail",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

// ==========================================================
// FINAL POLISHED UI: NewsSection
// ==========================================================
@Composable
fun NewsSection(
    articles: List<NewsArticleUI>,
    onNewsClick: (encodedUrl: String) -> Unit,
    onViewMoreClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // --- Header Section (DIRAPIKAN) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            // Pastikan elemen kiri & kanan mentok ke ujung
            horizontalArrangement = Arrangement.SpaceBetween,
            // [KUNCI] Pastikan teks sejajar tengah secara vertikal
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Health Updates",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = Color(0xFF1A1C24)
            )

            // Menggunakan TextButton agar area sentuh pas & alignment otomatis rapi
            TextButton(
                onClick = onViewMoreClick,
                // Mengurangi padding bawaan agar teks rapat ke kanan
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp) // Tinggi fix agar tidak loncat
            ) {
                Text(
                    text = "Lihat Semua",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp)) // Jarak header ke list dikurangi sedikit agar menyatu

        // --- List Artikel ---
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            articles.take(3).forEach { article ->
                NewsHomeItem(article = article, onClick = {
                    article.articleUrl?.let { url ->
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        onNewsClick(encodedUrl)
                    }
                })
            }
        }
    }
}

// ==========================================================
// UI IMPROVEMENT: NewsHomeItem (Card Style)
// ==========================================================
@Composable
fun NewsHomeItem(article: NewsArticleUI, onClick: () -> Unit) {
    // Container Kartu dengan Shadow Halus
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp) // Tinggi sedikit ditambah agar elemen tidak sesak
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp), // Sudut membulat modern
        colors = CardDefaults.cardColors(containerColor = Color.White), // Background PUTIH BERSIH
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // Shadow agar kartu "pop-out"
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- 1. Gambar (Full Height di Kiri) ---
            AsyncImage(
                model = article.imageUrl,
                contentDescription = article.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(110.dp) // Lebar gambar = Tinggi kartu (Jadi Kotak)
                    .fillMaxHeight(),
                placeholder = painterResource(id = R.drawable.background3), // Pastikan resource ini ada
                error = painterResource(id = R.drawable.background3)
            )

            // --- 2. Konten Teks (Di Kanan) ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp) // Padding dalam teks lebih lega
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Judul Berita
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp, // Jarak antar baris teks
                        fontSize = 16.sp
                    ),
                    color = Color(0xFF2D3436), // Abu Tua Premium
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Footer (Sumber Berita & Waktu/Icon)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tag Sumber Berita
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), // Background tag transparan
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = article.source.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Composable helper (private) untuk menata [ActionButton] dalam satu baris.
 * @param actions Daftar [ActionItem] yang akan ditampilkan.
 * @param onActionClick Callback yang dipanggil saat tombol diklik, membawa label tombol.
 */
@Composable
private fun ActionButtonsRow(
    actions: List<ActionItem>,
    onActionClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween, // Jarak antar tombol rata
        verticalAlignment = Alignment.Top
    ) {
        actions.forEach { action ->
            ActionButton(
                label = action.label,
                icon = action.icon,
                onClick = { onActionClick(action.label) }
            )
        }
    }
}

/**
 * Composable helper (private) untuk satu tombol aksi (Ikon + Label).
 * @param label Teks di bawah ikon.
 * @param icon Ikon yang ditampilkan.
 * @param onClick Aksi saat diklik.
 */
@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(85.dp)
    ) {
        // --- BUTTON LINGKARAN (INTERAKTIF) ---
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color(0xFFF2F2F7), // <-- iOS System Gray 6 (Abu sangat muda/tipis)
            contentColor = MaterialTheme.colorScheme.primary, // Warna Ikon Biru/Primary
            modifier = Modifier.size(56.dp) // Ukuran lingkaran sedikit lebih besar agar pas di jempol
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- LABEL TEKS ---
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.1).sp
            ),
            color = Color.Black, // Teks Hitam
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 16.sp
        )
    }
}