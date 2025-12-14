// File: features/patient/history/MedicalRecordResultScreen.kt
package com.example.project_mobileapps.features.patient.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_mobileapps.utils.PdfGenerator
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordResultScreen(
    viewModel: MedicalRecordResultViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Medis Digital", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                // [BARU] Tombol Download di Pojok Kanan Atas
                actions = {
                    val currentState = uiState
                    if (currentState is MedicalRecordUiState.Success) {
                        IconButton(onClick = {
                            val item = currentState.data
                            // Panggil Fungsi Generator PDF menggunakan context yang sudah dideklarasikan di atas
                            PdfGenerator.generateAndOpenMedicalReport(
                                context = context,
                                patientName = item.userName,
                                data = item
                            )
                        }) {
                            Icon(Icons.Default.Download, "Unduh PDF", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FA) // Background abu-abu sangat muda (Professional Look)
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is MedicalRecordUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is MedicalRecordUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is MedicalRecordUiState.Success -> {
                    val item = state.data
                    val dateFormat = SimpleDateFormat("dd MMMM yyyy • HH:mm", Locale.getDefault())
                    val dateStr = try { dateFormat.format(item.finishedAt ?: item.createdAt) } catch (e: Exception) { "-" }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. HEADER RINGKASAN
                        MedicalHeaderCard(
                            doctorName = "Dr. Budi Santoso", // Atau ambil dari item.doctorName jika ada
                            date = dateStr,
                            visitId = item.queueNumber.toString()
                        )

                        // 2. GRAFIK BMI (Bintang Utama!)
                        if (item.weightKg > 0 && item.heightCm > 0) {
                            BmiVisualizationCard(weight = item.weightKg, height = item.heightCm)
                        }

                        // 3. GRID TANDA VITAL
                        VitalSignsGrid(
                            bp = item.bloodPressure,
                            temp = item.temperature,
                            weight = item.weightKg,
                            height = item.heightCm
                        )

                        // 4. DIAGNOSA & KELUHAN
                        SectionCard(title = "Diagnosa Medis", icon = Icons.Outlined.HealthAndSafety, color = Color(0xFFE91E63)) {
                            InfoRow("Keluhan Utama", item.keluhan)
                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.3f))
                            InfoRow("Diagnosa Dokter", item.diagnosis.ifEmpty { "Belum ada diagnosa" }, isBold = true)
                            if (item.physicalExam.isNotEmpty()) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.3f))
                                InfoRow("Pemeriksaan Fisik", item.physicalExam)
                            }
                        }

                        // 5. RESEP & TINDAKAN
                        SectionCard(title = "Terapi & Resep", icon = Icons.Outlined.Medication, color = Color(0xFF4CAF50)) {
                            InfoRow("Tindakan Medis", item.treatment.ifEmpty { "-" })
                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.3f))
                            InfoRow("Resep Obat", item.prescription.ifEmpty { "-" })

                            if (item.doctorNotes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = Color(0xFFFFF3E0),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp)) {
                                        Icon(Icons.Outlined.Info, null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(item.doctorNotes, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE65100))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

// --- KOMPONEN UI VISUAL ---

@Composable
fun MedicalHeaderCard(doctorName: String, date: String, visitId: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Description, null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Resume Medis", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                Text(doctorName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text("$date • Antrian #$visitId", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun BmiVisualizationCard(weight: Double, height: Double) {
    val heightM = height / 100
    val bmi = weight / heightM.pow(2)
    val (status, color) = when {
        bmi < 18.5 -> "Underweight" to Color(0xFFFFB74D) // Orange
        bmi < 24.9 -> "Normal" to Color(0xFF66BB6A)      // Green
        bmi < 29.9 -> "Overweight" to Color(0xFFFF7043)  // Red Orange
        else -> "Obesity" to Color(0xFFEF5350)           // Red
    }
    // Posisi indikator (0.0 - 1.0)
    val progress = ((bmi - 15) / (35 - 15)).coerceIn(0.0, 1.0).toFloat()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Analisis BMI", fontWeight = FontWeight.Bold)
                Text(status, color = color, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Angka Besar
            Row(verticalAlignment = Alignment.Bottom) {
                Text(String.format("%.1f", bmi), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("kg/m²", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Visual Bar
            Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color.LightGray.copy(alpha = 0.3f))) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF64B5F6), color)))
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("15.0", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("35.0", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun VitalSignsGrid(bp: String, temp: Double, weight: Double, height: Double) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VitalCard(
            label = "Tekanan Darah",
            value = bp.ifEmpty { "-" },
            unit = "mmHg",
            icon = Icons.Outlined.FavoriteBorder,
            color = Color(0xFFE91E63),
            modifier = Modifier.weight(1f)
        )
        VitalCard(
            label = "Suhu Tubuh",
            value = "$temp",
            unit = "°C",
            icon = Icons.Outlined.Thermostat,
            color = Color(0xFFFF9800),
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VitalCard(
            label = "Berat Badan",
            value = "$weight",
            unit = "kg",
            icon = Icons.Default.MonitorWeight,
            color = Color(0xFF2196F3),
            modifier = Modifier.weight(1f)
        )
        VitalCard(
            label = "Tinggi Badan",
            value = "$height",
            unit = "cm",
            icon = Icons.Outlined.Height,
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun VitalCard(label: String, value: String, unit: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(2.dp))
                Text(unit, style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

@Composable
fun SectionCard(title: String, icon: ImageVector, color: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, isBold: Boolean = false) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) MaterialTheme.colorScheme.primary else Color.Black
        )
    }
}