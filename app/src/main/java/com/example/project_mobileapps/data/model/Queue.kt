package com.example.project_mobileapps.data.model

import java.util.Date

enum class QueueStatus { MENUNGGU, DIPANGGIL, DILAYANI, SELESAI, DIBATALKAN }

data class QueueItem(
    val queueNumber: Int,
    val userId: String,
    val userName: String,
    val doctorId: String,
    val keluhan: String,
    var status: QueueStatus = QueueStatus.MENUNGGU,
    val createdAt: Date = Date(),
    var calledAt: Date? = null,
    var startedAt: Date? = null,
    var finishedAt: Date? = null,
    var hasBeenLate: Boolean = false
)

data class PracticeStatus(
    val doctorId: String,
    val doctorName: String,
    var currentServingNumber: Int = 0,
    var lastQueueNumber: Int = 0,
    val dailyPatientLimit: Int = 50,
    val estimatedServiceTimeInMinutes: Int = 30,
    val isPracticeOpen: Boolean = false,
    var totalServed: Int = 0,
    val openingHour: Int = 9,
    val closingHour: Int = 17,
    val patientCallTimeLimitMinutes: Int = 15
)