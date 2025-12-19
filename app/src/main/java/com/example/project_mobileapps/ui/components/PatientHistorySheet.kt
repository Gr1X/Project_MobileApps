// File: features/doctor/components/PatientHistorySheet.kt

package com.example.project_mobileapps.features.doctor.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.data.model.MedicalRecord // <-- IMPORT MODEL BARU
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.project_mobileapps.data.model.PrescriptionItem // <-- IMPORT PRESCRIPTION ITEM

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHistorySheet(
    history: List<MedicalRecord>, // <-- PERBAIKAN 1: Menerima MedicalRecord
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .fillMaxHeight(0.6f)
        ) {
            // Header Sheet
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Riwayat Medis Pasien", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content List
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (history.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada riwayat kunjungan sebelumnya.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(history) { item ->
                        HistoryItemCard(item) // Menggunakan MedicalRecord
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(item: MedicalRecord) { // <-- PERBAIKAN 2: Menerima MedicalRecord
    // Format Tanggal Aman (Menggunakan createdAt dari MedicalRecord)
    val dateStr = try {
        val date = item.createdAt
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
    } catch (e: Exception) {
        "-"
    }

    // PERBAIKAN 3: Konversi List<PrescriptionItem> menjadi string yang dapat dibaca
    val prescriptionText = item.prescriptions.joinToString(separator = ", ") {
        // Mengasumsikan Anda ingin melihat Nama Obat dan Dosisnya
        "${it.medicineName} (${it.dosage})"
    }.ifEmpty {
        // Jika list kosong, ambil resep string yang diinput dokter (medicalAction) sebagai fallback
        item.medicalAction
    }.ifEmpty { "-" }


    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Card
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(dateStr, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(item.doctorName, style = MaterialTheme.typography.labelSmall, color = Color.Gray) // Menggunakan doctorName dari MedicalRecord
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Diagnosa
            Text("Diagnosa:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(item.diagnosis.ifEmpty { "-" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(4.dp))

            // Resep
            Text("Resep Obat:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(prescriptionText, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
    }
}