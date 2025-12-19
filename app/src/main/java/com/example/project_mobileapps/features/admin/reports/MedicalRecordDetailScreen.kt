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
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.FirestoreQueueRepository
import com.example.project_mobileapps.data.repo.UserRepository
import com.example.project_mobileapps.features.admin.manageSchedule.calculateAge
import com.example.project_mobileapps.ui.themes.*
import java.util.Calendar
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordDetailScreen(
    queueId: String,
    onNavigateBack: () -> Unit
) {
    // State
    var queueItem by remember { mutableStateOf<QueueItem?>(null) }
    var patientUser by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val userRepository = remember { UserRepository() }

    LaunchedEffect(queueId) {
        val item = FirestoreQueueRepository.getQueueById(queueId)
        queueItem = item
        if (item != null && item.userId.isNotEmpty()) {
            val user = userRepository.getUser(item.userId)
            patientUser = user
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Rekam Medis", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else if (queueItem == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Data tidak ditemukan.", color = TextSecondary)
            }
        } else {
            val item = queueItem!!

            // Format Tanggal Kunjungan (Menggunakan fungsi robust dari file sebelah jika ada, atau logic simple disini)
            val dateToParse = (item.finishedAt ?: item.createdAt).toString()
            val (day, month, _) = remember(dateToParse) { parseDateRobust(dateToParse) } // Pastikan fungsi ini accessible atau copy ulang logicnya
            val year = Calendar.getInstance().get(Calendar.YEAR)
            val fullDateStr = "$day $month $year"

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. HEADER PROFIL PASIEN (LENGKAP)
                PatientIdentityCardComplete(item, patientUser, fullDateStr)

                // 2. TANDA VITAL
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Pemeriksaan Fisik")
                    VitalSignsSection(item)
                }

                // 3. DIAGNOSA & TINDAKAN
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Diagnosa & Penanganan")
                    ClinicalSummaryCard(item)
                }

                // 4. Footer
                DisclaimerFooter()
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

// ==========================================
// SECTIONS & COMPONENTS
// ==========================================

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

/**
 * KARTU PROFIL PASIEN (VERSI LENGKAP)
 * Menampilkan: Foto, Nama, Email, No Antrian, Gender, Tgl Lahir, Umur, No HP.
 */
