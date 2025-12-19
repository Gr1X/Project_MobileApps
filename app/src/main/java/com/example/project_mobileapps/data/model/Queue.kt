package com.example.project_mobileapps.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

enum class QueueStatus {
    MENUNGGU,
    DIPANGGIL,
    DILAYANI,
    SELESAI,
    DIBATALKAN
}

/**
 * QUEUE_TICKETS (Sesuai ERD)
 * Hanya menyimpan data operasional antrian.
 * Data medis dipindah ke MedicalRecord.kt
 */
data class QueueItem(
    @DocumentId
    val id: String = "",
    val queueNumber: Int = 0,

    // Foreign Keys (Referensi)
    val userId: String = "",      // Referensi ke Users (Pasien)
    val doctorId: String = "",    // Referensi ke Users (Dokter)

    // Display Info (Redundansi untuk performa UI biar gak perlu fetch User lagi)
    val userName: String = "",
    val keluhan: String = "",     // Tetap di sini sebagai info awal buat Admin/Dokter

    // State Management
    var status: QueueStatus = QueueStatus.MENUNGGU,
    @get:PropertyName("missedCallCount")
    @set:PropertyName("missedCallCount")
    var missedCallCount: Int = 0,

    // --- FIELD MEDIS TERSEMBUNYI (DITANAMKAN DI QUEUE ITEM) ---
    // Ini adalah kunci agar pasien bisa melihat riwayat (History)
    val diagnosis: String = "",
    val treatment: String = "",
    val prescription: String = "",
    val doctorNotes: String = "",
    val bloodPressure: String = "",
    val temperature: Double = 0.0,
    val weightKg: Double = 0.0,
    val heightCm: Double = 0.0,
    val physicalExam: String = "",
    // ---------------------------------------------------------

    // Timestamps
    val createdAt: Date = Date(),
    var calledAt: Date? = null,
    var startedAt: Date? = null,
    var finishedAt: Date? = null
)

/**
 * PRACTICE_STATUS (Sesuai ERD)
 * Menyimpan state Live dari klinik
 */
data class PracticeStatus(
    val doctorId: String = "",
    val doctorName: String = "",
    var currentServingNumber: Int = 0,
    var lastQueueNumber: Int = 0,
    val dailyPatientLimit: Int = 50,
    val estimatedServiceTimeInMinutes: Int = 15,
    @get:PropertyName("isPracticeOpen")
    @set:PropertyName("isPracticeOpen")
    var isPracticeOpen: Boolean = false,
    var totalServed: Int = 0,

    // Jam Operasional & Aturan
    val openingHour: Int = 8,
    val closingHour: Int = 21,
    val patientCallTimeLimitMinutes: Int = 15
)