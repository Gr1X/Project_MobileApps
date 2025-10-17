package com.example.project_mobileapps.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import kotlinx.coroutines.delay
import java.util.Date
import java.util.concurrent.TimeUnit

@Composable
fun QueueChip(
    queueItem: QueueItem,
    isFirstInLine: Boolean
) {
    var timerText by remember { mutableStateOf("") }

    LaunchedEffect(queueItem.status, queueItem.startedAt, queueItem.calledAt) {
        when {
            // KASUS 1: Pasien sedang DILAYANI (timer berjalan naik)
            queueItem.status == QueueStatus.DILAYANI && queueItem.startedAt != null -> {
                while (true) {
                    val diff = Date().time - queueItem.startedAt!!.time
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
                    timerText = String.format("%02d:%02d", minutes, seconds)
                    delay(1000L)
                }
            }
            // KASUS 2: Pasien DIPANGGIL dan berada di paling depan (countdown)
            isFirstInLine && queueItem.status == QueueStatus.DIPANGGIL && queueItem.calledAt != null -> {
                val ONE_MINUTE_IN_MS = 1 * 60 * 1000
                while (true) {
                    val timeSinceCalled = Date().time - queueItem.calledAt!!.time
                    val timeRemaining = ONE_MINUTE_IN_MS - timeSinceCalled
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
                timerText = queueItem.status.name.lowercase().replaceFirstChar { it.titlecase() }
            }
        }
    }

    // Tentukan warna berdasarkan status
    val backgroundColor = when {
        queueItem.status == QueueStatus.DILAYANI -> MaterialTheme.colorScheme.primaryContainer
        isFirstInLine && queueItem.status == QueueStatus.DIPANGGIL -> Color(0xFFFFF9C4) // Kuning terang
        else -> MaterialTheme.colorScheme.surface // Warna surface biasa
    }
    val contentColor = when {
        queueItem.status == QueueStatus.DILAYANI -> MaterialTheme.colorScheme.onPrimaryContainer
        isFirstInLine && queueItem.status == QueueStatus.DIPANGGIL -> Color.Black
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("No.", color = contentColor.copy(alpha = 0.7f), fontSize = 12.sp)
            Text("${queueItem.queueNumber}", color = contentColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(timerText, color = contentColor.copy(alpha = 0.9f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}