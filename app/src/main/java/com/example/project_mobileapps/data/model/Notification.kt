package com.example.project_mobileapps.data.model

import java.util.UUID
import java.util.Date

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val timestamp: Date = Date(),
    var isRead: Boolean = false,
    val targetUserId: String? = null
)