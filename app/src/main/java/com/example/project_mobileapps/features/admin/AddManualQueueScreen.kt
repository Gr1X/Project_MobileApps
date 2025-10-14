package com.example.project_mobileapps.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualQueueScreen(
    onNavigateBack: () -> Unit,
    onAddQueue: (patientName: String, complaint: String) -> Unit
) {
    var patientName by remember { mutableStateOf("") }
    var complaint by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Antrian Manual") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Informasi Pasien",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Masukkan detail pasien untuk menambahkannya ke dalam antrian hari ini.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Kolom Input Nama Pasien dengan Ikon
            OutlinedTextField(
                value = patientName,
                onValueChange = { patientName = it },
                label = { Text("Nama Pasien") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Outlined.Person, contentDescription = "Nama Pasien")
                },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Kolom Input Keluhan dengan Ikon
            OutlinedTextField(
                value = complaint,
                onValueChange = { complaint = it },
                label = { Text("Keluhan Awal (Opsional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                leadingIcon = {
                    Icon(Icons.Outlined.Notes, contentDescription = "Keluhan")
                }
            )
            Spacer(modifier = Modifier.weight(1f)) // Mendorong tombol ke bawah

            // Tombol Simpan
            Button(
                onClick = { onAddQueue(patientName, complaint) },
                enabled = patientName.isNotBlank(), // Aktif jika nama sudah diisi
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Tambah ke Antrian")
            }
        }
    }
}