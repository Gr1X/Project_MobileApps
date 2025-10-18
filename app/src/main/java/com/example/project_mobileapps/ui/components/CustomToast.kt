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
/**
 * Enum untuk mendefinisikan tipe-tipe pesan Toast yang didukung.
 * Mempengaruhi warna dan ikon yang ditampilkan.
 */
// 1. Definisikan tipe-tipe Toast
enum class ToastType {
    SUCCESS,
    ERROR
}

// 2. Buat data class untuk menampung pesan Toast
/**
 * Data class untuk merepresentasikan satu pesan Toast.
 * @param message Teks pesan yang akan ditampilkan.
 * @param type Tipe Toast ([ToastType]), defaultnya [ToastType.SUCCESS].
 */
data class ToastMessage(val message: String, val type: ToastType)

// 3. Buat Manager (singleton) untuk mengontrol state Toast secara global
/**
 * Singleton object (`ToastManager`) untuk mengelola state Toast secara global.
 * Komponen lain dapat memanggil `ToastManager.showToast()` dari mana saja
 * untuk menampilkan pesan. [MainActivity] akan mengamati `toastMessage`
 * dan menampilkan [CustomToast] Composable.
 */
object ToastManager {
    private val _toastMessage = MutableStateFlow<ToastMessage?>(null)
    val toastMessage = _toastMessage.asStateFlow()
    /**
     * Fungsi untuk menampilkan Toast. Mengupdate [_toastMessage].
     * @param message Pesan yang akan ditampilkan.
     * @param type Tipe Toast ([ToastType]).
     */
    fun showToast(message: String, type: ToastType = ToastType.SUCCESS) {
        _toastMessage.update { ToastMessage(message, type) }
    }

    fun hideToast() {
        _toastMessage.update { null }
    }
}

// 4. Buat Composable untuk Tampilan UI Toast
/**
 * Composable untuk menampilkan UI Toast kustom.
 * Toast ini muncul dari atas layar dengan animasi dan hilang otomatis.
 *
 * @param modifier Modifier untuk [AnimatedVisibility].
 * @param toastMessage Objek [ToastMessage] yang sedang aktif (atau null).
 * Diambil dari [ToastManager.toastMessage].
 * @param onDismiss Callback yang dipanggil saat Toast harus disembunyikan
 * (baik otomatis setelah delay, atau jika diperlukan aksi dismiss manual).
 */
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
    /**
     * [LaunchedEffect] ini berfungsi sebagai timer otomatis.
     * `key1 = toastMessage` berarti efek ini akan dibatalkan dan dijalankan ulang
     * setiap kali `toastMessage` berubah (dari null ke pesan, atau pesan ke pesan baru).
     * Jika `toastMessage` tidak null, ia akan menunggu 3 detik (`delay(3000L)`)
     * lalu memanggil `onDismiss` untuk menyembunyikan Toast.
     */
    // Efek untuk menyembunyikan Toast secara otomatis setelah beberapa detik
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3000L) // Tampilkan selama 3 detik
            onDismiss()
        }
    }
    /**
     * [AnimatedVisibility] mengontrol kemunculan dan kehilangan Toast dengan animasi.
     * `visible = toastMessage != null` berarti Composable di dalamnya hanya akan
     * ada di composition tree jika ada pesan Toast.
     * `enter` dan `exit` mendefinisikan animasi (fade in/out, slide up/down).
     */
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