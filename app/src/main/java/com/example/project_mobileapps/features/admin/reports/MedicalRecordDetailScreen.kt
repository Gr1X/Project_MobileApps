package com.example.project_mobileapps.features.admin.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.repo.FirestoreQueueRepository
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordDetailScreen(
    queueId: String,
    onNavigateBack: () -> Unit
) {
    var queueItem by remember { mutableStateOf<QueueItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(queueId) {
        queueItem = FirestoreQueueRepository.getQueueById(queueId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Rekam Medis Elektronik",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1E293B)
                )
            )
        },
        floatingActionButton = {
            // Tombol Print/Export Profesional
            FloatingActionButton(
                onClick = { /* TODO: Implement Print/PDF */ },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Print, "Print")
            }
        },
        containerColor = Color(0xFFF1F5F9) // Background abu-abu muda modern
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (queueItem == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Data tidak ditemukan.", color = Color.Gray)
            }
        } else {
            val item = queueItem!!
            val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
            val dateStr = try { dateFormat.format(item.finishedAt ?: item.createdAt) } catch (e: Exception) { "-" }

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. LOGO & KOP KLINIK (Visual Branding)
                ClinicHeaderSection()

                // 2. KARTU PASIEN UTAMA
                PatientMainInfoCard(item, dateStr)

                // 3. GRAFIK KESEHATAN (BMI Calculator & Visualizer)
                // Ini menjawab request "Ada Grafik"
                if (item.weightKg > 0 && item.heightCm > 0) {
                    BMICalculatorCard(weight = item.weightKg, height = item.heightCm)
                }

                // 4. TANDA VITAL LENGKAP
                VitalSignsGrid(item)

                // 5. DETAIL MEDIS (SOAP)
                MedicalContentCard(
                    title = "Pemeriksaan & Diagnosa (S/O/A)",
                    icon = Icons.Outlined.Assignment,
                    accentColor = Color(0xFF2196F3)
                ) {
                    DetailRowItem("Keluhan Utama", item.keluhan)
                    Divider(Modifier.padding(vertical = 8.dp).fillMaxWidth(), color = Color(0xFFEEF2F6))
                    DetailRowItem("Pemeriksaan Fisik", item.physicalExam.ifEmpty { "-" })
                    Divider(Modifier.padding(vertical = 8.dp).fillMaxWidth(), color = Color(0xFFEEF2F6))
                    DetailRowItem("Diagnosa Dokter", item.diagnosis.ifEmpty { "Belum ada diagnosa" }, isHighlight = true)
                }

                // 6. RESEP & PLANNING
                MedicalContentCard(
                    title = "Terapi & Obat (Plan)",
                    icon = Icons.Outlined.Medication,
                    accentColor = Color(0xFF4CAF50)
                ) {
                    DetailRowItem("Resep Obat", item.prescription.ifEmpty { "-" })

                    if (item.doctorNotes.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFFFFF8E1),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Lightbulb, null, tint = Color(0xFFFFA000), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Catatan Tambahan", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFA000))
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(item.doctorNotes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // Ruang untuk FAB
                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

// ==========================================
// CUSTOM COMPONENTS (MODERN UI)
// ==========================================

@Composable
fun ClinicHeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Logo Placeholder
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(Icons.Default.LocalHospital, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("KLINIK SEHAT SELALU", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Jl. Kesehatan No. 123, Jakarta", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun PatientMainInfoCard(item: QueueItem, dateStr: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Initials
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF42A5F5), Color(0xFF1976D2)))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.userName.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("PASIEN", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 1.sp)
                Text(item.userName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Kunjungan: $dateStr", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun BMICalculatorCard(weight: Double, height: Double) {
    val heightM = height / 100
    val bmi = weight / heightM.pow(2)
    val (status, color) = when {
        bmi < 18.5 -> "Underweight" to Color(0xFFFFB74D) // Orange
        bmi < 24.9 -> "Normal" to Color(0xFF66BB6A)      // Green
        bmi < 29.9 -> "Overweight" to Color(0xFFFF7043)  // Red Orange
        else -> "Obesity" to Color(0xFFEF5350)           // Red
    }

    val progress = (bmi / 40.0).coerceIn(0.0, 1.0).toFloat()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Analisis BMI", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(status, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))

            // GRAFIK BATANG (Bar Chart Sederhana)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(String.format("%.1f", bmi), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF263238))
                Spacer(Modifier.width(4.dp))
                Text("kg/m²", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Visual Progress Bar
            Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                drawRoundRect(color = Color(0xFFE0E0E0), size = size, cornerRadius = CornerRadius(10f))
                drawRoundRect(color = color, size = size.copy(width = size.width * progress), cornerRadius = CornerRadius(10f))
            }
        }
    }
}

@Composable
fun VitalSignsGrid(item: QueueItem) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VitalCard(
            label = "Tekanan Darah",
            value = item.bloodPressure.ifEmpty { "-" },
            unit = "mmHg",
            icon = Icons.Outlined.Favorite,
            bg = Color(0xFFFCE4EC),
            tint = Color(0xFFEC407A),
            modifier = Modifier.weight(1f)
        )
        VitalCard(
            label = "Suhu Tubuh",
            value = "${item.temperature}",
            unit = "°C",
            icon = Icons.Outlined.Thermostat,
            bg = Color(0xFFE3F2FD),
            tint = Color(0xFF42A5F5),
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VitalCard(
            label = "Berat Badan",
            value = "${item.weightKg}",
            unit = "kg",
            icon = Icons.Outlined.MonitorWeight,
            bg = Color(0xFFF3E5F5),
            tint = Color(0xFFAB47BC),
            modifier = Modifier.weight(1f)
        )
        VitalCard(
            label = "Tinggi Badan",
            value = "${item.heightCm}",
            unit = "cm",
            icon = Icons.Outlined.Height,
            bg = Color(0xFFE8F5E9),
            tint = Color(0xFF66BB6A),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun VitalCard(label: String, value: String, unit: String, icon: ImageVector, bg: Color, tint: Color, modifier: Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(12.dp)) {
            Box(Modifier.size(32.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(2.dp))
                Text(unit, style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

@Composable
fun MedicalContentCard(title: String, icon: ImageVector, accentColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = accentColor)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun DetailRowItem(label: String, value: String, isHighlight: Boolean = false) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else Color(0xFF333333),
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
        )
    }
}