@Composable
fun PatientIdentityCardComplete(item: QueueItem, user: User?, visitDateStr: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            // --- BARIS 1: FOTO, NAMA, EMAIL, NO ANTRIAN ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Foto Profil
                if (user?.profilePictureUrl != null && user.profilePictureUrl.isNotBlank()) {
                    AsyncImage(
                        model = user.profilePictureUrl,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(60.dp).background(BrandPrimary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            item.userName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                // Info Utama
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.userName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (user != null) {
                        Text(
                            user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = BrandPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "No. Antrian #${item.queueNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(Modifier.height(20.dp))

            // --- BARIS 2: DATA DETAIL (GRID) ---
            // Data Diri Lengkap: Gender, Tgl Lahir, Telepon
            if (user != null) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Baris A: Gender & Umur
                    Row(modifier = Modifier.fillMaxWidth()) {
                        DataDetailItem(
                            label = "Jenis Kelamin",
                            value = user.gender.name.lowercase().replaceFirstChar { it.uppercase() },
                            icon = Icons.Outlined.Person,
                            modifier = Modifier.weight(1f)
                        )
                        DataDetailItem(
                            label = "Usia",
                            value = calculateAge(user.dateOfBirth),
                            icon = Icons.Outlined.Cake,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Baris B: Tgl Lahir & Telepon
                    Row(modifier = Modifier.fillMaxWidth()) {
                        DataDetailItem(
                            label = "Tanggal Lahir",
                            value = user.dateOfBirth,
                            icon = Icons.Outlined.CalendarMonth,
                            modifier = Modifier.weight(1f)
                        )
                        DataDetailItem(
                            label = "No. Telepon",
                            value = user.phoneNumber,
                            icon = Icons.Outlined.Phone,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Baris C: Tanggal Kunjungan (Khusus record ini)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        DataDetailItem(
                            label = "Tanggal Periksa",
                            value = visitDateStr,
                            icon = Icons.Outlined.Event,
                            modifier = Modifier.weight(1f)
                        )
                        // Spacer untuk layout grid rata kiri
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            } else {
                Text("Detail profil pasien tidak tersedia.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
fun DataDetailItem(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = TextSecondary.copy(alpha = 0.7f))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}

@Composable
fun VitalSignsSection(item: QueueItem) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Baris 1: Tensi & Suhu
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VitalStatCard(
                label = "Tekanan Darah",
                value = item.bloodPressure.ifEmpty { "-" },
                unit = "mmHg",
                icon = Icons.Outlined.FavoriteBorder,
                color = Color(0xFFE91E63), // Pink
                modifier = Modifier.weight(1f)
            )
            VitalStatCard(
                label = "Suhu Tubuh",
                value = "${item.temperature}",
                unit = "°C",
                icon = Icons.Outlined.Thermostat,
                color = Color(0xFFFF9800), // Orange
                modifier = Modifier.weight(1f)
            )
        }

        // Baris 2: Berat & Tinggi
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VitalStatCard(
                label = "Berat Badan",
                value = "${item.weightKg}",
                unit = "kg",
                icon = Icons.Filled.MonitorWeight,
                color = BrandPrimary, // Biru
                modifier = Modifier.weight(1f)
            )
            VitalStatCard(
                label = "Tinggi Badan",
                value = "${item.heightCm}",
                unit = "cm",
                icon = Icons.Outlined.Height,
                color = Color(0xFF4CAF50), // Hijau
                modifier = Modifier.weight(1f)
            )
        }

        // Baris 3: BMI (Jika ada data)
        if (item.weightKg > 0 && item.heightCm > 0) {
            BmiBarCompact(weight = item.weightKg, height = item.heightCm)
        }
    }
}

@Composable
fun VitalStatCard(
    label: String, value: String, unit: String, icon: ImageVector, color: Color, modifier: Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(38.dp).background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    if (value != "-" && value != "0" && value != "0.0") {
                        Spacer(Modifier.width(2.dp))
                        Text(unit, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(bottom = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BmiBarCompact(weight: Double, height: Double) {
    val heightM = height / 100
    val bmi = weight / heightM.pow(2)
    val (status, color) = when {
        bmi < 18.5 -> "Kurang" to Color(0xFFFFB74D)
        bmi < 24.9 -> "Normal" to Color(0xFF66BB6A)
        bmi < 29.9 -> "Berlebih" to Color(0xFFFF7043)
        else -> "Obesitas" to Color(0xFFEF5350)
    }
    val progress = (bmi / 40.0).coerceIn(0.0, 1.0).toFloat()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("BMI: ", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(String.format("%.1f", bmi), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text(status, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Canvas(modifier = Modifier.width(100.dp).height(6.dp)) {
                drawRoundRect(color = Color(0xFFF1F5F9), size = size, cornerRadius = CornerRadius(10f))
                drawRoundRect(color = color, size = size.copy(width = size.width * progress), cornerRadius = CornerRadius(10f))
            }
        }
    }
}

@Composable
fun ClinicalSummaryCard(item: QueueItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

            // Diagnosa Utama (Highlight)
            Column {
                Text("DIAGNOSA UTAMA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BrandPrimary, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    item.diagnosis.ifEmpty { "Belum ada diagnosa" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Subjective & Objective
            ReportItem("Keluhan (Subjective)", item.keluhan)
            ReportItem("Pemeriksaan Fisik (Objective)", item.physicalExam.ifEmpty { "-" })

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Plan / Action
            ReportItem("Tindakan Medis", item.treatment.ifEmpty { "-" }, highlight = true)
            ReportItem("Resep Obat", item.prescription.ifEmpty { "-" })

            if (item.doctorNotes.isNotEmpty()) {
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Catatan Tambahan", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                        Spacer(Modifier.height(4.dp))
                        Text(item.doctorNotes, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF78350F))
                    }
                }
            }
        }
    }
}

@Composable
fun ReportItem(label: String, value: String, highlight: Boolean = false) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = if(highlight) TextPrimary else Color(0xFF334155),
            fontWeight = if(highlight) FontWeight.SemiBold else FontWeight.Normal,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun DisclaimerFooter() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Outlined.Info, null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp).padding(top = 2.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "Rekam medis ini bersifat rahasia. Dokumen diterbitkan secara elektronik oleh sistem KlinIQ.",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary.copy(alpha = 0.6f),
            lineHeight = 14.sp
        )
    }
}