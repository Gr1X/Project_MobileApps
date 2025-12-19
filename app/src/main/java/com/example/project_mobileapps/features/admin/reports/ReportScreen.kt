// File: features/admin/reports/ReportScreen.kt
package com.example.project_mobileapps.features.admin.reports

import android.graphics.Paint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate // [PERBAIKAN] Import ini ditambahkan
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.project_mobileapps.R
import com.example.project_mobileapps.data.model.DailyReport
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.ui.themes.*
import java.util.Locale

// --- DEFINISI WARNA CHART ---
val ChartGradientStart = Color(0xFF4F46E5) // Indigo
val ChartGradientEnd = Color(0xFF818CF8)   // Light Indigo
val GridLineColor = Color(0xFFE2E8F0)      // Slate 200

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onPatientClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {

        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Text("Laporan",  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp), color = TextPrimary)
                    }
                }

                // 1. FILTER SECTION
                item {
                    FilterControlCard(
                        uiState = uiState,
                        onPeriodSelect = viewModel::setPeriod,
                        onYearSelect = viewModel::setYear,
                        onMonthSelect = viewModel::setMonth
                    )
                }

                // 2. KPI GRID
                item {
                    KpiStatsGrid(uiState)
                }

                // 3. GRAFIK (STYLE BARU ADMIN DASHBOARD)
                item {
                    ChartSectionCard(uiState.chartData, uiState.selectedPeriod)
                }

                // 4. DAFTAR PASIEN
                item {
                    Text(
                        "Riwayat Pasien (${uiState.uniquePatients.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                if (uiState.uniquePatients.isEmpty()) {
                    item { EmptyStateReport() }
                } else {
                    items(uiState.uniquePatients) { patient ->
                        PatientReportItem(patient = patient, onClick = { onPatientClick(patient.uid) })
                    }
                }

                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

// ==========================================
// 1. FILTER COMPONENTS (Modern Style)
// ==========================================

@Composable
fun FilterControlCard(
    uiState: ReportUiState,
    onPeriodSelect: (ReportPeriod) -> Unit,
    onYearSelect: (Int) -> Unit,
    onMonthSelect: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(Modifier.padding(20.dp)) {
            // Header Filter
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFF1F5F9), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Filter Laporan",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    SimpleDropdown(
                        label = uiState.selectedPeriod.displayName,
                        items = ReportPeriod.values().toList(),
                        itemLabel = { it.displayName },
                        onItemSelected = onPeriodSelect,
                        leadingIcon = Icons.Outlined.DateRange
                    )
                }

                if (uiState.selectedPeriod != ReportPeriod.HARIAN) {
                    Box(Modifier.weight(0.8f)) {
                        SimpleDropdown(
                            label = uiState.selectedYear.toString(),
                            items = uiState.availableYears,
                            itemLabel = { it.toString() },
                            onItemSelected = onYearSelect,
                            leadingIcon = Icons.Outlined.CalendarToday
                        )
                    }
                }
            }

            if (uiState.selectedPeriod == ReportPeriod.MINGGUAN) {
                Spacer(Modifier.height(12.dp))
                SimpleDropdown(
                    label = uiState.availableMonths[uiState.selectedMonth],
                    items = uiState.availableMonths.indices.toList(),
                    itemLabel = { uiState.availableMonths[it] },
                    onItemSelected = onMonthSelect,
                    leadingIcon = Icons.Outlined.CalendarMonth
                )
            }
        }
    }
}

@Composable
fun <T> SimpleDropdown(
    label: String,
    items: List<T>,
    itemLabel: (T) -> String,
    onItemSelected: (T) -> Unit,
    leadingIcon: ImageVector? = null
) {
    var expanded by remember { mutableStateOf(false) }
    // Animasi putar panah
    val rotateAnim by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "Rotate")

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF334155)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, if(expanded) BrandPrimary else Color(0xFFCBD5E1))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (leadingIcon != null) {
                        Icon(imageVector = leadingIcon, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                // [PERBAIKAN] Menggunakan Modifier.rotate() yang benar
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.rotate(rotateAnim)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White).fillMaxWidth(0.5f)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = itemLabel(item), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1E293B)) },
                    onClick = { onItemSelected(item); expanded = false },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ==========================================
// 2. CHART SECTION (Style Admin Dashboard)
// ==========================================

