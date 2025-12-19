// File: features/admin/reports/PatientHistoryDetailScreen.kt
package com.example.project_mobileapps.features.admin.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.project_mobileapps.data.model.HistoryItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.ui.themes.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHistoryDetailScreen(
    viewModel: PatientHistoryDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val historyList by viewModel.historyList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val patientProfile by viewModel.patientProfile.collectAsState()

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            TopAppBar(
                title = {
                    Text("Riwayat Medis Pasien", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. HEADER PROFIL PASIEN
                item {
                    if (patientProfile != null) {
                        PatientProfileCardSummary(patientProfile!!)
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp))
                                Text("Memuat data profil...", color = Color.Gray)
                            }
                        }
                    }
                }

                // 2. JUDUL SECTION
                item {
                    Text(
                        "Daftar Kunjungan (${historyList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // 3. LIST HISTORY
                if (historyList.isEmpty()) {
                    item { EmptyHistoryState() }
                } else {
                    items(items = historyList, key = { it.visitId }) { item ->
                        HistoryVisitCard(
                            item = item,
                            onClick = {
                                if (item.status == QueueStatus.SELESAI) {
                                    onNavigateToDetail(item.visitId)
                                }
                            }
                        )
                    }
                }

                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

// =================================================================
// COMPONENTS FIXED
// =================================================================

@Composable
fun PatientProfileCardSummary(user: User) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // --- HEADER: FOTO & NAMA ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!user.profilePictureUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = user.profilePictureUrl,
                        contentDescription = "Foto Profil",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_launcher_foreground)
                    )
                } else {
                    // Fallback Inisial
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(BrandPrimary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(16.dp))

            // --- GRID INFO (FIX LAYOUT MENABRAK) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Kolom 1: Gender (25%)
                Box(modifier = Modifier.weight(0.25f)) {
                    ProfileInfoItem(
                        label = "Gender",
                        value = user.gender.name.lowercase().replaceFirstChar { it.uppercase() },
                        icon = Icons.Outlined.Person
                    )
                }

                // Kolom 2: Lahir (35%)
                Box(modifier = Modifier.weight(0.35f)) {
                    ProfileInfoItem(
                        label = "Lahir",
                        value = user.dateOfBirth,
                        icon = Icons.Outlined.Cake
                    )
                }

                // Kolom 3: Telepon (40% - Paling Lebar)
                Box(modifier = Modifier.weight(0.4f)) {
                    ProfileInfoItem(
                        label = "Telepon",
                        value = user.phoneNumber,
                        icon = Icons.Outlined.Phone
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileInfoItem(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (value.isBlank()) "-" else value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun HistoryVisitCard(item: HistoryItem, onClick: () -> Unit) {
    val isFinished = item.status == QueueStatus.SELESAI

    // Parsing Tanggal dengan opsi Debug
    val (day, month, debugRaw) = remember(item.visitDate) {
        parseDateRobust(item.visitDate)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isFinished, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. KOTAK TANGGAL
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .width(52.dp)
                    .height(56.dp)
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    fontSize = 18.sp
                )
                Text(
                    text = month,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 2. KELUHAN
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Keluhan: ${item.initialComplaint}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                StatusChipModern(item.status)

                // [DEBUG MODE] Tampilkan format asli jika parsing gagal ("?" atau "BLN")
                if (month == "?" || month == "BLN") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Format: $debugRaw",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Red,
                        fontSize = 9.sp
                    )
                }
            }

            if (isFinished) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Detail",
                    tint = BrandPrimary.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * FUNGSI PARSING SUPER LENGKAP
 * Menangani format Bahasa Inggris (Dec) dan Indonesia (Des)
 */
fun parseDateRobust(dateStringRaw: String): Triple<String, String, String> {
    val dateString = dateStringRaw.trim()
    if (dateString.isBlank()) return Triple("?", "BLN", "Empty")

    // DAFTAR FORMAT (Inggris & Indo)
    val formatsToCheck = listOf(
        // English (US/Global) - Paling sering jadi masalah
        SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US), // Default Java Date
        SimpleDateFormat("dd MMM yyyy", Locale.US), // 15 Dec 2025
        SimpleDateFormat("dd MMMM yyyy", Locale.US), // 15 December 2025
        SimpleDateFormat("dd-MMM-yyyy", Locale.US), // 15-Dec-2025

        // Indonesia (ID)
        SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")),
        SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")),
        SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")),

        // Numeric Only (Universal)
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    )

    for (format in formatsToCheck) {
        try {
            val date = format.parse(dateString)
            if (date != null) {
                // Output Format Selalu Indonesia (Des, Jan, dst)
                val dayFormat = SimpleDateFormat("dd", Locale("id", "ID"))
                val monthFormat = SimpleDateFormat("MMM", Locale("id", "ID"))

                return Triple(
                    dayFormat.format(date),
                    monthFormat.format(date).uppercase(),
                    dateString
                )
            }
        } catch (e: Exception) { continue }
    }

    // Fallback Terakhir: Coba ambil angka secara manual
    // Misal: "15/12/2025" -> Ambil "15" dan "12"
    return try {
        val parts = dateString.split(" ", "-", "/", ".")
        if (parts.size >= 2 && parts[0].all { it.isDigit() }) {
            // Asumsi format: TANGGAL [separator] BULAN
            val d = parts[0].take(2)
            val m = parts[1].take(3).uppercase() // Ambil 3 huruf/angka bulan
            Triple(d, m, dateString)
        } else {
            Triple("?", "BLN", dateString)
        }
    } catch (e: Exception) {
        Triple("?", "BLN", dateString)
    }
}

@Composable
fun StatusChipModern(status: QueueStatus) {
    val (bgColor, textColor, text) = when (status) {
        QueueStatus.SELESAI -> Triple(StateSuccess.copy(alpha = 0.1f), StateSuccess, "Selesai")
        QueueStatus.DIBATALKAN -> Triple(StateError.copy(alpha = 0.1f), StateError, "Batal")
        QueueStatus.MENUNGGU -> Triple(StateWarning.copy(alpha = 0.1f), StateWarning, "Menunggu")
        QueueStatus.DIPANGGIL -> Triple(StateWarning.copy(alpha = 0.1f), StateWarning, "Dipanggil")
        QueueStatus.DILAYANI -> Triple(BrandPrimary.copy(alpha = 0.1f), BrandPrimary, "Diperiksa")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun EmptyHistoryState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Belum ada riwayat kunjungan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
    }
}