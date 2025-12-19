package com.example.project_mobileapps.features.admin.dashboard

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.model.DailyScheduleData
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.features.admin.manageSchedule.TopStatsGrid
// [PENTING] Import file Color.kt Anda
import com.example.project_mobileapps.ui.themes.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    onNavigateToSchedule: () -> Unit,
    onNavigateToMonitoring: () -> Unit,
    onNavigateToReports: () -> Unit, // Callback navigasi ke laporan
    viewModel: AdminDashboardViewModel = viewModel(
        factory = AdminDashboardViewModelFactory(AppContainer.queueRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedFilter by remember { mutableStateOf("Harian") }
    val filterOptions = listOf("Harian", "Mingguan", "Bulanan")

    LaunchedEffect(selectedFilter) {
        viewModel.loadChartData(selectedFilter)
    }

    val currentDate = remember {
        SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID")).format(Date())
    }

    Scaffold(
        containerColor = AdminBackground,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(currentDate, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    Text("Dashboard Admin",  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp), color = TextPrimary)
                }
            }

            // 1. OPERATION STATUS
            item {
                ProfessionalStatusCard(
                    practiceStatus = uiState.practiceStatus,
                    schedule = uiState.doctorScheduleToday,
                    onManageSchedule = onNavigateToSchedule
                )
            }

            // 2. STATS GRID
            item {
                Text(
                    "Ringkasan Hari Ini",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(12.dp))
                DashboardStatsGrid(
                    total = uiState.totalPatientsToday,
                    waiting = uiState.patientsWaiting,
                    finished = uiState.patientsFinished
                )
            }

            // 3. ANALYTICS SECTION
            item {
                Column {
                    // HEADER ANALITIK (Judul & Tombol Lihat Laporan)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Analitik Klinik",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )

                        // [TOMBOL BARU] Lihat Selengkapnya
                        TextButton(
                            onClick = onNavigateToReports,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "Lihat Laporan",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = BrandPrimary
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(14.dp), tint = BrandPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // FILTER BUTTONS (Sekarang di baris sendiri agar rapi)
                    TimeRangeFilter(
                        options = filterOptions,
                        selectedOption = selectedFilter,
                        onOptionSelected = { selectedFilter = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.isLoadingChart) {
                        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = BrandPrimary)
                        }
                    } else {
                        WeeklyChartCard(
                            title = uiState.chartTitle.ifEmpty { "Tren Kunjungan" },
                            subtitle = "Periode $selectedFilter",
                            data = uiState.chartData,
                            labels = uiState.chartLabels,
                            trendLabel = uiState.trendLabel,
                            isTrendPositive = uiState.isTrendPositive
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row of Charts (Donut & Demo)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            StatusDonutChartCard(
                                waiting = uiState.patientsWaiting,
                                finished = uiState.patientsFinished,
                                total = uiState.totalPatientsToday
                            )
                        }
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            // PASTIKAN INI MENGAMBIL DATA DARI VIEWMODEL
                            DemographicsCard(
                                maleCount = uiState.maleCount,
                                femaleCount = uiState.femaleCount
                            )
                        }
                    }
                }
            }

            // 4. ACTIVE QUEUE
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Antrian Aktif",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        TextButton(onClick = onNavigateToMonitoring) {
                            Text("Lihat Monitor", color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (uiState.isLoading) {
                        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = BrandPrimary)
                        }
                    } else if (uiState.top5ActiveQueue.isEmpty()) {
                        EmptyStateCard()
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val currentLimit = uiState.practiceStatus?.patientCallTimeLimitMinutes ?: 15
                            uiState.top5ActiveQueue.forEach { queueItem ->
                                QueueListItem(
                                    item = queueItem,
                                    limitMinutes = currentLimit)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
// ==========================================
// COMPONENT: TIME RANGE FILTER
// ==========================================
@Composable
fun TimeRangeFilter(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .background(SurfaceWhite, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            val bgColor = if (isSelected) AdminBackground else Color.Transparent
            val textColor = if (isSelected) TextPrimary else TextSecondary.copy(alpha = 0.7f)
            val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(bgColor)
                    .clickable { onOptionSelected(option) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = fontWeight,
                        fontSize = 10.sp
                    ),
                    color = textColor
                )
            }
        }
    }
}

