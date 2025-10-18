package com.example.project_mobileapps.data.repo

import com.example.project_mobileapps.data.model.NotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
/**
 * Singleton object yang berfungsi sebagai repository (in-memory) untuk notifikasi.
 * Mengelola daftar notifikasi yang akan ditampilkan di dalam aplikasi.
 */
object NotificationRepository {
    private val _notificationsFlow = MutableStateFlow<List<NotificationItem>>(emptyList())
    /**
     * Aliran data reaktif (StateFlow) yang diekspos ke UI.
     * UI akan mengamati ini untuk menampilkan daftar notifikasi.
     */
    val notificationsFlow = _notificationsFlow.asStateFlow()
    /**
     * Menambahkan notifikasi baru ke daftar.
     * @param message Isi pesan notifikasi.
     * @param targetUserId (Opsional) ID user target, agar notifikasi hanya muncul untuk user tsb.
     */

    fun addNotification(message: String, targetUserId: String? = null) {
        val newNotification = NotificationItem(message = message, targetUserId = targetUserId)
        _notificationsFlow.update { currentList ->
            listOf(newNotification) + currentList
        }
    }
    /**
     * Menandai semua notifikasi yang ada sebagai 'telah dibaca'.
     */
    fun markAllAsRead() {
        _notificationsFlow.update { currentList ->
            currentList.map { it.copy(isRead = true) }
        }
    }
    /**
     * Menghapus (dismiss) notifikasi tertentu dari daftar.
     * @param id ID unik dari notifikasi yang akan dihapus.
     */
    fun dismissNotification(id: String) {
        _notificationsFlow.update { currentList ->
            currentList.filterNot { it.id == id }
        }
    }
}