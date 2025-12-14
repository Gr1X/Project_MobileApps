// File: features/admin/manageSchedule/AdminQueueMonitorScreen.kt
package com.example.project_mobileapps.features.admin.manageSchedule

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.project_mobileapps.R
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.model.Role
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet
import com.example.project_mobileapps.ui.components.QrScannerScreen
import com.example.project_mobileapps.ui.components.ToastManager
import com.example.project_mobileapps.ui.components.ToastType
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- ACTION SEALED CLASS ---
sealed class ConfirmationAction {
    data class Finish(val patient: PatientQueueDetails) : ConfirmationAction()
    data class Arrive(val patient: PatientQueueDetails) : ConfirmationAction()
    object CallNext : ConfirmationAction()
    data class Cancel(val patient: PatientQueueDetails) : ConfirmationAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminQueueMonitorScreen(
    viewModel: AdminQueueMonitorViewModel,
    currentUserRole: Role?,
    onNavigateToHistory: (String) -> Unit,
    onNavigateToMedicalRecord: (String, String, String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // State Lokal
    var showScanner by remember { mutableStateOf(false) }
    var isProcessingQr by remember { mutableStateOf(false) }
    var activeConfirmation by remember { mutableStateOf<ConfirmationAction?>(null) }
    var patientToView by remember { mutableStateOf<PatientQueueDetails?>(null) }

    // Helper Date (Safe)
    val todayDate = remember {
        try {
            SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date())
        } catch (e: Exception) {
            SimpleDateFormat("dd/MM/yyyy").format(Date())
        }
    }

    // --- INITIAL FILTER SETTING ---
    LaunchedEffect(Unit) {
        viewModel.filterByStatus(QueueStatus.MENUNGGU)
    }

    // --- FUNGSI INTENT (WHATSAPP & CALL) ---
    fun openWhatsApp(phoneNumber: String) {
        try {
            var formattedNumber = phoneNumber.trim()
            if (formattedNumber.startsWith("0")) {
                formattedNumber = "62" + formattedNumber.substring(1)
            } else if (formattedNumber.startsWith("+62")) {
                formattedNumber = formattedNumber.substring(1)
            }

            val url = "https://api.whatsapp.com/send?phone=$formattedNumber"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            context.startActivity(i)
        } catch (e: Exception) {
            ToastManager.showToast("WhatsApp tidak ditemukan", ToastType.ERROR)
        }
    }

    fun makeCall(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:$phoneNumber")
            context.startActivity(intent)
        } catch (e: Exception) {
            ToastManager.showToast("Gagal membuka telepon", ToastType.ERROR)
        }
    }