@Composable
fun ChartSectionCard(data: List<DailyReport>, period: ReportPeriod) {
    // 1. Kalkulasi Trend Sederhana (Untuk UI Badge)
    val counts = data.map { it.count }
    val (trendLabel, isPositive) = calculateSimpleTrend(counts)

    // Warna Trend
    val trendColor = if (isPositive) StateSuccess else StateError
    val trendIcon = if (isPositive) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown
    val bgTrend = trendColor.copy(alpha = 0.1f)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            // Header Chart (Title & Badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Statistik Kunjungan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        "Periode ${period.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Badge Trend
                Surface(
                    color = bgTrend,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = trendIcon, contentDescription = null, tint = trendColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = trendLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = trendColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Area Grafik (Canvas)
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                if (data.isEmpty() || data.all { it.count == 0 }) {
                    Text("Belum ada data grafik", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                } else {
                    // Panggil Grafik Canvas Style
                    PatientBarChartCanvas(data)
                }
            }
        }
    }
}

// Logic sederhana hitung trend untuk display
fun calculateSimpleTrend(data: List<Int>): Pair<String, Boolean> {
    if (data.size < 2) return "0%" to true
    val first = data.first().toFloat()
    val last = data.last().toFloat()
    if (first == 0f) return if (last > 0) "+100%" to true else "0%" to true

    val diff = last - first
    val pct = (diff / first) * 100
    val sign = if (pct >= 0) "+" else ""
    return String.format(Locale.US, "%s%.1f%%", sign, pct) to (pct >= 0)
}

// Chart Drawing Logic (Canvas - Sama persis dengan Admin Dashboard)
@Composable
fun PatientBarChartCanvas(data: List<DailyReport>) {
    val counts = data.map { it.count }
    val labels = data.map { it.label }

    val rawMax = counts.maxOrNull() ?: 0
    val yMax = if (rawMax == 0) 5 else ((rawMax / 5) + 1) * 5

    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(data) {
        animationPlayed = false
        animationPlayed = true
    }
    val animateHeight by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "BarAnim"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val leftAxisWidth = 60f
        val bottomAxisHeight = 50f
        val chartWidth = size.width - leftAxisWidth
        val chartHeight = size.height - bottomAxisHeight

        // Grid Lines & Axis Y
        val gridCount = 4
        val textPaintY = Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8") // Slate 400
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
                color = GridLineColor.copy(alpha = 0.5f),
                start = Offset(leftAxisWidth, yPos),
                end = Offset(size.width, yPos),
                pathEffect = if (i == 0) null else gridPathEffect
            )
        }

        val totalBars = counts.size
        val slotWidth = chartWidth / totalBars
        val barWidth = (slotWidth * 0.6f).coerceAtMost(100f)
        val slotPadding = (slotWidth - barWidth) / 2

        counts.forEachIndexed { index, value ->
            val barHeight = (value.toFloat() / yMax) * chartHeight * animateHeight
            val xStart = leftAxisWidth + (index * slotWidth) + slotPadding
            val yTop = chartHeight - barHeight

            // Gambar Bar dengan Gradient
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(ChartGradientStart, ChartGradientEnd)),
                topLeft = Offset(x = xStart, y = yTop),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )

            val labelTextPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#94A3B8") // Slate 400
                textSize = 26f
                textAlign = Paint.Align.CENTER
            }
            // Label Sumbu X (Ambil 3 huruf pertama)
            val labelText = labels.getOrElse(index) { "" }.take(3)

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
// 3. KPI STATS GRID
// ==========================================

@Composable
fun KpiStatsGrid(state: ReportUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiStatCard("Pasien Selesai", "${state.totalPatientsServed}", Icons.Outlined.CheckCircle, Color(0xFF10B981), Modifier.weight(1f))
            KpiStatCard("Rata-rata/Hari", String.format("%.1f", state.avgPatientsPerDay), Icons.Outlined.DateRange, Color(0xFF3B82F6), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiStatCard("Waktu Layan", "${state.avgServiceTimeMinutes} mnt", Icons.Outlined.Timer, Color(0xFF8B5CF6), Modifier.weight(1f))
            KpiStatCard("Rate Batal", "${state.cancellationRate.toInt()}%", Icons.Outlined.Cancel, Color(0xFFEF4444), Modifier.weight(1f))
        }
    }
}

@Composable
fun KpiStatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
        }
    }
}

// ==========================================
// 4. PATIENT LIST ITEMS
// ==========================================

@Composable
fun PatientReportItem(patient: User, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto Profil / Inisial
            if (!patient.profilePictureUrl.isNullOrBlank()) {
                AsyncImage(
                    model = patient.profilePictureUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_launcher_foreground)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(BrandPrimary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = patient.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = patient.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = if(patient.phoneNumber.isNotEmpty()) patient.phoneNumber else patient.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFCBD5E1))
        }
    }
}

@Composable
fun EmptyStateReport() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Assignment, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(8.dp))
        Text("Tidak ada data laporan.", color = Color.Gray)
    }
}