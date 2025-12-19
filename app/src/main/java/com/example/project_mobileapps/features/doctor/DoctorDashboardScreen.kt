// File: features/doctor/DoctorDashboardScreen.kt
package com.example.project_mobileapps.features.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.project_mobileapps.R
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.features.admin.manageSchedule.PatientQueueDetails
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet
import com.example.project_mobileapps.ui.themes.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Enum State untuk Logic Tampilan
enum class PatientCardState {
    SERVING,    // Hijau
    CALLED,     // Kuning/Orange
    READY_NEXT  // Biru (Brand)
}

@Composable
fun DoctorDashboardScreen(
    viewModel: DoctorViewModel,
    onNavigateToQueueList: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by AuthRepository.currentUser.collectAsState()

    // State Konfirmasi
    var showStatusSheet by remember { mutableStateOf(false) }
    var pendingStatusValue by remember { mutableStateOf(false) }

    // --- LOGIC HIERARKI DATA ---
    val activePatient = uiState.currentlyServing
    val calledPatient = uiState.patientCalled
    val nextPatient = if (activePatient == null && calledPatient == null) uiState.topQueueList.firstOrNull() else null

    val listStartIndex = if (nextPatient != null) 1 else 0
    val queueListToShow = uiState.topQueueList.drop(listStartIndex).take(3)

    Scaffold(containerColor = Color(0xFFF8F9FA)) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. HEADER
            item {
                DoctorProfileHeader(
                    greeting = uiState.greeting,
                    name = uiState.doctorName,
                    photoUrl = currentUser?.profilePictureUrl
                )
            }

            // 2. STATUS PRAKTIK (Desain Baru: Control Center)
            item {
                PracticeControlCard(
                    isOpen = uiState.practiceStatus?.isPracticeOpen ?: false,
                    operatingHours = uiState.operatingHours,
                    onToggle = { isToggled ->
                        pendingStatusValue = isToggled
                        showStatusSheet = true
                    }
                )
            }

            // 3. STATISTIK
            item {
                Text(
                    "Ringkasan Hari Ini",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardStatItem(
                        modifier = Modifier.weight(1f),
                        label = "Menunggu",
                        value = uiState.waitingCount.toString(),
                        icon = Icons.Outlined.People,
                        color = StatusWarning
                    )
                    DashboardStatItem(
                        modifier = Modifier.weight(1f),
                        label = "Selesai",
                        value = uiState.finishedCount.toString(),
                        icon = Icons.Outlined.CheckCircle,
                        color = StatusSuccess
                    )
                }
            }

            // 4. HERO CARD (Status Ruangan)
            item {
                SectionHeader("Status Ruangan", "Monitor >", onNavigateToQueueList)
                Spacer(modifier = Modifier.height(12.dp))

                when {
                    activePatient != null -> HeroPatientCard(activePatient, PatientCardState.SERVING, onNavigateToQueueList)
                    calledPatient != null -> HeroPatientCard(calledPatient, PatientCardState.CALLED, onNavigateToQueueList)
                    nextPatient != null -> HeroPatientCard(nextPatient, PatientCardState.READY_NEXT, onNavigateToQueueList)
                    else -> EmptyStateHero()
                }
            }

            // 5. LIST ANTRIAN
            if (queueListToShow.isNotEmpty()) {
                item {
                    Text(
                        "Antrian Berikutnya",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                }
                items(queueListToShow) { item ->
                    AdminStyleQueueItem(item)
                }
            }
        }

        // Bottom Sheet Confirmation
        if (showStatusSheet) {
            val isOpening = pendingStatusValue
            ConfirmationBottomSheet(
                onDismiss = { showStatusSheet = false },
                onConfirm = {
                    viewModel.toggleQueue(pendingStatusValue)
                    showStatusSheet = false
                },
                title = if (isOpening) "Buka Praktik?" else "Tutup Praktik?",
                text = if (isOpening) "Pasien dapat mengambil antrian baru." else "Sistem tidak akan menerima antrian baru."
            )
        }
    }
}

// ===========================================================================
// COMPONENT IMPLEMENTATION (REFINED UI)
// ===========================================================================

@Composable
fun SectionHeader(title: String, actionText: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = actionText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = BrandPrimary,
            modifier = Modifier.clickable { onAction() }
        )
    }
}

/**
 * Kartu Status Praktik Baru (Modern Control Center)
 * Lebih informatif, layout rapi, dan ada indikator visual background.
 */
@Composable
fun PracticeControlCard(isOpen: Boolean, operatingHours: String, onToggle: (Boolean) -> Unit) {
    val backgroundColor = if (isOpen) Color(0xFFECFDF5) else Color(0xFFF1F5F9) // Hijau Muda vs Abu
    val accentColor = if (isOpen) Color(0xFF10B981) else Color(0xFF64748B) // Hijau vs Abu Tua
    val icon = if (isOpen) Icons.Filled.MeetingRoom else Icons.Filled.DoorFront
    val statusText = if (isOpen) "PRAKTIK SEDANG BUKA" else "PRAKTIK DITUTUP"

    // Tanggal Hari Ini
    val todayDate = remember { SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID")).format(Date()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background Decoration Icon (Transparan)
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.08f),
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 20.dp)
            )

            Column(modifier = Modifier.padding(20.dp)) {
                // Baris Atas: Chip Status & Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Chip
                    Surface(
                        color = accentColor,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Circle, null, tint = Color.White, modifier = Modifier.size(8.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Switch
                    Switch(
                        checked = isOpen,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.LightGray
                        ),
                        modifier = Modifier.scale(0.9f)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Baris Bawah: Jam & Tanggal
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccessTime, null, tint = accentColor, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = operatingHours,
                            style = MaterialTheme.typography.headlineSmall, // Font Besar untuk Jam
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = todayDate,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorProfileHeader(greeting: String, name: String, photoUrl: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = greeting, style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(BrandPrimary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )
            }
        }
    }
}

@Composable
fun DashboardStatItem(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(40.dp).background(color.copy(0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
fun HeroPatientCard(
    patient: PatientQueueDetails,
    state: PatientCardState,
    onClick: () -> Unit
) {
    val (accentColor, statusText) = when(state) {
        PatientCardState.SERVING -> StatusSuccess to "SEDANG DIPERIKSA"
        PatientCardState.CALLED -> StatusWarning to "DIPANGGIL..."
        PatientCardState.READY_NEXT -> BrandPrimary to "ANTRIAN BERIKUTNYA"
        else -> Color.Gray to "-"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(accentColor).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(statusText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
            }
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("No. Antrian", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("${patient.queueItem.queueNumber}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = accentColor)
                }
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray.copy(0.5f)))
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(patient.user?.name ?: patient.queueItem.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Keluhan: ${patient.queueItem.keluhan}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.LightGray)
            }
        }
    }
}

@Composable
fun AdminStyleQueueItem(item: PatientQueueDetails) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(Color(0xFFF1F5F9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("${item.queueItem.queueNumber}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.user?.name ?: item.queueItem.userName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(item.queueItem.keluhan, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Surface(color = StatusWarning.copy(0.1f), shape = RoundedCornerShape(6.dp)) {
                Text("Menunggu", style = MaterialTheme.typography.labelSmall, color = StatusWarning, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
fun EmptyStateHero() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Outlined.Inbox, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tidak ada antrian aktif", color = TextSecondary)
        }
    }
}