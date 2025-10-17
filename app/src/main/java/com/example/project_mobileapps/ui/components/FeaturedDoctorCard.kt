package com.example.project_mobileapps.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.project_mobileapps.data.model.Doctor
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FeaturedDoctorCard(
    doctor: Doctor,
    practiceStatus: PracticeStatus?,
    upcomingQueue: List<QueueItem>,
    availableSlots: Int,
    onTakeQueueClick: () -> Unit
) {
    // --- PERBAIKAN LOGIKA STATUS ---
    val isPracticeOpen = practiceStatus?.isPracticeOpen ?: false
    val statusText = if (isPracticeOpen) "Buka" else "Tutup"
    val statusChipColor = if (isPracticeOpen) Color(0xFFE0F7FA) else Color(0xFFFFEBEE)
    val statusTextColor = if (isPracticeOpen) Color(0xFF00796B) else MaterialTheme.colorScheme.error
    val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column {
            Box {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            color = Color.Transparent, // Biarkan transparan
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = statusChipColor
                        ) {
                            Text(
                                text = statusText,
                                color = statusTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 15.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(currentDate, color = Color.White.copy(alpha = 0.8f))
                    Text(doctor.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    // --- PERBAIKAN TOMBOL ---
                    Button(
                        onClick = onTakeQueueClick,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        enabled = isPracticeOpen, // Tombol dikontrol oleh isPracticeOpen
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (isPracticeOpen) "Ambil Antrian" else "Praktik Tutup")
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.3f))
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
            text = if (slotsAvailable > 0) "$slotsAvailable slot tersedia" else "Penuh",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}