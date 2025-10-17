// Salin dan ganti seluruh isi file: ui/components/PatientStatsChart.kt

package com.example.project_mobileapps.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_mobileapps.data.local.DailyReport

@Composable
fun PatientStatsChart(
    reportData: List<DailyReport>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Laporan Data Pasien", // Judul yang lebih sesuai
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Cek jika data kosong
            if (reportData.isEmpty()) {
                Text(
                    text = "Tidak ada data untuk ditampilkan.",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp), // Tinggi area grafik
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    val maxPatients = reportData.maxOfOrNull { it.totalPatients }?.toFloat() ?: 1f

                    reportData.forEach { dailyData ->
                        BarItem(
                            value = dailyData.totalPatients,
                            maxValue = maxPatients,
                            label = dailyData.day
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.BarItem(value: Int, maxValue: Float, label: String) {
    val isYearlyView = label.length > 3 // Asumsi label tahun (misal: "2024") lebih dari 3 karakter

    Column(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Teks Angka (Value)
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // 2. Bar Grafik (Container)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Biarkan Box ini mengisi sisa ruang
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Bar yang sebenarnya
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction = if (maxValue > 0) value / maxValue else 0f)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        // 3. Teks Label (Hari/Bulan/Tahun)
        Text(
            text = label,
            fontSize = if (isYearlyView) 10.sp else 12.sp, // Font lebih kecil untuk tahun
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}