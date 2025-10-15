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
    var calledAt: Date? = null, // Waktu saat giliran dipanggil
    var startedAt: Date? = null,  // Waktu saat konsultasi dimulai (pasien hadir)
    var finishedAt: Date? = null
)

data class PracticeStatus(
    val doctorId: String,
    val doctorName: String,
    var currentServingNumber: Int = 0,
    var lastQueueNumber: Int = 0,
    val dailyPatientLimit: Int = 50,
    val estimatedServiceTimeInMinutes: Int = 30, // Kita sesuaikan jadi 30 menit
    val isPracticeOpen: Boolean = false,
    var totalServed: Int = 0,
    val openingHour: Int = 9,  // <-- TAMBAHKAN JAM BUKA (09:00)
    val closingHour: Int = 200 // <-- TAMBAHKAN JAM TUTUP (17:00)
)