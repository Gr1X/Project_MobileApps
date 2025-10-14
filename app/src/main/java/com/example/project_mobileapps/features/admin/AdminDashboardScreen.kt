package com.example.project_mobileapps.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.features.doctor.DoctorUiState
import com.example.project_mobileapps.features.doctor.DoctorViewModel
import com.example.project_mobileapps.features.doctor.DoctorViewModelFactory
import com.example.project_mobileapps.features.doctor.PatientQueueCard // Kita pakai ulang dari UI Dokter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogoutClick: () -> Unit,
    viewModel: DoctorViewModel = viewModel(
        factory = DoctorViewModelFactory(AppContainer.queueRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Admin") },
                actions = {
                    TextButton(onClick = onLogoutClick) {
                        Text("Logout")
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
        ) {

            AdminPracticeStatusCard(status = uiState.practiceStatus)

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Pantau Antrian Hari Ini", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (uiState.queueList.isEmpty()) {
                Text("Belum ada pasien dalam antrian.", modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.queueList) { queueItem ->
                        AdminPatientInfoCard(item = queueItem)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPracticeStatusCard(status: com.example.project_mobileapps.data.model.PracticeStatus?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Status Praktik Dokter", style = MaterialTheme.typography.labelMedium)
            val statusText = if (status?.isPracticeOpen == true) "Buka" else "Tutup"
            Text(statusText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Sedang Dilayani: No. ${status?.currentServingNumber ?: 0}")
            Text("Total Pasien Hari Ini: ${status?.totalServed ?: 0} pasien")
        }
    }
}

// =======================================================
// BUAT COMPOSABLE BARU YANG LEBIH SEDERHANA UNTUK ADMIN
// =======================================================
@Composable
fun AdminPatientInfoCard(item: QueueItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (item.status) {
                com.example.project_mobileapps.data.model.QueueStatus.DILAYANI -> MaterialTheme.colorScheme.primaryContainer
                com.example.project_mobileapps.data.model.QueueStatus.SELESAI -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "No. ${item.queueNumber}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(item.userName, style = MaterialTheme.typography.bodyLarge)
            }
            Text(item.status.name, fontWeight = FontWeight.Medium)
        }
    }
}