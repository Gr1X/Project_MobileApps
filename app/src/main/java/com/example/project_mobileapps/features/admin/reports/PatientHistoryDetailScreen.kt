// File BARU: features/admin/reports/PatientHistoryDetailScreen.kt

package com.example.project_mobileapps.features.admin.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.ui.components.CircularBackButton
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.res.painterResource
import com.example.project_mobileapps.R
import com.example.project_mobileapps.data.model.HistoryItem

/**
 * Composable for displaying the detailed visit history of a specific patient.
 * This screen is typically accessed by an Admin from the main reports screen.
 *
 * @param viewModel The [PatientHistoryDetailViewModel] that provides the patient's data and visit history.
 * @param onNavigateBack Callback function to navigate back to the previous screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHistoryDetailScreen(
    viewModel: PatientHistoryDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Detail Pasien") },
                navigationIcon = { CircularBackButton(onClick = onNavigateBack) }
            )
        }
    ) { padding ->
        // Handle loading state
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Use LazyColumn for efficient scrolling of potentially long visit histories.
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Item 1: Patient Information Header
                item {
                    uiState.patient?.let { PatientInfoHeader(patient = it) }
                }

                // Item 2: Section Title for Visit History
                item {
                    Text(
                        "Daftar Kunjungan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Item 3 onwards: List of Visit History Cards or an empty message.
                if (uiState.visitHistory.isEmpty()) {
                    item { Text("Pasien ini belum memiliki riwayat kunjungan.") }
                } else {
                    items(uiState.visitHistory) { visit ->
                        VisitHistoryCard(visit = visit)
                    }
                }
            }
        }
    }
}

/**
 * Displays a header card with the patient's main profile information.
 *
 * @param patient The [User] object containing the patient's details.
 */
@Composable
private fun PatientInfoHeader(patient: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = patient.profilePictureUrl,
                    contentDescription = "Foto Profil",
                    placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                    error = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(patient.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(patient.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Divider()
            // Displays key info like Gender, DOB, and Phone in a compact row.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                InfoChip("Gender", patient.gender.name)
                InfoChip("Tgl. Lahir", patient.dateOfBirth)
                InfoChip("Telepon", patient.phoneNumber)
            }
        }
    }
}

/**
 * A small, reusable Composable to display a piece of information with a label.
 * e.g., "Gender" (label) and "PRIA" (value).
 */
@Composable
private fun InfoChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Displays a card for a single past visit, showing the date, complaint, and status.
 *
 * @param visit The [HistoryItem] object for a specific visit.
 */
@Composable
private fun VisitHistoryCard(visit: HistoryItem) { // <-- Tipe diubah ke HistoryItem
    val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Gunakan properti yang benar dari HistoryItem
            Text(visit.visitDate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Keluhan Awal:", style = MaterialTheme.typography.labelMedium)
            Text(visit.initialComplaint, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))
            // Status text is colored based on whether it was completed or cancelled.
            Text(
                "Status: ${visit.status.name}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (visit.status == QueueStatus.SELESAI) Color(0xFF00C853) else MaterialTheme.colorScheme.error
            )
        }
    }
}