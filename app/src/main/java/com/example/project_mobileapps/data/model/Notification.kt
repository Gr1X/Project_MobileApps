package com.example.project_mobileapps.data.model

import java.util.UUID
import java.util.Date

/**
 * Merepresentasikan satu item notifikasi di dalam aplikasi.
 * Digunakan untuk memberitahu pengguna tentang event penting, seperti antrian dipanggil,
 * status praktik berubah, atau pengingat lainnya.
 *
 * @property id ID unik untuk setiap notifikasi, dibuat secara otomatis menggunakan UUID.
 * @property message Isi pesan teks dari notifikasi yang akan ditampilkan ke pengguna.
 * @property timestamp Waktu dan tanggal kapan notifikasi ini dibuat, default-nya adalah waktu saat ini.
 * @property isRead Flag untuk menandai apakah notifikasi sudah dibaca oleh pengguna atau belum.
 * @property targetUserId ID pengguna spesifik yang menjadi target notifikasi. Jika null, notifikasi bersifat umum (broadcast).
 */
data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val timestamp: Date = Date(),
    var isRead: Boolean = false,
    val targetUserId: String? = null
)