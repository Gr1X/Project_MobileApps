package com.example.project_mobileapps.data.repo

import com.example.project_mobileapps.data.local.DailyReport
import com.example.project_mobileapps.data.local.DailyScheduleData
import com.example.project_mobileapps.data.local.DummyHistoryDatabase
import com.example.project_mobileapps.data.local.DummyReportDatabase
import com.example.project_mobileapps.data.local.DummyScheduleDatabase
import com.example.project_mobileapps.data.model.HistoryItem
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Date
import java.util.Calendar

object DummyQueueRepository : QueueRepository {

    private val doctorList = mapOf("doc_123" to "Dr. Budi Santoso")
    private val FIFTEEN_MINUTES_IN_MS = 1 * 60 * 1000
    private val _practiceStatusFlow = MutableStateFlow(
        mapOf("doc_123" to PracticeStatus(
            doctorId = "doc_123",
            doctorName = "Dr. Budi Santoso",
            currentServingNumber = 4,
            lastQueueNumber = 8,
            isPracticeOpen = true,
            totalServed = 3
        ))
    )

    override val practiceStatusFlow: StateFlow<Map<String, PracticeStatus>> = _practiceStatusFlow.asStateFlow()
    private val _dailyQueuesFlow = MutableStateFlow(
        listOf(
            // Skenario Antrian Hari Ini
            QueueItem(1, "pasien_selesai_1", "Rina Amelia", "doc_123", "Pusing", QueueStatus.SELESAI, finishedAt = Date(System.currentTimeMillis() - 3600000 * 3)),
            QueueItem(2, "pasien_selesai_2", "Joko Susilo", "doc_123", "Sakit Perut", QueueStatus.SELESAI, finishedAt = Date(System.currentTimeMillis() - 3600000 * 2)),
            QueueItem(3, "pasien_selesai_3", "Siti Nurhaliza", "doc_123", "Cek Rutin", QueueStatus.SELESAI, finishedAt = Date(System.currentTimeMillis() - 3600000)),
            QueueItem(4, "pasien03", "Agus Setiawan", "doc_123", "Nyeri Sendi", QueueStatus.DILAYANI, startedAt = Date(System.currentTimeMillis() - 600000)),
            QueueItem(5, "pasien04", "Dewi Anggraini", "doc_123", "Batuk Pilek", QueueStatus.MENUNGGU),
            QueueItem(6, "pasien05", "Eko Prasetyo", "doc_123", "Konsultasi", QueueStatus.MENUNGGU),
            QueueItem(7, "pasien01", "Budi Santoso", "doc_123", "Mata Merah", QueueStatus.MENUNGGU),
            QueueItem(8, "pasien02", "Citra Lestari", "doc_123", "Demam", QueueStatus.MENUNGGU)
        )
    )

    override val dailyQueuesFlow: StateFlow<List<QueueItem>> = _dailyQueuesFlow.asStateFlow()

    override suspend fun checkForLatePatients(doctorId: String) {
        val now = Date().time
        _dailyQueuesFlow.update { currentList ->
            val (latePatients, onTimePatients) = currentList.partition { item ->
                item.status == QueueStatus.DIPANGGIL &&
                        item.calledAt != null &&
                        (now - item.calledAt!!.time > FIFTEEN_MINUTES_IN_MS) &&
                        !item.hasBeenLate
            }

            if (latePatients.isNotEmpty()) {
                val updatedLatePatients = latePatients.map { latePatient ->
                    NotificationRepository.addNotification(
                        message = "Anda terlambat. Nomor antrian ${latePatient.queueNumber} dipindahkan ke akhir.",
                        targetUserId = latePatient.userId
                    )

                    latePatient.copy(status = QueueStatus.MENUNGGU, calledAt = null, hasBeenLate = true)
                }
                onTimePatients + updatedLatePatients
            } else {
                currentList
            }
        }
    }

