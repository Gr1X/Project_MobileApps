package com.example.project_mobileapps.data.repo

import com.example.project_mobileapps.data.model.DailyReport
import com.example.project_mobileapps.data.model.DailyScheduleData
import com.example.project_mobileapps.data.model.HistoryItem
import com.example.project_mobileapps.data.model.MedicalRecord // <-- Import Baru
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import kotlinx.coroutines.flow.StateFlow
import java.util.Date

interface QueueRepository {
    val practiceStatusFlow: StateFlow<Map<String, PracticeStatus>>
    val dailyQueuesFlow: StateFlow<List<QueueItem>>


    suspend fun getPatientMedicalHistory(patientId: String): Result<List<MedicalRecord>>
    // [UBAH INI] Harus memiliki 3 parameter
    suspend fun addManualQueue(
        patientName: String,
        complaint: String,
        phoneNumber: String,
        gender: String, // Baru
        dob: String     // Baru
    ): Result<QueueItem>

    // [TAMBAH INI] Fungsi baru yang dibutuhkan oleh ViewModel
    suspend fun addQueueByUserId(userId: String, userName: String, complaint: String): Result<QueueItem>
    suspend fun takeQueueNumber(doctorId: String, userId: String, userName: String, keluhan: String): Result<QueueItem>
    suspend fun cancelQueue(userId: String, doctorId: String): Result<Unit>
    suspend fun callNextPatient(doctorId: String): Result<Unit>
    suspend fun setPracticeOpen(doctorId: String, isOpen: Boolean): Result<Unit>
    suspend fun confirmPatientArrival(queueId: Int, doctorId: String): Result<Unit>
    suspend fun updatePracticeStatus(doctorId: String, isOpen: Boolean): Result<Unit>

    // [TAMBAHAN BARU] Update Status Antrian Pasien (Menunggu -> Dipanggil -> Dilayani)
    suspend fun updateQueueStatus(queueId: String, status: QueueStatus): Result<Unit>
    // Fungsi ini bisa dihapus atau dikosongkan karena logic-nya pindah ke submitMedicalRecord
    suspend fun finishConsultation(
        queueNumber: Int,
        doctorId: String,
        diagnosis: String,
        treatment: String,
        prescription: String,
        notes: String
    ): Result<Unit>

    suspend fun submitMedicalRecord(
        queueId: String,
        medicalData: Map<String, Any>
    ): Result<Unit>

    suspend fun getPatientStatistics(daysBack: Int): Map<String, Int>
    suspend fun getGenderStatistics(daysBack: Int): Map<String, Int>
    suspend fun getVisitHistory(userId: String): List<HistoryItem>
    suspend fun checkForLatePatients(doctorId: String)
    suspend fun getWeeklyReport(): List<DailyReport>
    suspend fun getQueuesByDateRange(start: Date, end: Date): List<QueueItem>
    suspend fun resetQueue()
    suspend fun getDoctorSchedule(doctorId: String): List<DailyScheduleData>
    suspend fun updateDoctorSchedule(doctorId: String, newSchedule: List<DailyScheduleData>): Result<Unit>
    suspend fun updateEstimatedServiceTime(doctorId: String, minutes: Int): Result<Unit>
    suspend fun updatePatientCallTimeLimit(doctorId: String, minutes: Int): Result<Unit>
    suspend fun confirmArrivalByQr(documentId: String): Result<Unit>

    // [OPSIONAL] Helper untuk mengambil 1 record detail
    suspend fun getMedicalRecordById(recordId: String): MedicalRecord?

    // Tetap ada untuk ambil data antrian logistik
    suspend fun getQueueById(queueId: String): QueueItem?
}