// ==========================================
// CHART: BAR CHART
// ==========================================
    @Composable
    fun WeeklyChartCard(
        title: String,
        subtitle: String,
        data: List<Int>,
        labels: List<String>,
        trendLabel: String,       // [BARU]
        isTrendPositive: Boolean
        ) {

        val trendColor = if (isTrendPositive) StatusSuccess else StatusError // Hijau / Merah
        val trendIcon = if (isTrendPositive) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown
        val bgTrend = trendColor.copy(alpha = 0.1f)

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    // [UPDATE BAGIAN BADGE INI]
                    Surface(color = bgTrend, shape = RoundedCornerShape(50)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = trendIcon, // Icon Dinamis
                                contentDescription = null,
                                tint = trendColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = trendLabel, // Teks Dinamis (+12% atau -5%)
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = trendColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    if (data.isEmpty()) {
                        Text("Belum ada data", modifier = Modifier.align(Alignment.Center), color = TextSecondary)
                    } else {
                        BarChartWithLabels(data = data, labels = labels)
                    }
                }
            }
        }
    }

    @Composable
    fun BarChartWithLabels(data: List<Int>, labels: List<String>) {
        val rawMax = data.maxOrNull() ?: 0
        val yMax = if (rawMax == 0) 5 else ((rawMax / 5) + 1) * 5

        var animationPlayed by remember { mutableStateOf(false) }
        LaunchedEffect(data) {
            animationPlayed = false
            animationPlayed = true
        }
        val animateHeight by animateFloatAsState(
            targetValue = if (animationPlayed) 1f else 0f,
            animationSpec = tween(durationMillis = 800)
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val leftAxisWidth = 60f
            val bottomAxisHeight = 50f
            val chartWidth = size.width - leftAxisWidth
            val chartHeight = size.height - bottomAxisHeight

            // Grid Lines & Axis Y
            val gridCount = 4
            val textPaintY = Paint().apply {
                color = TextSecondary.toArgb()
                textSize = 24f
                textAlign = Paint.Align.RIGHT
            }
            val gridPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            for (i in 0..gridCount) {
                val fraction = i.toFloat() / gridCount
                val yPos = chartHeight - (fraction * chartHeight)
                val value = (fraction * yMax).toInt()

                drawContext.canvas.nativeCanvas.drawText(
                    value.toString(),
                    leftAxisWidth - 15f,
                    yPos + 8f,
                    textPaintY
                )

                drawLine(
                    color = GridLineColor.copy(alpha = 0.5f), // Pakai Template Color
                    start = Offset(leftAxisWidth, yPos),
                    end = Offset(size.width, yPos),
                    pathEffect = if (i == 0) null else gridPathEffect
                )
            }

            val totalBars = data.size
            val slotWidth = chartWidth / totalBars
            val barWidth = (slotWidth * 0.6f).coerceAtMost(100f)
            val slotPadding = (slotWidth - barWidth) / 2

            data.forEachIndexed { index, value ->
                val barHeight = (value.toFloat() / yMax) * chartHeight * animateHeight
                val xStart = leftAxisWidth + (index * slotWidth) + slotPadding
                val yTop = chartHeight - barHeight

                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(ChartGradientStart, ChartGradientEnd)),
                    topLeft = Offset(x = xStart, y = yTop),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                val labelTextPaint = Paint().apply {
                    color = TextSecondary.toArgb()
                    textSize = 26f
                    textAlign = Paint.Align.CENTER
                }
                val labelText = labels.getOrElse(index) { "" }
                drawContext.canvas.nativeCanvas.drawText(
                    labelText,
                    xStart + (barWidth / 2),
                    size.height - 10f,
                    labelTextPaint
                )
            }
        }
    }

// ==========================================
// OTHER COMPONENTS
// ==========================================

