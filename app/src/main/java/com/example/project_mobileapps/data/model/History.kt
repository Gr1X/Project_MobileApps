package com.example.project_mobileapps.data.model

// Di data/model/History.kt
data class HistoryItem(
    val visitId: String,
    val userId: String,
    val doctorName: String,
    val visitDate: String,
    val initialComplaint: String,
    val status: QueueStatus
)