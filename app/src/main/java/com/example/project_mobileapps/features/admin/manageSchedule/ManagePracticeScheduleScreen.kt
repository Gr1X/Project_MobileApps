// File: features/admin/manageSchedule/ManagePracticeScheduleScreen.kt
package com.example.project_mobileapps.features.admin.manageSchedule

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_mobileapps.data.model.DailyScheduleData
import com.example.project_mobileapps.ui.themes.TextPrimary

/**
 * Layar Manajemen Jadwal Praktik
 * - Removed: UI Jam Istirahat
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePracticeScheduleScreen(
    viewModel: ManagePracticeScheduleViewModel
) {
    // Collect State dari ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.saveSchedule(context) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Save, contentDescription = "Simpan")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF4F6F8))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Spacer(Modifier.height(4.dp))
                    Text("Jadwal Dokter",  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp), color = TextPrimary)
                }
            }

            // --- BAGIAN 1: ESTIMASI WAKTU (SLIDER) ---
            item {
                ServiceTimeCard(
                    estimatedTime = uiState.estimatedServiceTime,
                    onValueChange = { viewModel.updateServiceTime(it) }
                )
            }

            // --- BAGIAN 2: BATAS WAKTU PANGGIL (ADD/MINUS BUTTONS) ---
            item {
                CallLimitCard(
                    limitTime = uiState.callTimeLimit,
                    onIncrease = { viewModel.onCallTimeLimitChange(1) },
                    onDecrease = { viewModel.onCallTimeLimitChange(-1) }
                )
            }

            item {
                Text(
                    text = "Jadwal Operasional",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
            }

            // --- BAGIAN 3: LIST JADWAL (Tanpa Istirahat) ---
            items(uiState.schedules) { schedule ->
                ScheduleCardItem(
                    schedule = schedule,
                    onOpenChange = { isOpen ->
                        viewModel.onStatusChange(schedule.dayOfWeek, isOpen)
                    },
                    onTimeClick = { isStart ->
                        showTimePicker(context, if (isStart) schedule.startTime else schedule.endTime) { h, m ->
                            viewModel.onTimeChange(schedule.dayOfWeek, h, m, isStart)
                        }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// =================================================================
// SUB-COMPONENTS
// =================================================================

@Composable
fun ServiceTimeCard(estimatedTime: Int, onValueChange: (Int) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Estimasi Per Pasien", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Waktu rata-rata pemeriksaan dokter", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("$estimatedTime Menit", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            Slider(
                value = estimatedTime.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 5f..60f,
                steps = 10,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CallLimitCard(limitTime: Int, onIncrease: () -> Unit, onDecrease: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Batas Waktu Panggil", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Toleransi menunggu pasien sebelum dilewati", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDecrease,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.Remove, null)
                }

                Text(
                    text = "$limitTime Menit",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onIncrease,
                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                ) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
fun ScheduleCardItem(
    schedule: DailyScheduleData,
    onOpenChange: (Boolean) -> Unit,
    onTimeClick: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Hari & Switch Buka
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (schedule.isOpen) Color(0xFF4CAF50) else Color.LightGray))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(schedule.dayOfWeek, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Switch(checked = schedule.isOpen, onCheckedChange = onOpenChange, modifier = Modifier.scale(0.8f))
            }

            // Konten (Muncul jika Buka)
            AnimatedVisibility(visible = schedule.isOpen) {
                Column {
                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))

                    // Jam Operasional Only
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Jam Operasional", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TimeChip(time = schedule.startTime, onClick = { onTimeClick(true) })
                        Text("s/d", fontWeight = FontWeight.Bold, color = Color.LightGray)
                        TimeChip(time = schedule.endTime, onClick = { onTimeClick(false) })
                    }
                }
            }
        }
    }
}

// --- UTILS ---
@Composable
fun TimeChip(time: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF5F5F5),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
        modifier = Modifier.clickable { onClick() }.width(100.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
            Text(time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

fun showTimePicker(context: Context, currentTime: String, onTimeSelected: (Int, Int) -> Unit) {
    val parts = currentTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    TimePickerDialog(context, { _, h, m -> onTimeSelected(h, m) }, initialHour, initialMinute, true).show()
}