@Composable
fun StatusDonutChartCard(waiting: Int, finished: Int, total: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Status Pasien", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)

            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                Canvas(modifier = Modifier.size(90.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    val totalSafe = if (total == 0) 1 else total
                    val waitingAngle = (waiting.toFloat() / totalSafe) * 360f
                    val finishedAngle = (finished.toFloat() / totalSafe) * 360f

                    drawCircle(color = DonutEmpty, style = Stroke(width = strokeWidth))

                    if (total > 0) {
                        drawArc(color = StatusSuccess, startAngle = -90f, sweepAngle = finishedAngle, useCenter = false, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                        drawArc(color = StatusError, startAngle = -90f + finishedAngle, sweepAngle = waitingAngle, useCenter = false, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$total", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    Text("Total", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendItem(StatusSuccess, "Selesai")
                LegendItem(StatusError, "Antri")
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondary)
    }
}

@Composable
fun DemographicsCard(maleCount: Int = 0, femaleCount: Int = 0) {
    // Total Real
    val total = maleCount + femaleCount

    // Hitung Persentase (Safe Division)
    val malePct = if (total > 0) maleCount.toFloat() / total else 0f
    val femalePct = if (total > 0) femaleCount.toFloat() / total else 0f

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Demografi", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                // Tampilkan subtitle sesuai kondisi data
                val subtitle = if (total == 0) "Belum ada data" else "Berdasarkan Gender"
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary.copy(alpha = 0.5f), fontSize = 10.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Tampilkan Count Asli di dalam kurung
                DemographicItem("Wanita", femalePct, GenderFemale, femaleCount)
                DemographicItem("Pria", malePct, GenderMale, maleCount)
            }

            val descText = when {
                total == 0 -> "Menunggu data pasien masuk."
                femaleCount > maleCount -> "Mayoritas pasien adalah wanita."
                maleCount > femaleCount -> "Mayoritas pasien adalah pria."
                else -> "Jumlah pasien pria & wanita seimbang."
            }

            Text(
                text = descText,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                color = TextSecondary
            )
        }
    }
}

@Composable
fun DemographicItem(label: String, percentage: Float, color: Color, count: Int) { // Tambah param count
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp), color = TextPrimary)
            // Format: "45% (12)"
            Text("${(percentage * 100).toInt()}% ($count)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)).background(AdminBackground)) {
            Box(modifier = Modifier.fillMaxWidth(percentage).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(color))
        }
    }
}

@Composable
fun DashboardTopBar(date: String) {
    Row(modifier = Modifier.fillMaxWidth().background(SurfaceWhite).padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column { Text("Dashboard Admin", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp), color = TextPrimary); Text(date, style = MaterialTheme.typography.bodyMedium, color = TextSecondary) }
    }
}

@Composable
fun ProfessionalStatusCard(practiceStatus: PracticeStatus?, schedule: DailyScheduleData?, onManageSchedule: () -> Unit) {
    val isOpen = practiceStatus?.isPracticeOpen ?: false
    val statusColor = if (isOpen) StatusSuccess else StatusError
    val statusText = if (isOpen) "BUKA" else "TUTUP"
    val scheduleText = if (schedule != null && schedule.isOpen) "${schedule.startTime} - ${schedule.endTime}" else "Libur"

    Card(colors = CardDefaults.cardColors(containerColor = SurfaceWhite), elevation = CardDefaults.cardElevation(1.dp), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(BrandPrimary.copy(0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Store, null, tint = BrandPrimary) }
                    Spacer(Modifier.width(12.dp)); Column { Text("Operasional", style = MaterialTheme.typography.labelMedium, color = TextSecondary); Text(scheduleText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary) }
                }
                Surface(color = statusColor.copy(0.1f), shape = RoundedCornerShape(50), border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(0.2f))) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape)); Spacer(Modifier.width(6.dp)); Text(statusText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = statusColor)
                    }
                }
            }
            Spacer(Modifier.height(16.dp)); Divider(color = AdminBackground); Spacer(Modifier.height(12.dp))
            Button(onClick = onManageSchedule, modifier = Modifier.fillMaxWidth().height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = AdminBackground, contentColor = TextPrimary), shape = RoundedCornerShape(10.dp), elevation = ButtonDefaults.buttonElevation(0.dp)) {
                Icon(Icons.Outlined.EditCalendar, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Kelola Jadwal Dokter", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun DashboardStatsGrid(total: Int, waiting: Int, finished: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DashboardStatItem(Modifier.weight(1f), "Total", total.toString(), Icons.Outlined.People, BrandPrimary)
        DashboardStatItem(Modifier.weight(1f), "Menunggu", waiting.toString(), Icons.Outlined.Timer, StatusWarning)
        DashboardStatItem(Modifier.weight(1f), "Selesai", finished.toString(), Icons.Outlined.CheckCircle, StatusSuccess)
    }
}

