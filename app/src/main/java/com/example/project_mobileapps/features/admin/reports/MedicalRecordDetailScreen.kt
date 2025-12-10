// File: features/admin/reports/MedicalRecordDetailScreen.kt
package com.example.project_mobileapps.features.admin.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.FirestoreQueueRepository
import com.example.project_mobileapps.data.repo.UserRepository
import com.example.project_mobileapps.ui.themes.PrimaryPeriwinkle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordDetailScreen(
    queueId: String,
    onNavigateBack: () -> Unit
) {
    // State untuk Data Antrian & Data Pasien
    var queueItem by remember { mutableStateOf<QueueItem?>(null) }
    var patientUser by remember { mutableStateOf<User?>(null) } // <-- State Baru untuk User
    var isLoading by remember { mutableStateOf(true) }

    // Repository Instance (bisa diinject, tapi untuk simplisitas kita buat disini)
    val userRepository = remember { UserRepository() }

    LaunchedEffect(queueId) {
        // 1. Ambil Data Antrian
        val item = FirestoreQueueRepository.getQueueById(queueId)
        queueItem = item

        // 2. Jika antrian ada, ambil Data Detail User berdasarkan userId
        if (item != null && item.userId.isNotEmpty()) {
            val user = userRepository.getUser(item.userId)
            patientUser = user
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rekam Medis Digital", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1E293B)
                )
            )
        },
        containerColor = Color(0xFFF1F5F9) // Cool Gray Background (Konsisten dengan User App)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPeriwinkle)
            }
        } else if (queueItem == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Data rekam medis tidak ditemukan.", color = Color.Gray)
            }
        } else {
            val item = queueItem!!
            // Format Tanggal Kunjungan
            val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy • HH:mm", Locale("id", "ID"))
            val dateStr = try { dateFormat.format(item.finishedAt ?: item.createdAt) } catch (e: Exception) { "-" }

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. KOP KLINIK (Header Resmi)
                ClinicBrandingHeader()

                // 2. KARTU PASIEN LENGKAP (UI Baru dengan Data Diri)
                PatientFullProfileCard(item, patientUser, dateStr)

                // Separator Halus
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Divider(Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.4f))
                    Text(" DATA MEDIS ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Divider(Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.4f))
                }

                // 3. BMI Analysis
                if (item.weightKg > 0 && item.heightCm > 0) {
                    BmiAnalysisCard(weight = item.weightKg, height = item.heightCm)
                }

                // 4. Vital Signs Grid
                VitalSignsCleanGrid(item)

                // 5. Laporan Dokter (SOAP)
                MedicalReportCard(item)

                // Footer Disclaimer
                Spacer(Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)), // Kuning Sangat Muda
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCD34D).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Info, null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Dokumen ini diterbitkan secara elektronik oleh sistem KlinIQ dan sah tanpa tanda tangan basah.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF92400E),
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

// ==========================================
// CUSTOM COMPONENTS (IMPROVED UI/UX)
// ==========================================

@Composable
fun ClinicBrandingHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = PrimaryPeriwinkle,
            modifier = Modifier.size(42.dp)
        ) {
            Icon(Icons.Default.LocalHospital, null, tint = Color.White, modifier = Modifier.padding(8.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "KlinIQ Medical Center",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                letterSpacing = 0.5.sp
            )
            Text(
                "Jl. Kesehatan No. 123, Jakarta Selatan",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B)
            )
        }
    }
}

/**
 * Kartu Profil Pasien yang Lebih Lengkap (Data Diri User + Data Antrian)
 */
