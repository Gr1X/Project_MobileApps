package com.example.project_mobileapps.features.patient.doctorDetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.project_mobileapps.R
import com.example.project_mobileapps.data.local.DailyScheduleData
import com.example.project_mobileapps.ui.components.CircularBackButton
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet
import com.example.project_mobileapps.ui.themes.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDetailScreen(
    viewModel: DoctorDetailViewModel = viewModel(),
    onBookingClick: (doctorId: String, keluhan: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val doctor by viewModel.doctor.collectAsState()
    val schedule by viewModel.schedule.collectAsState()
    val practiceStatus by viewModel.practiceStatus.collectAsState() // <-- Ambil status praktik
    var keluhan by remember { mutableStateOf("") }
    var showConfirmationSheet by remember { mutableStateOf(false) }

    // Tentukan apakah praktik buka atau tutup
    val isPracticeOpen = practiceStatus?.isPracticeOpen ?: false

    if (showConfirmationSheet && doctor != null) {
        ConfirmationBottomSheet(
            onDismiss = { showConfirmationSheet = false },
            onConfirm = {
                showConfirmationSheet = false
                onBookingClick(doctor!!.id, keluhan)
            },
            title = "Konfirmasi Antrian?",
            text = "Dengan menekan 'Konfirmasi', Anda akan dimasukkan ke dalam daftar antrian. Pastikan Anda dapat hadir di lokasi praktik."
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil Dokter") },
                navigationIcon = { CircularBackButton(onClick = onNavigateBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { showConfirmationSheet = true },
                        // --- PERUBAHAN DI SINI ---
                        enabled = isPracticeOpen && keluhan.isNotBlank(), // Tombol aktif jika praktik BUKA & keluhan diisi
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            // Teks tombol juga dinamis
                            text = if (isPracticeOpen) "Ambil Nomor Antrian" else "Praktik Sedang Tutup",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (doctor != null) {
            val currentDoctor = doctor!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                AsyncImage(
                    model = currentDoctor.photoUrl,
                    contentDescription = "Foto ${currentDoctor.name}",
                    placeholder = painterResource(id = R.drawable.doctor_budi),
                    error = painterResource(id = R.drawable.doctor_budi),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = currentDoctor.name,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentDoctor.specialization,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                // --- PERUBAHAN DI SINI: TAMPILKAN JADWAL ---
                ScheduleInfo(schedule)
                Spacer(modifier = Modifier.height(24.dp))
                // -----------------------------------------

                Text(
                    text = "Keluhan Awal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    OutlinedTextField(
                        value = keluhan,
                        onValueChange = { keluhan = it },
                        placeholder = { Text("Tuliskan keluhan awal Anda di sini...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 150.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

// --- COMPOSABLE BARU UNTUK MENAMPILKAN JADWAL ---
@Composable
fun ScheduleInfo(schedule: List<DailyScheduleData>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text("Jadwal Praktik", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (schedule.isEmpty()) {
                    Text("Jadwal tidak tersedia.", color = TextSecondary)
                } else {
                    schedule.forEach { day ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(day.dayOfWeek, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (day.isOpen) "${day.startTime} - ${day.endTime}" else "Tutup",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (day.isOpen) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (day.isOpen) MaterialTheme.colorScheme.primary else TextSecondary
                            )
                        }
                        if (schedule.last() != day) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}