    override suspend fun takeQueueNumber(doctorId: String, userId: String, userName: String, keluhan: String): Result<QueueItem> {
        delay(500)
        val currentStatus = _practiceStatusFlow.value[doctorId] ?: return Result.failure(Exception("Dokter tidak ditemukan"))
        val currentQueue = _dailyQueuesFlow.value

        if (!currentStatus.isPracticeOpen) return Result.failure(Exception("Pendaftaran sudah ditutup."))
        if (currentStatus.lastQueueNumber >= currentStatus.dailyPatientLimit) return Result.failure(Exception("Antrian penuh untuk hari ini."))

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)


        val totalMinutesRemaining = (currentStatus.closingHour * 60) - ((currentHour * 60) + currentMinute)

        if (totalMinutesRemaining <= 0) {
            return Result.failure(Exception("Waktu praktik sudah habis."))
        }

        val waitingPatientsCount = currentQueue.count {
            it.status == QueueStatus.MENUNGGU || it.status == QueueStatus.DIPANGGIL
        }
        val estimatedTimeForBacklog = waitingPatientsCount * currentStatus.estimatedServiceTimeInMinutes

        if ((estimatedTimeForBacklog + currentStatus.estimatedServiceTimeInMinutes) > totalMinutesRemaining) {
            return Result.failure(Exception("Pendaftaran ditutup karena waktu praktik tidak mencukupi untuk antrian saat ini."))
        }

        val newQueueNumber = currentStatus.lastQueueNumber + 1
        val newQueueItem = QueueItem(newQueueNumber, userId, userName, doctorId, keluhan)

