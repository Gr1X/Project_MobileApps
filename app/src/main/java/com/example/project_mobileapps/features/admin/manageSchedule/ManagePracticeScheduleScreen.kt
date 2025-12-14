package com.example.project_mobileapps.features.admin.manageSchedule

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_mobileapps.data.model.DailyScheduleData
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePracticeScheduleScreen(viewModel: ManagePracticeScheduleViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSaveConfirmation by remember { mutableStateOf(false) }

    if (showSaveConfirmation) {
        ConfirmationBottomSheet(
            onDismiss = { showSaveConfirmation = false },
            onConfirm = {
                showSaveConfirmation = false
                viewModel.saveSchedule(context)
            },
            title = "Simpan Perubahan Jadwal?",
            text = "Perubahan akan langsung diterapkan pada sistem antrian otomatis."
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Praktik", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSaveConfirmation = true },
                icon = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Save, "Simpan")
                    }
                },
                text = { Text(if (uiState.isSaving) "Menyimpan..." else "Simpan Perubahan") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                expanded = !uiState.isSaving
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- HEADER INFO ---
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Sistem Otomatis",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Klinik akan Buka/Tutup & Istirahat otomatis mengikuti jam di bawah ini.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // --- BAGIAN 1: DURASI & WAKTU ---
            item {
                Text(
                    "Parameter Antrian",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimeConfigCard(
                        title = "Batas Panggil",
                        desc = "Toleransi telat",
                        value = uiState.callTimeLimit,
                        unit = "Menit",
                        icon = Icons.Default.Timer,
                        color = Color(0xFFFF9800),
                        onValueChange = viewModel::onCallTimeLimitChange,
                        modifier = Modifier.weight(1f)
                    )

                    TimeConfigCard(
                        title = "Estimasi Layanan",
                        desc = "Rata-rata per pasien",
                        value = uiState.estimatedServiceTime,
                        unit = "Menit",
                        icon = Icons.Default.AccessTime,
                        color = Color(0xFF2196F3),
                        onValueChange = viewModel::onServiceTimeChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- BAGIAN 2: JADWAL MINGGUAN ---
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Jadwal Mingguan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(uiState.schedules) { schedule ->
                    ScheduleDayCard(
                        scheduleData = schedule,
                        onStatusChange = { isOpen ->
                            viewModel.onStatusChange(schedule.dayOfWeek, isOpen)
                        },
                        onTimeChange = { hour, minute, isStartTime ->
                            viewModel.onTimeChange(schedule.dayOfWeek, hour, minute, isStartTime)
                        },
                        onBreakStatusChange = { isEnabled ->
                            viewModel.onBreakStatusChange(schedule.dayOfWeek, isEnabled)
                        },
                        onBreakTimeChange = { hour, minute, isStartTime ->
                            viewModel.onBreakTimeChange(schedule.dayOfWeek, hour, minute, isStartTime)
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// --- COMPONENTS ---

@Composable
fun TimeConfigCard(
    title: String,
    desc: String,
    value: Int,
    unit: String,
    icon: ImageVector,
    color: Color,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.labelSmall, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // Kontrol Plus/Minus Modern
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(2.dp)
            ) {
                IconButton(onClick = { onValueChange(-1) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { onValueChange(1) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                }
            }
            Text(unit, style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
fun ScheduleDayCard(
    scheduleData: DailyScheduleData,
    onStatusChange: (Boolean) -> Unit,
    onTimeChange: (hour: Int, minute: Int, isStartTime: Boolean) -> Unit,
    onBreakStatusChange: (Boolean) -> Unit,
    onBreakTimeChange: (hour: Int, minute: Int, isStartTime: Boolean) -> Unit
) {
    val context = LocalContext.current
    val isOpen = scheduleData.isOpen
    // Animasi background warna kartu
    val backgroundColor by animateColorAsState(
        if (isOpen) MaterialTheme.colorScheme.surface else Color(0xFFF5F5F5),
        label = "bgColor"
    )
    val borderColor = if (isOpen) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent

    // Rotasi panah untuk indikasi expand (meski konten selalu visible jika open, ini untuk estetika)
    val arrowRotation by animateFloatAsState(if (isOpen) 180f else 0f, label = "rotation")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(if (isOpen) 2.dp else 0.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // HEADER ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Indikator HARI (Bulat warna)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isOpen) MaterialTheme.colorScheme.primaryContainer else Color.LightGray.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = scheduleData.dayOfWeek.take(1),
                            fontWeight = FontWeight.Bold,
                            color = if (isOpen) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = scheduleData.dayOfWeek,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOpen) MaterialTheme.colorScheme.onSurface else Color.Gray
                        )
                        Text(
                            text = if (isOpen) "Buka: ${scheduleData.startTime} - ${scheduleData.endTime}" else "Tutup / Libur",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOpen) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }

                Switch(
                    checked = isOpen,
                    onCheckedChange = onStatusChange,
                    thumbContent = {
                        if (isOpen) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp))
                        } else {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp))
                        }
                    },
                    modifier = Modifier.scale(0.8f)
                )
            }

            // KONTEN EXPANDABLE (JAM & ISTIRAHAT)
            AnimatedVisibility(
                visible = isOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // 1. JAM OPERASIONAL UTAMA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Jam Operasional", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeChipLarge(
                            label = "Buka",
                            time = scheduleData.startTime,
                            modifier = Modifier.weight(1f),
                            onClick = { showTimePicker(context, scheduleData.startTime) { h, m -> onTimeChange(h, m, true) } }
                        )

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp).size(16.dp)
                        )

                        TimeChipLarge(
                            label = "Tutup",
                            time = scheduleData.endTime,
                            modifier = Modifier.weight(1f),
                            onClick = { showTimePicker(context, scheduleData.endTime) { h, m -> onTimeChange(h, m, false) } }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. JAM ISTIRAHAT (SECTION)
                    Surface(
                        color = Color(0xFFFFF8E1), // Kuning lembut untuk highlight istirahat
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE082).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Restaurant, null, tint = Color(0xFFF57F17), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Jam Istirahat", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF57F17))
                                }
                                Switch(
                                    checked = scheduleData.isBreakEnabled,
                                    onCheckedChange = onBreakStatusChange,
                                    modifier = Modifier.scale(0.7f),
                                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFFFBC02D))
                                )
                            }

                            // Input Jam Istirahat (Muncul jika enabled)
                            AnimatedVisibility(visible = scheduleData.isBreakEnabled) {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TimeChipSmall(
                                            time = scheduleData.breakStartTime,
                                            modifier = Modifier.weight(1f),
                                            onClick = { showTimePicker(context, scheduleData.breakStartTime) { h, m -> onBreakTimeChange(h, m, true) } }
                                        )
                                        Text(
                                            "-",
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF57F17)
                                        )
                                        TimeChipSmall(
                                            time = scheduleData.breakEndTime,
                                            modifier = Modifier.weight(1f),
                                            onClick = { showTimePicker(context, scheduleData.breakEndTime) { h, m -> onBreakTimeChange(h, m, false) } }
                                        )
                                    }
                                }
                            }
                            if (!scheduleData.isBreakEnabled) {
                                Text(
                                    "Tidak ada istirahat (Non-stop)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- REUSABLE TIME CHIPS ---

@Composable
fun TimeChipLarge(label: String, time: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun TimeChipSmall(time: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE082)),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF57F17) // Orange Gelap
            )
        }
    }
}

fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)

fun showTimePicker(context: android.content.Context, currentTime: String, onTimeSelected: (Int, Int) -> Unit) {
    val parts = currentTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    TimePickerDialog(
        context,
        { _, hour, minute -> onTimeSelected(hour, minute) },
        initialHour,
        initialMinute,
        true
    ).show()
}