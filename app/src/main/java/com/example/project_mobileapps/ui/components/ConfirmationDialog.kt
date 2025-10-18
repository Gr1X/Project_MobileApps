package com.example.project_mobileapps.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
/**
 * Composable reusable wrapper untuk [AlertDialog] standar Material 3.
 * Menyederhanakan pembuatan dialog konfirmasi umum (Judul, Teks, Konfirmasi, Batal).
 *
 * @param onDismiss Callback yang dipanggil saat dialog ditutup (klik di luar atau tombol Batal).
 * @param onConfirm Callback yang dipanggil saat tombol "Konfirmasi" diklik.
 * @param title Judul dialog.
 * @param text Teks isi/deskripsi dialog.
 */
@Composable
fun ConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    text: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Konfirmasi")
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}