@Composable
fun DashboardStatItem(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = SurfaceWhite), elevation = CardDefaults.cardElevation(1.dp), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(modifier = Modifier.size(32.dp).background(color.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.height(12.dp)); Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary); Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun QueueListItem(
    item: QueueItem,
    limitMinutes: Int
) {
    val statusColor = when (item.status) { QueueStatus.DILAYANI -> BrandPrimary; QueueStatus.DIPANGGIL -> StatusWarning; else -> TextSecondary }
    val bgCard = if(item.status == QueueStatus.DILAYANI) BrandPrimary.copy(0.05f) else SurfaceWhite
    val border = if(item.status == QueueStatus.DILAYANI) androidx.compose.foundation.BorderStroke(1.dp, BrandPrimary.copy(0.3f)) else null

    Card(
        colors = CardDefaults.cardColors(containerColor = bgCard),
        border = border,
        elevation = CardDefaults.cardElevation(if(border==null) 1.dp else 0.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Lingkaran Nomor
            Box(modifier = Modifier.size(48.dp).background(AdminBackground, CircleShape), contentAlignment = Alignment.Center) {
                Text(text = item.queueNumber.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            }

            Spacer(Modifier.width(16.dp))

            // Kolom Nama & Keluhan/Timer
            Column(modifier = Modifier.weight(1f)) {
                Text(item.userName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)

                // [LOGIKA TAMPILAN]
                // Jika status DIPANGGIL -> Tampilkan TIMER
                // Jika status lain -> Tampilkan KELUHAN
                if (item.status == QueueStatus.DIPANGGIL) {
                    Spacer(Modifier.height(4.dp))
                    CallTimer(
                        calledAt = item.calledAt,
                        limitMinutes = limitMinutes // Limit dinamis dari parameter
                    )
                } else {
                    Text("Keluhan: ${item.keluhan}", style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(Modifier.width(8.dp))

            // Status Chip
            Surface(color = statusColor.copy(0.1f), shape = RoundedCornerShape(4.dp)) {
                Text(item.status.name, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = statusColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceWhite), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(32.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Inbox, null, tint = Color.LightGray, modifier = Modifier.size(48.dp)); Spacer(Modifier.height(8.dp)); Text("Tidak ada antrian aktif saat ini", color = TextSecondary)
        }
    }
}

// ==========================================
// COMPONENT: TIMER (Copy Paste di bawah file)
// ==========================================
@Composable
fun CallTimer(
    calledAt: Date?,
    limitMinutes: Int,
    modifier: Modifier = Modifier
) {
    var timeRemaining by remember { mutableLongStateOf(0L) }
    var progress by remember { mutableFloatStateOf(1f) }

    // Warna timer: Hijau jika aman, Merah jika < 20%
    val timerColor = if (progress < 0.2f) StatusError else StatusSuccess

    // [LOGIKA SINKRONISASI]
    // Saat 'limitMinutes' berubah di Firebase -> UI State update -> LaunchedEffect restart -> Timer menyesuaikan
    LaunchedEffect(calledAt, limitMinutes) {
        if (calledAt == null) return@LaunchedEffect

        val limitMillis = limitMinutes * 60 * 1000L
        val deadline = calledAt.time + limitMillis

        while (true) {
            val now = System.currentTimeMillis()
            val diff = deadline - now

            if (diff <= 0) {
                timeRemaining = 0
                progress = 0f
                break
            } else {
                timeRemaining = diff
                progress = diff.toFloat() / limitMillis.toFloat()
            }
            kotlinx.coroutines.delay(1000L)
        }
    }

    val minutes = (timeRemaining / 1000) / 60
    val seconds = (timeRemaining / 1000) % 60
    val timerText = String.format("%02d:%02d", minutes, seconds)

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Timer, null, tint = timerColor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = timerText,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = timerColor
        )
    }
}