package com.example.project_mobileapps.data.repo

import com.example.project_mobileapps.data.local.DailyReport
import com.example.project_mobileapps.data.local.DailyScheduleData
import com.example.project_mobileapps.data.model.HistoryItem
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import kotlinx.coroutines.flow.StateFlow

interface QueueRepository {
    val practiceStatusFlow: StateFlow<Map<String, PracticeStatus>>
    val dailyQueuesFlow: StateFlow<List<QueueItem>>
    suspend fun takeQueueNumber(doctorId: String, userId: String, userName: String, keluhan: String): Result<QueueItem>
    suspend fun cancelQueue(userId: String, doctorId: String): Result<Unit>
    suspend fun callNextPatient(doctorId: String): Result<Unit>
    suspend fun setPracticeOpen(doctorId: String, isOpen: Boolean): Result<Unit>
    suspend fun confirmPatientArrival(queueId: Int, doctorId: String): Result<Unit>
    suspend fun finishConsultation(queueId: Int, doctorId: String): Result<Unit>  // Konsultasi Selesai
    suspend fun getVisitHistory(userId: String): List<HistoryItem>
    suspend fun checkForLatePatients(doctorId: String)
    suspend fun addManualQueue(patientName: String, complaint: String): Result<QueueItem>
    suspend fun getWeeklyReport(): List<DailyReport>
    suspend fun resetQueue()
    suspend fun getDoctorSchedule(doctorId: String): List<DailyScheduleData>
    suspend fun updateDoctorSchedule(doctorId: String, newSchedule: List<DailyScheduleData>): Result<Unit>
    suspend fun updateEstimatedServiceTime(doctorId: String, minutes: Int): Result<Unit>
    suspend fun updatePatientCallTimeLimit(doctorId: String, minutes: Int): Result<Unit>
}