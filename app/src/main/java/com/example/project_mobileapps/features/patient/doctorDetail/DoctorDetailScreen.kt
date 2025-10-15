package com.example.project_mobileapps.features.patient.doctorDetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.project_mobileapps.R
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDetailScreen(
    viewModel: DoctorDetailViewModel = viewModel(),
    onBookingClick: (doctorId: String, keluhan: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val doctor by viewModel.doctor.collectAsState()
    var keluhan by remember { mutableStateOf("") }
    var showConfirmationSheet by remember { mutableStateOf(false) }

    if (showConfirmationSheet) {
        val currentDoctor = doctor!!
        ConfirmationBottomSheet(
            onDismiss = { showConfirmationSheet = false },
            onConfirm = {
                showConfirmationSheet = false
                onBookingClick(currentDoctor.id, keluhan)
            },
            title = "Confirm Queue?",
            subtitle = "Keluhan",
            text = "$keluhan"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(doctor?.name ?: "Detail Dokter") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (doctor != null) {
                val currentDoctor = doctor!!
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    AsyncImage(
                        model = currentDoctor.photoUrl,
                        contentDescription = "Foto ${currentDoctor.name}",
                        placeholder = painterResource(id = R.drawable.ic_launcher_background),
                        error = painterResource(id = R.drawable.ic_launcher_background),
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = currentDoctor.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentDoctor.specialization,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedTextField(
                        value = keluhan,
                        onValueChange = { keluhan = it },
                        label = { Text("Masukkan Keluhan Awal Anda") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showConfirmationSheet = true },
                        enabled = keluhan.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Ambil Nomor Antrian")
                    }
                }
            } else {
                CircularProgressIndicator()
            }
        }
    }
}