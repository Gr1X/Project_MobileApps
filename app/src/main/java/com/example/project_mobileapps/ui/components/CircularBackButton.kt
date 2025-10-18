package com.example.project_mobileapps.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
/**
 * Composable reusable untuk tombol kembali (back button) berbentuk lingkaran
 * dengan border (outline). Menggunakan [OutlinedIconButton].
 *
 * @param onClick Aksi (lambda) yang dijalankan saat tombol diklik.
 * Biasanya berisi logika `navController.popBackStack()`.
 */
@Composable
fun CircularBackButton(
    onClick: () -> Unit
) {
    OutlinedIconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back"
        )
    }
}