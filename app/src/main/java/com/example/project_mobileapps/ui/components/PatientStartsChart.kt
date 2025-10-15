package com.example.project_mobileapps.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.data.local.DailyReport

@Composable
fun PatientStatsChart(
    reportData: List<DailyReport>,
    modifier: Modifier = Modifier
) {
    val maxPatients = reportData.maxOfOrNull { it.totalPatients } ?: 1

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Laporan Pasien Mingguan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp), // Tinggi area grafik
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom // Batang grafik mulai dari bawah
            ) {
                reportData.forEach { dailyData ->
                    Bar(
                        value = dailyData.totalPatients,
                        maxValue = maxPatients,
                        label = dailyData.day
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.Bar(value: Int, maxValue: Int, label: String) {
    val barHeight = (value.toFloat() / maxValue.toFloat()) * 150 // Hitung tinggi bar relatif

    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Angka di atas bar
        Text(text = value.toString(), style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(4.dp))
        // Bar
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(barHeight.dp)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Label hari di bawah bar
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}