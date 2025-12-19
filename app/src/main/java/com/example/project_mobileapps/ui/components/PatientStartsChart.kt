// File: ui/components/PatientStatsChart.kt
package com.example.project_mobileapps.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.data.model.DailyReport

/**
 * Composable reusable untuk menampilkan grafik batang (bar chart)
 * data laporan pasien.
 * * Styling: Menggunakan Canvas & Grid (ala Kode 1)
 * Warna Bar: Menggunakan Primary Color (ala Kode 2)
 */
@Composable
fun PatientStatsChart(
    reportData: List<DailyReport>,
    modifier: Modifier = Modifier
) {
    // Persiapan Data untuk Canvas
    val dataCounts = reportData.map { it.count }
    val dataLabels = reportData.map { it.label }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Laporan Data Pasien",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                if (reportData.isEmpty()) {
                    Text(
                        "Tidak ada data untuk ditampilkan.",
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Panggil fungsi Canvas Chart yang sudah dimodifikasi
                    BarChartCanvasStyle(
                        data = dataCounts,
                        labels = dataLabels,
                        barColor = MaterialTheme.colorScheme.primary, // Warna Bar dari Kode 2
                        gridColor = MaterialTheme.colorScheme.outlineVariant,
                        textColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BarChartCanvasStyle(
    data: List<Int>,
    labels: List<String>,
    barColor: Color,
    gridColor: Color,
    textColor: Color
) {
    val rawMax = data.maxOrNull() ?: 0
    // Menghitung batas atas sumbu Y (kelipatan 5)
    val yMax = if (rawMax == 0) 5 else ((rawMax / 5) + 1) * 5

    // Animasi naik turun bar
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(data) {
        animationPlayed = false
        animationPlayed = true
    }
    val animateHeight by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "BarHeightAnimation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Konfigurasi Dimensi
        val leftAxisWidth = 60f
        val bottomAxisHeight = 50f
        val chartWidth = size.width - leftAxisWidth
        val chartHeight = size.height - bottomAxisHeight

        // Konfigurasi Paint untuk Teks (Native Android Canvas)
        val textPaintY = Paint().apply {
            color = textColor.toArgb()
            textSize = 30f // Ukuran font sumbu Y
            textAlign = Paint.Align.RIGHT
        }
        val labelTextPaint = Paint().apply {
            color = textColor.toArgb()
            textSize = 32f // Ukuran font label sumbu X
            textAlign = Paint.Align.CENTER
        }

        // Efek garis putus-putus untuk Grid
        val gridPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

        // 1. MENGGAMBAR GRID LINES & LABEL SUMBU Y
        val gridCount = 4
        for (i in 0..gridCount) {
            val fraction = i.toFloat() / gridCount
            val yPos = chartHeight - (fraction * chartHeight)
            val value = (fraction * yMax).toInt()

            // Gambar Angka di Sumbu Y
            drawContext.canvas.nativeCanvas.drawText(
                value.toString(),
                leftAxisWidth - 15f,
                yPos + 10f, // Sedikit offset agar center vertical terhadap garis
                textPaintY
            )

            // Gambar Garis Grid
            drawLine(
                color = gridColor.copy(alpha = 0.5f),
                start = Offset(leftAxisWidth, yPos),
                end = Offset(size.width, yPos),
                pathEffect = if (i == 0) null else gridPathEffect // Garis bawah solid, sisanya putus-putus
            )
        }

        // 2. MENGGAMBAR BAR & LABEL SUMBU X
        val totalBars = data.size
        val slotWidth = chartWidth / totalBars
        // Lebar bar max 100f agar tidak terlalu gemuk jika data sedikit
        val barWidth = (slotWidth * 0.5f).coerceAtMost(100f)
        val slotPadding = (slotWidth - barWidth) / 2

        data.forEachIndexed { index, value ->
            // Hitung tinggi bar berdasarkan value dan animasi
            val barHeight = (value.toFloat() / yMax) * chartHeight * animateHeight

            val xStart = leftAxisWidth + (index * slotWidth) + slotPadding
            val yTop = chartHeight - barHeight

            // Gambar Bar (Persegi Panjang dengan sudut melengkung di atas)
            drawRoundRect(
                color = barColor, // Menggunakan Solid Color (Warna Kode 2)
                topLeft = Offset(x = xStart, y = yTop),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Gambar Label di Sumbu X (Sen, Sel, dst)
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