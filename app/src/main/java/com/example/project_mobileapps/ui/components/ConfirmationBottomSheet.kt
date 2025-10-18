package com.example.project_mobileapps.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.ui.themes.PrimaryPeriwinkle // Pastikan import ini sesuai
import kotlinx.coroutines.launch
/**
 * Composable reusable untuk menampilkan Modal Bottom Sheet konfirmasi.
 * Menampilkan judul, teks deskripsi, dan dua tombol aksi (Konfirmasi & Batal).
 *
 * @param onDismiss Callback yang dipanggil saat bottom sheet ditutup (baik oleh user
 * swipe ke bawah, klik di luar, atau klik tombol Batal).
 * @param onConfirm Callback yang dipanggil saat tombol "Konfirmasi" diklik.
 * @param title Judul yang ditampilkan di bagian atas bottom sheet.
 * @param text Teks deskripsi/pertanyaan konfirmasi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmationBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    text: String
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    // Fungsi pembantu untuk menutup sheet dengan animasi
    /**
     * Fungsi helper internal untuk menutup bottom sheet dengan animasi.
     * Memanggil [sheetState.hide] lalu [onDismiss] setelah animasi selesai.
     */
    val closeSheet: () -> Unit = {
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            // Judul Dialog
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Teks Deskripsi
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            // Tombol Aksi
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Konfirmasi", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { closeSheet() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Batal", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}