    // --- DIALOG SCANNER ---
    if (showScanner) {
        Dialog(
            onDismissRequest = { showScanner = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                QrScannerScreen(
                    onQrCodeScanned = { scannedId ->
                        if (!isProcessingQr) {
                            isProcessingQr = true
                            showScanner = false
                            viewModel.processQrCode(scannedId)
                        }
                    }
                )
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(250.dp).border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp)))
                }
                IconButton(
                    onClick = { showScanner = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, "Tutup", tint = Color.White)
                }
                Text(
                    "Arahkan kamera ke QR Code Pasien",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    // --- BOTTOM SHEET KONFIRMASI ---
    activeConfirmation?.let { action ->
        val (title, msg, onConfirm) = when (action) {
            is ConfirmationAction.Cancel -> Triple("Batalkan Antrian?", "Hapus pasien dari antrian hari ini?", { viewModel.cancelPatientQueue(action.patient) })
            is ConfirmationAction.CallNext -> Triple("Panggil Pasien?", "Pasien berikutnya akan dipanggil.", { viewModel.callNextPatient() })
            is ConfirmationAction.Arrive -> Triple("Konfirmasi Kehadiran?", "Pasien sudah ada di ruang tunggu?", { viewModel.confirmPatientArrival(action.patient.queueItem.queueNumber) })
            is ConfirmationAction.Finish -> Triple("Selesaikan Sesi?", "Lanjut ke pengisian Rekam Medis.", {
                val item = action.patient.queueItem
                // [PERBAIKAN] Kirim item.userId sebagai parameter ke-4
                onNavigateToMedicalRecord(item.id, item.userName, item.queueNumber.toString(), item.userId)
            })
        }

        ConfirmationBottomSheet(
            onDismiss = { activeConfirmation = null },
            onConfirm = { onConfirm(); activeConfirmation = null },
            title = title, text = msg
        )
    }

    // --- BOTTOM SHEET DETAIL PASIEN ---
    if (patientToView != null) {
        PatientDetailBottomSheet(
            patientDetails = patientToView!!,
            onDismiss = { patientToView = null },
            onCancelClick = { activeConfirmation = ConfirmationAction.Cancel(patientToView!!); patientToView = null },
            onSeeHistoryClick = { patientToView?.user?.uid?.let { onNavigateToHistory(it) } },
            onWhatsAppClick = { patientToView?.user?.phoneNumber?.let { openWhatsApp(it) } },
            onCallClick = { patientToView?.user?.phoneNumber?.let { makeCall(it) } }
        )
    }

    // --- LAYOUT UTAMA ---
    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isProcessingQr = false; showScanner = true },
                icon = { Icon(Icons.Outlined.QrCodeScanner, null) },
                text = { Text("Scan QR") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. HEADER
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 16.dp)) {
                    Text(todayDate, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    Text("Monitoring Antrian", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Spacer(Modifier.height(16.dp))
                    TopStatsGrid(uiState)
                }
            }

            // 2. ACTIVE STATUS
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        "Status Aktif",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(Modifier.height(12.dp))

                    MainActionSection(
                        uiState = uiState,
                        onAction = { activeConfirmation = it },
                        currentUserRole = currentUserRole,
                        viewModel = viewModel,
                        onWhatsAppClick = { openWhatsApp(it) },
                        onCallClick = { makeCall(it) }
                    )
                }
            }

            // 3. DAFTAR ANTRIAN & FILTER
            item {
                Column {
                    Text(
                        "Daftar Pasien",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(12.dp))

                    // Safe Scrollable Filter
                    FilterChipsRow(
                        options = uiState.filterOptions,
                        selectedOption = uiState.selectedFilter,
                        onOptionSelected = { viewModel.filterByStatus(it) }
                    )
                }
            }

            if (uiState.isLoading) {
                item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (uiState.fullQueueList.isEmpty()) {
                item { EmptyStateView() }
            } else {
                // Hapus parameter KEY untuk menghindari crash ID ganda
                items(uiState.fullQueueList) { patient ->
                    PatientQueueRowEnhanced(
                        patientDetails = patient,
                        onClick = { patientToView = patient },
                        onCancelClick = { activeConfirmation = ConfirmationAction.Cancel(patient) },
                        onHistoryClick = { patient.user?.uid?.let { onNavigateToHistory(it) } }
                    )
                }
            }
        }
    }
}

// =================================================================
// COMPOSABLES VISUAL (SAFE & PROFESSIONAL)
// =================================================================

@Composable
fun TopStatsGrid(uiState: DoctorQueueUiState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Dilayani", uiState.currentlyServing?.queueItem?.queueNumber?.toString() ?: "-", Icons.Outlined.MedicalServices, Color(0xFFE8F5E9), Color(0xFF2E7D32), Modifier.weight(1f))
        StatCard("Menunggu", uiState.totalWaitingCount.toString(), Icons.Outlined.PeopleAlt, Color(0xFFE3F2FD), Color(0xFF1565C0), Modifier.weight(1f))
        StatCard("Next", uiState.nextInLine?.queueItem?.queueNumber?.toString() ?: "-", Icons.Outlined.ConfirmationNumber, Color(0xFFFFF3E0), Color(0xFFEF6C00), Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, color: Color, textColor: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textColor)
            Text(label, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.8f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipsRow(
    options: List<QueueStatus>,
    selectedOption: QueueStatus?,
    onOptionSelected: (QueueStatus?) -> Unit
) {
    // Gunakan Row + HorizontalScroll agar lebih aman dibanding LazyRow di dalam LazyColumn
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedOption == null,
            onClick = { onOptionSelected(null) },
            label = { Text("Semua") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Color.White)
        )
        options.forEach { status ->
            val label = status.name.lowercase().replaceFirstChar { it.titlecase() }
            FilterChip(
                selected = selectedOption == status,
                onClick = { onOptionSelected(status) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Color.White)
            )
        }
    }
}

