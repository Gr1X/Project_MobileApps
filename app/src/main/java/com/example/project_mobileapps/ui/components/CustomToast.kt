// File: ui/components/CustomToast.kt
package com.example.project_mobileapps.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// 1. Enum Tipe Toast
enum class ToastType {
    SUCCESS,
    ERROR,
    INFO
}

// 2. Data Class Pesan
data class ToastMessage(
    val message: String,
    val type: ToastType = ToastType.SUCCESS,
    val id: Long = System.nanoTime()
)

// 3. Toast Manager (Singleton)
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

// 4. UI Composable yang Diimprovisasi
@Composable
fun CustomToast(
    modifier: Modifier = Modifier,
    toastMessage: ToastMessage?,
    onDismiss: () -> Unit
) {
    // Timer Auto-Dismiss
    LaunchedEffect(toastMessage?.id) {
        if (toastMessage != null) {
            delay(4000) // Tampil selama 4 detik
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = toastMessage != null,
        // Animasi Slide dari Atas + Fade
        enter = slideInVertically(
            initialOffsetY = { -it }, // Mulai dari atas layar
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy) // Efek memantul sedikit
        ) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
            .padding(top = 16.dp, start = 16.dp, end = 16.dp) // Margin aman dari status bar
            .zIndex(100f) // Pastikan selalu di paling atas (z-index tinggi)
    ) {
        toastMessage?.let { msg ->
            // Konfigurasi Warna & Ikon berdasarkan Tipe
            val (bgColor, contentColor, iconVec) = when (msg.type) {
                ToastType.SUCCESS -> Triple(
                    Color(0xFFE8F5E9), // Hijau Muda (Background)
                    Color(0xFF1B5E20), // Hijau Tua (Teks/Ikon)
                    Icons.Rounded.CheckCircle
                )
                ToastType.ERROR -> Triple(
                    Color(0xFFFFEBEE), // Merah Muda
                    Color(0xFFB71C1C), // Merah Tua
                    Icons.Rounded.Error
                )
                ToastType.INFO -> Triple(
                    Color(0xFFE3F2FD), // Biru Muda
                    Color(0xFF0D47A1), // Biru Tua
                    Icons.Rounded.Info
                )
            }

            // Desain Kartu Kapsul
            Surface(
                shape = RoundedCornerShape(28.dp), // Bentuk Pill/Kapsul
                color = bgColor,
                contentColor = contentColor,
                border = BorderStroke(1.dp, contentColor.copy(alpha = 0.1f)),
                shadowElevation = 6.dp, // Bayangan agar melayang
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ikon Status dalam Lingkaran Transparan
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(contentColor.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVec,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Teks Pesan
                    Text(
                        text = msg.message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Tombol Tutup (X) Kecil
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Tutup",
                            tint = contentColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}