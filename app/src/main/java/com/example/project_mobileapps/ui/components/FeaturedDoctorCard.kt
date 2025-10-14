package com.example.project_mobileapps.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.project_mobileapps.data.model.Doctor
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.google.common.collect.Multimaps.index
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


@Composable
fun FeaturedDoctorCard(
    doctor: Doctor,
    practiceStatus: PracticeStatus?,
    upcomingQueue: List<QueueItem>,
    availableSlots: Int,
    onTakeQueueClick: () -> Unit
) {
    // Data Dinamis
    val statusText = if (practiceStatus?.isPracticeOpen == true) "Open" else "Tutup"
    val statusColor = if (practiceStatus?.isPracticeOpen == true) Color(0xFFE0F7FA) else Color(0xFFFFEBEE)
    val clinicStatusColor = if (practiceStatus?.isPracticeOpen == true) Color(0xFF00796B) else Color.Red
    val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF6A84FF))
    ) {
        Column {
            Box {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            color = Color(0x000000),
                            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                        )
                )

                AsyncImage(
                    model = doctor.photoUrl,
                    contentDescription = doctor.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(170.dp)
                        .offset(y = 20.dp, x = (-8).dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                .padding(horizontal = 15.dp, vertical = 6.dp)
                        ) {
                            Text(statusText, color = clinicStatusColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(currentDate, color = Color.White.copy(alpha = 0.8f))
                    Text(doctor.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    Button(
                        onClick = onTakeQueueClick,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        enabled = practiceStatus?.isPracticeOpen == true
                    ) {
                        Text("Ambil Antrian")
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 20.dp)
                ) {
                    AvailabilityInfo(slotsAvailable = availableSlots)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (upcomingQueue.isEmpty()) {
                        Text("Belum ada antrian.", color = Color.White.copy(alpha = 0.8f))
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(upcomingQueue) { index, queueItem ->
                                QueueChip(
                                    queueItem = queueItem,
                                    // Item pertama (index 0) adalah yang paling depan
                                    isFirstInLine = (index == 0)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =======================================================
// GANTI SELURUH FUNGSI INI
// =======================================================
@Composable
private fun QueueChip(
    queueItem: QueueItem,
    isFirstInLine: Boolean
) {
    var timerText by remember { mutableStateOf("") }

    // Efek ini akan mengelola semua logika timer
    LaunchedEffect(queueItem.status, queueItem.startedAt, queueItem.calledAt) {
        when {
            // KASUS 1: Pasien sedang DILAYANI (timer berjalan naik)
            queueItem.status == com.example.project_mobileapps.data.model.QueueStatus.DILAYANI && queueItem.startedAt != null -> {
                while (true) {
                    val diff = Date().time - queueItem.startedAt!!.time
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
                    timerText = String.format("%02d:%02d", minutes, seconds)
                    delay(1000L)
                }
            }
            // KASUS 2: Pasien DIPANGGIL dan berada di paling depan (countdown 15 menit)
            isFirstInLine && queueItem.status == com.example.project_mobileapps.data.model.QueueStatus.DIPANGGIL && queueItem.calledAt != null -> {
                val FIFTEEN_MINUTES_IN_MS = 1 * 60 * 1000
                while (true) {
                    val timeSinceCalled = Date().time - queueItem.calledAt!!.time
                    val timeRemaining = FIFTEEN_MINUTES_IN_MS - timeSinceCalled
                    if (timeRemaining > 0) {
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemaining)
                        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeRemaining) % 60
                        timerText = String.format("%02d:%02d", minutes, seconds)
                    } else {
                        timerText = "Waktu Habis"
                        break
                    }
                    delay(1000L)
                }
            }
            // KASUS 3: Status lainnya (MENUNGGU, SELESAI, dll)
            else -> {
                timerText = queueItem.status.name
            }
        }
    }

    // Tentukan warna berdasarkan status
    val backgroundColor = when {
        queueItem.status == com.example.project_mobileapps.data.model.QueueStatus.DILAYANI -> MaterialTheme.colorScheme.primaryContainer
        isFirstInLine && queueItem.status == com.example.project_mobileapps.data.model.QueueStatus.DIPANGGIL -> Color(0xFFFFF9C4) // Kuning terang
        else -> Color.White.copy(alpha = 0.3f)
    }
    val contentColor = if (queueItem.status == com.example.project_mobileapps.data.model.QueueStatus.DILAYANI || (isFirstInLine && queueItem.status == com.example.project_mobileapps.data.model.QueueStatus.DIPANGGIL)) Color.Black else Color.White

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No.", color = contentColor, fontSize = 12.sp)
            Text("${queueItem.queueNumber}", color = contentColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(timerText, color = contentColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}
// =======================================================

@Composable
fun AvailabilityInfo(
        slotsAvailable: Int,
        modifier: Modifier = Modifier
    ) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Antrian",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )

        Text(
            text = " • ",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )

        Text(
            text = "$slotsAvailable slots",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

