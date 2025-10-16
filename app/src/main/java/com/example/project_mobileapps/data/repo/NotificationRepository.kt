package com.example.project_mobileapps.data.repo

import com.example.project_mobileapps.data.model.NotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object NotificationRepository {
    private val _notificationsFlow = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notificationsFlow = _notificationsFlow.asStateFlow()

    fun addNotification(message: String, targetUserId: String? = null) {
        val newNotification = NotificationItem(message = message, targetUserId = targetUserId)
        _notificationsFlow.update { currentList ->
            listOf(newNotification) + currentList
        }
    }

    fun markAllAsRead() {
        _notificationsFlow.update { currentList ->
            currentList.map { it.copy(isRead = true) }
        }
    }

    fun dismissNotification(id: String) {
        _notificationsFlow.update { currentList ->
            currentList.filterNot { it.id == id }
        }
    }
}