@Composable
fun MainActionSection(
    uiState: DoctorQueueUiState,
    onAction: (ConfirmationAction) -> Unit,
    currentUserRole: Role?,
    viewModel: AdminQueueMonitorViewModel,
    onWhatsAppClick: (String) -> Unit,
    onCallClick: (String) -> Unit
) {
    when {
        uiState.currentlyServing != null -> {
            val patient = uiState.currentlyServing
            ActivePatientCard(
                statusColor = Color(0xFF2E7D32),
                statusText = "SEDANG KONSULTASI",
                patient = patient,
                showContactButtons = false,
                content = {
                    TimerDisplay(startTime = patient.queueItem.startedAt?.time ?: 0, isCountdown = false)
                    Spacer(Modifier.height(16.dp))
                    if (currentUserRole == Role.DOKTER) {
                        Button(onClick = { onAction(ConfirmationAction.Finish(patient)) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.CheckCircle, null); Spacer(Modifier.width(8.dp)); Text("Selesaikan Sesi")
                        }
                    } else {
                        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Menunggu Dokter...") }
                    }
                }
            )
        }
        uiState.patientCalled != null -> {
            val patient = uiState.patientCalled
            ActivePatientCard(
                statusColor = Color(0xFFEF6C00),
                statusText = "MENUNGGU KEHADIRAN",
                patient = patient,
                showContactButtons = true,
                onWhatsApp = { patient.user?.phoneNumber?.let { onWhatsAppClick(it) } },
                onCall = { patient.user?.phoneNumber?.let { onCallClick(it) } },
                content = {
                    TimerDisplay(startTime = patient.queueItem.calledAt?.time ?: 0, isCountdown = true, limitMinutes = 15)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { onAction(ConfirmationAction.Arrive(patient)) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00))) {
                        Icon(Icons.Default.PersonPinCircle, null); Spacer(Modifier.width(8.dp)); Text("Konfirmasi Hadir")
                    }
                }
            )
        }
        uiState.nextInLine != null -> {
            val patient = uiState.nextInLine
            ActivePatientCard(
                statusColor = Color(0xFF1565C0),
                statusText = "ANTRIAN BERIKUTNYA",
                patient = patient,
                showContactButtons = true,
                onWhatsApp = { patient.user?.phoneNumber?.let { onWhatsAppClick(it) } },
                onCall = { patient.user?.phoneNumber?.let { onCallClick(it) } },
                content = {
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { onAction(ConfirmationAction.CallNext) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Campaign, null); Spacer(Modifier.width(8.dp)); Text("Panggil Pasien")
                    }
                }
            )
        }
        else -> {
            Card(modifier = Modifier.fillMaxWidth().height(150.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)), border = BorderStroke(1.dp, Color.LightGray.copy(0.5f))) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Tidak ada antrian aktif.", color = Color.Gray) }
            }
        }
    }
}

@Composable
fun ActivePatientCard(
    statusColor: Color, statusText: String, patient: PatientQueueDetails,
    showContactButtons: Boolean, onWhatsApp: () -> Unit = {}, onCall: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(statusColor).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(statusText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
            }
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    AsyncImage(
                        model = patient.user?.profilePictureUrl, contentDescription = null,
                        placeholder = painterResource(R.drawable.ic_launcher_foreground),
                        error = painterResource(R.drawable.ic_launcher_foreground),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(70.dp).clip(CircleShape).background(Color.LightGray)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("No. ${patient.queueItem.queueNumber}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = statusColor)
                        Text(patient.user?.name ?: patient.queueItem.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Keluhan: ${patient.queueItem.keluhan}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, maxLines = 1)
                    }
                }
                if (showContactButtons) {
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onWhatsApp, modifier = Modifier.weight(1f)) { Text("WhatsApp") }
                        OutlinedButton(onClick = onCall, modifier = Modifier.weight(1f)) { Text("Call") }
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray.copy(0.2f))
                Spacer(Modifier.height(16.dp))
                content()
            }
        }
    }
}

