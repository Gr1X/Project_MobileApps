package com.example.project_mobileapps.features.home

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_mobileapps.R
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.ui.components.DoctorCard
import kotlinx.coroutines.delay
import java.util.Date
import java.util.concurrent.TimeUnit
import com.example.project_mobileapps.ui.components.FeaturedDoctorCard


private data class ActionItem(val label: String, val icon: ImageVector)
private data class DayData(val day: String, val date: String, val isSelected: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onDoctorClick: (String) -> Unit,
    onNavigateToQueue: () -> Unit,
    onProfileClick: () -> Unit,
    onTakeQueueClick: () -> Unit,
    onNewsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showNotificationMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {  },

                navigationIcon = {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Profil",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },

                actions = {
                    // Tombol Notifikasi di kanan
                    Box {
                        IconButton(onClick = { showNotificationMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifikasi"
                            )
                        }
                        // Dropdown Menu
                        DropdownMenu(
                            expanded = showNotificationMenu,
                            onDismissRequest = { showNotificationMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Antrian Anda akan segera tiba!") },
                                onClick = { /* Aksi notifikasi 1 */ showNotificationMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Selamat datang di HealthyApp.") },
                                onClick = { /* Aksi notifikasi 2 */ showNotificationMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Jadwal dokter telah diperbarui.") },
                                onClick = { /* Aksi notifikasi 3 */ showNotificationMenu = false }
                            )
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
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "How are you feeling today?",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Light
            )

            // =======================================================
            // TAMBAHKAN BAGIAN BARU DI SINI
            // =======================================================
            Spacer(Modifier.height(24.dp))

            // Definisikan daftar tombol yang ingin ditampilkan
            val actionItems = listOf(
                ActionItem("Berita", Icons.Outlined.Newspaper),
                ActionItem("Lacak Makanan", Icons.Outlined.RestaurantMenu),
                ActionItem("Obat", Icons.Outlined.LocalPharmacy)
            )

            ActionButtonsRow(
                actions = actionItems,
                onActionClick = { label ->
                    when (label) {
                        "Berita" -> onNewsClick()
                        else -> Toast.makeText(context, "$label diklik!", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            // =======================================================
            // PERBAIKI LOGIKA KARTU ANTRIAN DI SINI
            // =======================================================
            if (uiState.activeQueue != null) {
                QueueStatusCard(
                    queue = uiState.activeQueue,
                    onClick = onNavigateToQueue
                )
            }
//            else {
//                CurrentQueueCard(
//                    servingPatient = uiState.currentlyServingPatient,
//                    onTakeQueueClick = onTakeQueueClick
//                )
//            }

            Spacer(Modifier.height(8.dp))

            // =======================================================
            // Bagian Dokter
            // =======================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Jadwal Praktik",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = { /* Aksi "See detail" */ }) {
                    Text("See detail")
                }
            }

            Spacer(Modifier.height(8.dp))

            if (uiState.doctor != null) {
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

@Composable
fun MedicalIcon(
    label: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

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

@Composable
fun CurrentQueueCard(servingPatient: QueueItem?, onTakeQueueClick: () -> Unit) {
    var consultationTime by remember { mutableStateOf("00:00") }

    LaunchedEffect(servingPatient) {
        if (servingPatient?.status == com.example.project_mobileapps.data.model.QueueStatus.DILAYANI && servingPatient.startedAt != null) {
            while (true) {
                val diff = Date().time - servingPatient.startedAt!!.time
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
                consultationTime = String.format("%02d:%02d", minutes, seconds)
                delay(1000L)
            }
        } else {
            consultationTime = "00:00"
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Saat Ini Dilayani",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (servingPatient != null) {
                    Text(
                        "${servingPatient.queueNumber}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Waktu Berjalan: $consultationTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Tidak ada antrian",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Button(onClick = onTakeQueueClick) {
                Text("Ambil Antrian")
            }
        }
    }
}

@Composable
fun PublicQueueInfoCard(status: PracticeStatus?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Antrian",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    "${status?.currentServingNumber ?: 0}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(onClick = onClick) {
                Text("Lihat Antrian")
            }
        }
    }
}

@Composable
private fun ActionButtonsRow(
    actions: List<ActionItem>,
    onActionClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround // Beri jarak yang sama antar tombol
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

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp
        )
    }
}