@Composable
fun PatientFullProfileCard(item: QueueItem, user: User?, dateStr: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            // Bagian Atas: Nama & No Antrian
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar Inisial
                Surface(
                    shape = CircleShape,
                    color = PrimaryPeriwinkle.copy(alpha = 0.1f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            item.userName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPeriwinkle
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text("PASIEN", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(
                        item.userName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text("No. Antrian #${item.queueNumber}", style = MaterialTheme.typography.bodyMedium, color = PrimaryPeriwinkle, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(16.dp))
            Divider(color = Color(0xFFF1F5F9))
            Spacer(Modifier.height(16.dp))

            // Bagian Bawah: Grid Data Diri (Umur, Gender, HP, Tgl)
            if (user != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PatientInfoItem("Jenis Kelamin", user.gender.name.lowercase().replaceFirstChar { it.uppercase() }, Icons.Default.Person)
                    PatientInfoItem("Usia", calculateAge(user.dateOfBirth), Icons.Outlined.Cake) // Hitung Umur
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PatientInfoItem("Telepon", user.phoneNumber, Icons.Outlined.Phone)
                    PatientInfoItem("Tanggal", dateStr.split("•")[0].trim(), Icons.Outlined.Event) // Ambil tanggalnya saja
                }
            } else {
                // Fallback jika user null (misal akun dihapus)
                Text("Data profil detail pengguna tidak tersedia.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun PatientInfoItem(label: String, value: String, icon: ImageVector) {
    Row(modifier = Modifier.width(140.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
        }
    }
}

// Helper Hitung Umur Sederhana
fun calculateAge(dob: String): String {
    if (dob == "N/A" || dob.isBlank()) return "-"
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val birthDate = sdf.parse(dob) ?: return "-"
        val today = Calendar.getInstance()
        val birthCal = Calendar.getInstance().apply { time = birthDate }

        var age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        "$age Tahun"
    } catch (e: Exception) {
        dob // Return raw string jika format beda
    }
}

@Composable
fun BmiAnalysisCard(weight: Double, height: Double) {
    val heightM = height / 100
    val bmi = weight / heightM.pow(2)
    val (status, color) = when {
        bmi < 18.5 -> "Berat Kurang" to Color(0xFFFFB74D) // Orange
        bmi < 24.9 -> "Normal" to Color(0xFF66BB6A)      // Green
        bmi < 29.9 -> "Berlebih" to Color(0xFFFF7043)    // Red Orange
        else -> "Obesitas" to Color(0xFFEF5350)          // Red
    }
    val progress = (bmi / 40.0).coerceIn(0.0, 1.0).toFloat()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Analisis BMI", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(status, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))

            // Angka Besar
            Row(verticalAlignment = Alignment.Bottom) {
                Text(String.format("%.1f", bmi), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF334155))
                Text(" kg/m²", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
            }

            Spacer(Modifier.height(12.dp))
            // Slim Progress Bar
            Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                drawRoundRect(color = Color(0xFFF1F5F9), size = size, cornerRadius = CornerRadius(10f))
                drawRoundRect(color = color, size = size.copy(width = size.width * progress), cornerRadius = CornerRadius(10f))
            }
        }
    }
}

@Composable
fun VitalSignsCleanGrid(item: QueueItem) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VitalStatItem("Tensi", item.bloodPressure.ifEmpty { "-" }, "mmHg", Icons.Outlined.FavoriteBorder, Color(0xFFE91E63), Modifier.weight(1f))
        VitalStatItem("Suhu", "${item.temperature}", "°C", Icons.Outlined.Thermostat, Color(0xFFFF9800), Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VitalStatItem("Berat", "${item.weightKg}", "kg", Icons.Outlined.MonitorWeight, Color(0xFF2196F3), Modifier.weight(1f))
        VitalStatItem("Tinggi", "${item.heightCm}", "cm", Icons.Outlined.Height, Color(0xFF4CAF50), Modifier.weight(1f))
    }
}

@Composable
fun VitalStatItem(label: String, value: String, unit: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    Spacer(Modifier.width(2.dp))
                    Text(unit, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 10.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}

@Composable
fun MedicalReportCard(item: QueueItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(0.dp)) {

            // Header Diagnosa
            Column(Modifier.padding(20.dp).fillMaxWidth().background(Color(0xFFF8FAFC))) {
                SectionLabel("DIAGNOSA UTAMA", Icons.Default.Circle, PrimaryPeriwinkle)
                Spacer(Modifier.height(8.dp))
                Text(
                    item.diagnosis.ifEmpty { "Belum ada diagnosa" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F172A)
                )
            }

            Divider(color = Color(0xFFE2E8F0))

            // SOAP Content
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

                ReportField("Keluhan Pasien (Subjective)", item.keluhan)
                ReportField("Pemeriksaan Fisik (Objective)", item.physicalExam.ifEmpty { "-" })

                Divider(color = Color(0xFFF1F5F9))

                ReportField("Tindakan Medis", item.treatment.ifEmpty { "-" }, isImportant = true)
                ReportField("Resep Obat", item.prescription.ifEmpty { "-" })

                if (item.doctorNotes.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFFFFBEB), // Light Yellow
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.EditNote, null, modifier = Modifier.size(16.dp), tint = Color(0xFFB45309))
                                Spacer(Modifier.width(8.dp))
                                Text("Catatan Dokter", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(item.doctorNotes, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF78350F))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(8.dp), tint = color)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color, letterSpacing = 1.sp)
    }
}

@Composable
fun ReportField(label: String, value: String, isImportant: Boolean = false) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isImportant) Color(0xFF0F172A) else Color(0xFF334155),
            fontWeight = if (isImportant) FontWeight.SemiBold else FontWeight.Normal,
            lineHeight = 22.sp
        )
    }
}