@Composable
fun TimerDisplay(startTime: Long, isCountdown: Boolean, limitMinutes: Int = 0) {
    var timeText by remember { mutableStateOf("--:--") }
    LaunchedEffect(startTime) {
        val deadline = startTime + (limitMinutes * 60 * 1000)
        while (true) {
            val now = Date().time
            val diff = if (isCountdown) deadline - now else now - startTime
            if (isCountdown && diff < 0) { timeText = "WAKTU HABIS" }
            else {
                val m = TimeUnit.MILLISECONDS.toMinutes(diff)
                val s = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
                timeText = String.format("%02d:%02d", m, s)
            }
            delay(1000L)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(if (isCountdown) "Batas Waktu" else "Durasi", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(timeText, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = if(timeText=="WAKTU HABIS") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun PatientQueueRowEnhanced(
    patientDetails: PatientQueueDetails,
    onClick: () -> Unit,
    onCancelClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp), border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primary.copy(0.1f)) {
                Box(contentAlignment = Alignment.Center) { Text("${patientDetails.queueItem.queueNumber}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(patientDetails.user?.name ?: patientDetails.queueItem.userName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                val age = calculateAge(patientDetails.user?.dateOfBirth)
                Text("Usia: $age Thn • ${patientDetails.queueItem.keluhan}", style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StatusChip(patientDetails.queueItem.status)
            Spacer(Modifier.width(8.dp))
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Menu", tint = Color.Gray) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Lihat Riwayat") }, onClick = { showMenu = false; onHistoryClick() }, leadingIcon = { Icon(Icons.Outlined.History, null) })
                    if (patientDetails.queueItem.status == QueueStatus.MENUNGGU) {
                        DropdownMenuItem(text = { Text("Batalkan", color = Color.Red) }, onClick = { showMenu = false; onCancelClick() }, leadingIcon = { Icon(Icons.Outlined.Cancel, null, tint = Color.Red) })
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Assignment, null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
        Text("Belum ada antrian.", color = Color.Gray)
    }
}

@Composable
fun StatusChip(status: QueueStatus) {
    val (text, color, textColor) = when (status) {
        QueueStatus.MENUNGGU -> Triple("Menunggu", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        QueueStatus.DIPANGGIL -> Triple("Dipanggil", Color(0xFFFFF9C4), Color.Black)
        QueueStatus.DILAYANI -> Triple("Dilayani", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        QueueStatus.SELESAI -> Triple("Selesai", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        QueueStatus.DIBATALKAN -> Triple("Batal", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    }
    Text(text, modifier = Modifier.clip(CircleShape).background(color).padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = textColor)
}

fun calculateAge(dob: String?): String {
    if (dob.isNullOrEmpty()) return "-"
    return try {
        val year = dob.split("-")[0].toInt()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        (currentYear - year).toString()
    } catch (e: Exception) { "-" }
}

// Pastikan Anda menyalin fungsi DetailRow & PatientDetailBottomSheet dari kode sebelumnya jika belum ada
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailBottomSheet(
    patientDetails: PatientQueueDetails,
    onDismiss: () -> Unit,
    onCancelClick: () -> Unit,
    onSeeHistoryClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onCallClick: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Detail Pasien", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            DetailRow(label = "Nama Lengkap", value = patientDetails.user?.name ?: patientDetails.queueItem.userName)
            DetailRow(label = "Nomor Antrian", value = "#${patientDetails.queueItem.queueNumber}")
            DetailRow(label = "Keluhan Awal", value = patientDetails.queueItem.keluhan)
            DetailRow(label = "Tanggal Lahir", value = patientDetails.user?.dateOfBirth ?: "-")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DetailRow(label = "Nomor Telepon", value = patientDetails.user?.phoneNumber ?: "N/A")

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions di Detail Sheet
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onWhatsAppClick, modifier = Modifier.weight(1f)) {
                    // Gunakan icon default jika tidak ada resource khusus
                    Icon(Icons.Default.Call, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("WhatsApp")
                }
                OutlinedButton(onClick = onCallClick, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Call, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Call")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (patientDetails.user != null) {
                Button(
                    onClick = onSeeHistoryClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lihat Rekam Medis (History)")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (patientDetails.queueItem.status == QueueStatus.MENUNGGU || patientDetails.queueItem.status == QueueStatus.DIPANGGIL) {
                OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Batalkan Antrian")
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}