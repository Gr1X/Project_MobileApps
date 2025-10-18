// File BARU: ui/components/CustomToast.kt

package com.example.project_mobileapps.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// 1. Definisikan tipe-tipe Toast
enum class ToastType {
    SUCCESS,
    ERROR
}

// 2. Buat data class untuk menampung pesan Toast
data class ToastMessage(val message: String, val type: ToastType)

// 3. Buat Manager (singleton) untuk mengontrol state Toast secara global
object ToastManager {
    private val _toastMessage = MutableStateFlow<ToastMessage?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    fun showToast(message: String, type: ToastType = ToastType.SUCCESS) {
        _toastMessage.update { ToastMessage(message, type) }
    }

    fun hideToast() {
        _toastMessage.update { null }
    }
}

// 4. Buat Composable untuk Tampilan UI Toast
@Composable
fun CustomToast(
    modifier: Modifier = Modifier,
    toastMessage: ToastMessage?,
    onDismiss: () -> Unit
) {
    val (backgroundColor, icon, iconColor) = when (toastMessage?.type) {
        ToastType.SUCCESS -> Triple(Color(0xFFE8F5E9), Icons.Outlined.CheckCircle, Color(0xFF388E3C)) // Hijau
        ToastType.ERROR -> Triple(MaterialTheme.colorScheme.errorContainer, Icons.Outlined.ErrorOutline, MaterialTheme.colorScheme.onErrorContainer) // Merah
        null -> Triple(Color.Transparent, null, Color.Transparent)
    }

    // Efek untuk menyembunyikan Toast secara otomatis setelah beberapa detik
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3000L) // Tampilkan selama 3 detik
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = toastMessage != null,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = modifier
    ) {
        toastMessage?.let {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = backgroundColor)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (icon != null) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconColor)
                    }
                    Text(
                        text = it.message,
                        color = contentColorFor(backgroundColor),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}