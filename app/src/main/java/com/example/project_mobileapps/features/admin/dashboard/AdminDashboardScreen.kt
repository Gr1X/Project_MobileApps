package com.example.project_mobileapps.features.admin.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.features.admin.AdminDashboardViewModel
import com.example.project_mobileapps.features.admin.AdminDashboardViewModelFactory
import com.example.project_mobileapps.ui.components.PatientStatsChart

@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel = viewModel(
        factory = AdminDashboardViewModelFactory(AppContainer.queueRepository)
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Hello, Admin 👋",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "How are you feeling today?",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Light
            )
        }

        item {
            StatsHeader(
                total = uiState.totalPatientsToday,
                waiting = uiState.patientsWaiting,
                finished = uiState.patientsFinished
            )
        }

        item {
            Button(
                onClick = { viewModel.callNextPatient(context) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.practiceStatus?.isPracticeOpen ?: false
            ) {
                Text("Panggil Pasien Berikutnya")
            }
        }

        item {
            AdminPracticeStatusCard(status = uiState.practiceStatus)
        }

        item {
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Pantau Antrian Hari Ini", style = MaterialTheme.typography.titleMedium)
        }

        if (uiState.isLoading) {
            item {
                CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally))
            }
        } else if (uiState.queueList.isEmpty()) {
            item {
                Text("Belum ada pasien dalam antrian.", modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally))
            }
        } else {
            items(uiState.queueList) { queueItem ->
                AdminPatientInfoCard(item = queueItem)
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            PatientStatsChart(reportData = uiState.weeklyReport)
        }
    }
}

@Composable
fun StatsHeader(total: Int, waiting: Int, finished: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            label = "Total Pasien",
            value = total.toString(),
            icon = Icons.Outlined.Groups,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Menunggu",
            value = waiting.toString(),
            icon = Icons.Outlined.HourglassTop,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Selesai",
            value = finished.toString(),
            icon = Icons.Outlined.CheckCircle,
            color = Color(0xFF00C853),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AdminPracticeStatusCard(status: PracticeStatus?) {
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


@Composable
fun AdminPatientInfoCard(item: QueueItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (item.status) {
                QueueStatus.DILAYANI -> MaterialTheme.colorScheme.primaryContainer
                QueueStatus.SELESAI -> MaterialTheme.colorScheme.surfaceVariant
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
                Text(text = "No. ${item.queueNumber}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(item.userName, style = MaterialTheme.typography.bodyLarge)
            }
            Text(item.status.name, fontWeight = FontWeight.Medium)
        }
    }
}