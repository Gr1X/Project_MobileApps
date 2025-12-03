package com.example.project_mobileapps.features.patient.home

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.repo.NotificationRepository
import kotlinx.coroutines.delay
import java.util.Date
import java.util.concurrent.TimeUnit
import com.example.project_mobileapps.ui.components.FeaturedDoctorCard

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
    onNewsClick: () -> Unit,
    onSmartMealPlanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showNotificationMenu by remember { mutableStateOf(false) }
    val notifications by NotificationRepository.notificationsFlow.collectAsState()
    val hasUnreadNotifications = notifications.any { !it.isRead }
    val haptic = LocalHapticFeedback.current

    // --- 1. TAMBAHKAN STATE UNTUK DIALOG ---
    var showMealPlanDialog by remember { mutableStateOf(false) }

    // --- 2. BUAT DIALOG-NYA ---
    if (showMealPlanDialog) {
        AlertDialog(
            onDismissRequest = { showMealPlanDialog = false },
            title = { Text("Fitur Segera Hadir!") },
            text = { Text("Fitur ini nantinya akan memberikan rekomendasi rencana makan (meal plan) yang dipersonalisasi menggunakan machine learning untuk memprediksi kebutuhan nutrisi Anda.") },
            confirmButton = {
                TextButton(onClick = { showMealPlanDialog = false }) {
                    Text("Mengerti")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {  },

                navigationIcon = {
                    IconButton(
                        onClick = {onProfileClick},
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Profil",
                            modifier = Modifier.size(32.dp)
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

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

            Spacer(Modifier.height(24.dp))

            val actionItems = listOf(
                ActionItem("Booking", Icons.Outlined.LocalPharmacy),
                ActionItem("News", Icons.Outlined.Newspaper),
                ActionItem("Smart Meal Plan", Icons.Outlined.RestaurantMenu)
            )

            Text("Quick Actions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            ActionButtonsRow(
                actions = actionItems,
                onActionClick = { label ->
                    // PERBAIKAN DI SINI: Samakan String dengan label di atas
                    when (label) {
                        "Booking" -> {
                            // Masukkan aksi booking di sini, misal:
                            onNavigateToQueue()
                            // atau jika belum ada navigasi:
                            // Toast.makeText(context, "Fitur Booking", Toast.LENGTH_SHORT).show()
                        }
                        "News" -> onNewsClick() // Ubah dari "Berita Kesehatan" jadi "News"
                        "Smart Meal Plan" -> {
                            // Pastikan Anda mem-passing lambda navigasi untuk ini di parameter HomeScreen
                            // Atau panggil langsung jika navController tersedia di scope ini (tapi best practice-nya via callback)
                            onSmartMealPlanClick()
                        }
                        else -> Toast.makeText(context, "$label diklik!", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            if (uiState.doctor != null) {
                Text("Appointment", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                FeaturedDoctorCard(
                    doctor = uiState.doctor,
                    practiceStatus = uiState.practiceStatus,
                    upcomingQueue = uiState.upcomingQueue,
                    availableSlots = uiState.availableSlots,
                    onTakeQueueClick = onTakeQueueClick
                )
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
        horizontalArrangement = Arrangement.SpaceAround
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
        modifier = Modifier
            .width(90.dp)
            .clickable(onClick = onClick)
            .clip(MaterialTheme.shapes.medium)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center, // 2. Rata tengah (wajib untuk 2 baris)
            maxLines = 2, // 3. Batasi maksimal 2 baris
            minLines = 2, // 4. TRICK PENTING: Paksa minimal 2 baris agar tinggi semua tombol sama rata (sejajar)
            overflow = TextOverflow.Ellipsis, // Jika lebih dari 2 baris, kasih titik-titik (...)
            lineHeight = 14.sp // 5. Atur jarak antar baris agar tidak terlalu renggang
        )
    }
}