        _dailyQueuesFlow.update { it + newQueueItem }
        _practiceStatusFlow.update {
            it.toMutableMap().apply { this[doctorId] = currentStatus.copy(lastQueueNumber = newQueueNumber) }
        }
        return Result.success(newQueueItem)
    }

    override suspend fun cancelQueue(userId: String, doctorId: String): Result<Unit> {
        _dailyQueuesFlow.update { list -> list.map {
            if (it.userId == userId && it.doctorId == doctorId) it.copy(status = QueueStatus.DIBATALKAN) else it
        }}
        return Result.success(Unit)
    }

    override suspend fun callNextPatient(doctorId: String): Result<Unit> {
        delay(500)
        checkForLatePatients(doctorId)

        val currentStatus = _practiceStatusFlow.value[doctorId] ?: return Result.failure(Exception("Dokter tidak ditemukan"))
        val relevantQueue = _dailyQueuesFlow.value.filter {
            it.doctorId == doctorId && it.status != QueueStatus.SELESAI && it.status != QueueStatus.DIBATALKAN
        }

        val currentlyServing = relevantQueue.find { it.status == QueueStatus.DILAYANI }
        if (currentlyServing != null) return Result.failure(Exception("Masih ada pasien yang dilayani."))

        val nextPatient = relevantQueue.firstOrNull { it.status == QueueStatus.MENUNGGU }

        if (nextPatient == null) return Result.failure(Exception("Tidak ada antrian berikutnya."))

        NotificationRepository.addNotification(
            message = "Nomor antrian ${nextPatient.queueNumber} telah dipanggil. Segera menuju ruang periksa.",
            targetUserId = nextPatient.userId
        )


        _dailyQueuesFlow.update { list ->
            list.map {
                if (it.queueNumber == nextPatient.queueNumber && it.doctorId == doctorId) {
                    it.copy(status = QueueStatus.DIPANGGIL, calledAt = Date())
                } else {
                    it
                }
            }
        }
        return Result.success(Unit)
    }

    override suspend fun getWeeklyReport(): List<DailyReport> {
        delay(300)
        return DummyReportDatabase.weeklyReport
    }

    override suspend fun confirmPatientArrival(queueId: Int, doctorId: String): Result<Unit> {
        delay(500)
        val currentStatus = _practiceStatusFlow.value[doctorId] ?: return Result.failure(Exception("Dokter tidak ditemukan"))

        _dailyQueuesFlow.update { list ->
            list.map {
                if (it.queueNumber == queueId && it.doctorId == doctorId) {
                    it.copy(status = QueueStatus.DILAYANI, startedAt = Date())
                } else {
                    it
                }
            }
        }
        _practiceStatusFlow.update {
            it.toMutableMap().apply {
                this[doctorId] = currentStatus.copy(currentServingNumber = queueId)
            }
        }
        return Result.success(Unit)
    }

    override suspend fun finishConsultation(queueId: Int, doctorId: String): Result<Unit> {
        delay(500)
        val currentStatus = _practiceStatusFlow.value[doctorId] ?: return Result.failure(Exception("Dokter tidak ditemukan"))

        _dailyQueuesFlow.update { list ->
            list.map {
                if (it.queueNumber == queueId && it.doctorId == doctorId) {
                    it.copy(status = QueueStatus.SELESAI, finishedAt = Date())
                } else {
                    it
                }
            }
        }
        _practiceStatusFlow.update {
            it.toMutableMap().apply {
                this[doctorId] = currentStatus.copy(totalServed = currentStatus.totalServed + 1)
            }
        }
        return Result.success(Unit)
    }

    override suspend fun setPracticeOpen(doctorId: String, isOpen: Boolean): Result<Unit> {
        val currentStatus = _practiceStatusFlow.value[doctorId] ?: return Result.failure(Exception("Dokter tidak ditemukan"))
        _practiceStatusFlow.update {
            it.toMutableMap().apply { this[doctorId] = currentStatus.copy(isPracticeOpen = isOpen) }
        }
        return Result.success(Unit)
    }

    override suspend fun getVisitHistory(userId: String): List<HistoryItem> {
        delay(300)
        val dateFormat = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("id", "ID"))

        return _dailyQueuesFlow.value
            .filter { it.userId == userId && it.status == QueueStatus.SELESAI }
            .map { queueItem ->
                HistoryItem(
                    visitId = queueItem.queueNumber.toString(),
                    doctorName = doctorList[queueItem.doctorId] ?: "Dokter",
                    visitDate = dateFormat.format(queueItem.finishedAt ?: queueItem.createdAt),
                    initialComplaint = queueItem.keluhan
                )
            }
            .sortedByDescending { it.visitDate }
    }

    override suspend fun addManualQueue(patientName: String, complaint: String): Result<QueueItem> {
        delay(500)
        val doctorId = "doc_123"
        val currentStatus = _practiceStatusFlow.value[doctorId] ?: return Result.failure(Exception("Dokter tidak ditemukan"))

        // Cek batasan kuota
        if (currentStatus.lastQueueNumber >= currentStatus.dailyPatientLimit) {
            return Result.failure(Exception("Antrian penuh untuk hari ini."))
        }

        val newQueueNumber = currentStatus.lastQueueNumber + 1
        val manualUserId = "manual_${System.currentTimeMillis()}"

        val newQueueItem = QueueItem(
            queueNumber = newQueueNumber,
            userId = manualUserId,
            userName = "$patientName (Manual)", // Tandai sebagai manual
            doctorId = doctorId,
            keluhan = complaint.ifBlank { "Tidak ada keluhan awal" }
        )

        _dailyQueuesFlow.update { it + newQueueItem }
        _practiceStatusFlow.update {
            it.toMutableMap().apply { this[doctorId] = currentStatus.copy(lastQueueNumber = newQueueNumber) }
        }
        return Result.success(newQueueItem)
    }

    override suspend fun resetQueue() {
        delay(500)
        _dailyQueuesFlow.value = emptyList()
        _practiceStatusFlow.update { statusMap ->
            statusMap.mapValues { (doctorId, status) ->
                status.copy(
                    currentServingNumber = 0,
                    lastQueueNumber = 0,
                    totalServed = 0,
                    isPracticeOpen = true
                )
            }
        }
    }

    override suspend fun getDoctorSchedule(doctorId: String): List<DailyScheduleData> {
        delay(300)
        return DummyScheduleDatabase.weeklySchedules[doctorId] ?: emptyList()
    }

    override suspend fun updateDoctorSchedule(doctorId: String, newSchedule: List<DailyScheduleData>): Result<Unit> {
        delay(500)
        DummyScheduleDatabase.weeklySchedules[doctorId] = newSchedule.toMutableList()
        return Result.success(Unit)
    }


}