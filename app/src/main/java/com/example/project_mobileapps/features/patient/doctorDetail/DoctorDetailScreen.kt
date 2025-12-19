// File: features/patient/doctorDetail/DoctorDetailScreen.kt
package com.example.project_mobileapps.features.patient.doctorDetail

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.project_mobileapps.R
import com.example.project_mobileapps.data.model.DailyScheduleData
import com.example.project_mobileapps.ui.components.CircularBackButton
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet
import com.example.project_mobileapps.ui.themes.TextSecondary
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDetailScreen(
    viewModel: DoctorDetailViewModel = viewModel(),
    onBookingClick: (doctorId: String, keluhan: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val doctor by viewModel.doctor.collectAsState()
    val schedule by viewModel.schedule.collectAsState()
    val practiceStatus by viewModel.practiceStatus.collectAsState()
    val distance by viewModel.distance.collectAsState() // Observe Jarak

    var keluhan by remember { mutableStateOf("") }
    var showConfirmationSheet by remember { mutableStateOf(false) }

    // --- LOKASI LOGIC ---
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        viewModel.calculateDistanceToClinic(location.latitude, location.longitude)
                    } else {
                        Toast.makeText(context, "Pastikan GPS aktif", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) {
                // Handle error
            }
        } else {
            Toast.makeText(context, "Izin lokasi diperlukan untuk hitung jarak", Toast.LENGTH_SHORT).show()
        }
    }

    // Trigger ambil lokasi saat layar dibuka
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // --- UI UTAMA ---
    val isPracticeOpen = practiceStatus?.isPracticeOpen ?: false

    if (showConfirmationSheet && doctor != null) {
        ConfirmationBottomSheet(
            onDismiss = { showConfirmationSheet = false },
            onConfirm = {
                showConfirmationSheet = false
                onBookingClick(doctor!!.id, keluhan)
            },
            title = "Konfirmasi Antrian?",
            text = "Pastikan Anda dapat hadir di lokasi praktik sebelum jam operasional berakhir."
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil Dokter") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 16.dp, color = Color.White) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { showConfirmationSheet = true },
                        enabled = isPracticeOpen && keluhan.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (isPracticeOpen) "Ambil Nomor Antrian" else "Praktik Sedang Tutup",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFF8F9FA) // Background sedikit abu modern
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
                // 1. HEADER PROFILE (Dibuat lebih bersih)
                Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = currentDoctor.photoUrl,
                        contentDescription = "Foto Dokter",
                        placeholder = painterResource(id = R.drawable.doctor_budi), // Pastikan ada drawable ini
                        error = painterResource(id = R.drawable.doctor_budi),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentDoctor.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = currentDoctor.specialization,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 2. LOKASI KLINIK (FEATURE BARU - PROFESSIONAL CARD)
                ClinicLocationCard(
                    address = viewModel.clinicAddress,
                    distance = distance,
                    onOpenMap = {
                        val uri = Uri.parse(viewModel.getMapIntentUri())
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        intent.setPackage("com.google.android.apps.maps")
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri)) // Fallback browser
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 3. JADWAL PRAKTIK
                ScheduleInfo(schedule)

                Spacer(modifier = Modifier.height(20.dp))

                // 4. FORM KELUHAN
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Keluhan Anda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = keluhan,
                        onValueChange = { keluhan = it },
                        placeholder = { Text("Contoh: Demam tinggi sejak kemarin sore...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 120.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

// --- KOMPONEN UI LOKASI (Professional Look) ---
@Composable
fun ClinicLocationCard(
    address: String,
    distance: String?,
    onOpenMap: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Lokasi Praktik",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.height(150.dp).fillMaxWidth()) {
                // Background Peta Statis (Bisa pakai Image statis biar ringan)
                Image(
                    painter = painterResource(id = R.drawable.background3), // Gunakan background yg ada atau map_placeholder
                    contentDescription = "Map Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient Overlay biar teks terbaca
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                startY = 100f
                            )
                        )
                )

                // Info Jarak (Floating Badge)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.NearMe, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        if (distance != null) {
                            Text(text = distance, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        } else {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                        }
                    }
                }

                // Info Alamat & Tombol (Bottom)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Klinik Utama",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onOpenMap,
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = "Buka Maps")
                    }
                }
            }
        }
    }
}

// --- JADWAL COMPONENT (Dipercantik) ---
@Composable
fun ScheduleInfo(schedule: List<DailyScheduleData>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("Jadwal Praktik", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (schedule.isEmpty()) {
                    Text("Jadwal tidak tersedia.", color = TextSecondary)
                } else {
                    schedule.forEachIndexed { index, day ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(day.dayOfWeek, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF424242))
                            if (day.isOpen) {
                                Text(
                                    "${day.startTime} - ${day.endTime}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text("Tutup", style = MaterialTheme.typography.bodyMedium, color = Color.Red.copy(alpha = 0.6f))
                            }
                        }
                        if (index < schedule.size - 1) {
                            Divider(color = Color(0xFFF5F5F5))
                        }
                    }
                }
            }
        }
    }
}