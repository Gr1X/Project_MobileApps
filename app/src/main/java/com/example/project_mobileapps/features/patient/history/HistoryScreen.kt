// File: features/profile/HistoryScreen.kt
package com.example.project_mobileapps.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.model.HistoryItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.ui.themes.*
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    onHistoryClick: (HistoryItem) -> Unit,
    viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(AppContainer.queueRepository, AuthRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Color(0xFFF8F9FA), // Background Abu Soft (Konsisten Admin)
        topBar = {
            TopAppBar(
                title = {
                    Text("Riwayat Kunjungan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = BrandPrimary
                )
            } else if (uiState.historyList.isEmpty()) {
                EmptyHistoryState() // Menggunakan Style Empty State Admin
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header kecil seperti di Admin
                    item {
                        Text(
                            "Daftar Kunjungan (${uiState.historyList.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(uiState.historyList) { historyItem ->
                        HistoryVisitCard(
                            item = historyItem,
                            onClick = { onHistoryClick(historyItem) }
                        )
                    }

                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }
}

// =================================================================
// COMPONENTS (Diadaptasi dari Admin Style)
// =================================================================

@Composable
fun HistoryVisitCard(item: HistoryItem, onClick: () -> Unit) {
    // Logic klik: Hanya bisa diklik jika SELESAI (untuk melihat rekam medis)
    // Atau sesuaikan dengan kebutuhan user profile (misal melihat detail pembatalan)
    val isClickable = true

    // Parsing Tanggal Robust (Sama seperti Admin)
    val (day, month, _) = remember(item.visitDate) {
        parseDateRobust(item.visitDate)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isClickable, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. KOTAK TANGGAL (Style Admin)
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

            // 2. INFO DOKTER & KELUHAN
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.doctorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Keluhan: ${item.initialComplaint}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                StatusChipModern(item.status)
            }

            // Icon Panah
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Detail",
                tint = BrandPrimary.copy(alpha = 0.5f)
            )
        }
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

/**
 * FUNGSI PARSING ROBUST (Sama dengan Admin)
 * Menangani format Bahasa Inggris (Dec) dan Indonesia (Des) agar tidak error "?"
 */
fun parseDateRobust(dateStringRaw: String): Triple<String, String, String> {
    val dateString = dateStringRaw.trim()
    if (dateString.isBlank()) return Triple("?", "BLN", "Empty")

    // DAFTAR FORMAT (Inggris & Indo)
    val formatsToCheck = listOf(
        // English (US/Global)
        SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US),
        SimpleDateFormat("dd MMM yyyy", Locale.US),
        SimpleDateFormat("dd MMMM yyyy", Locale.US),
        SimpleDateFormat("dd-MMM-yyyy", Locale.US),

        // Indonesia (ID)
        SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")),
        SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")),
        SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")),

        // Numeric Only
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    )

    for (format in formatsToCheck) {
        try {
            val date = format.parse(dateString)
            if (date != null) {
                // Output Format Selalu Indonesia (Des, Jan, dst) untuk konsistensi UI
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

    // Fallback: Parsing Manual String
    return try {
        val parts = dateString.split(" ", "-", "/", ".")
        if (parts.size >= 2 && parts[0].all { it.isDigit() }) {
            val d = parts[0].take(2)
            val m = parts[1].take(3).uppercase()
            Triple(d, m, dateString)
        } else {
            Triple("?", "BLN", dateString)
        }
    } catch (e: Exception) {
        Triple("?", "BLN", dateString)
    }
}