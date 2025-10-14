package com.example.project_mobileapps.data.repo

import com.example.project_mobileapps.data.local.DummyHistoryDatabase
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
    private val FIFTEEN_MINUTES_IN_MS = 60 * 1000
    private val _practiceStatusFlow = MutableStateFlow(
        doctorList.mapValues { PracticeStatus(doctorId = it.key, doctorName = it.value, isPracticeOpen = true) }
    )
    override val practiceStatusFlow: StateFlow<Map<String, PracticeStatus>> = _practiceStatusFlow.asStateFlow()
    private val _dailyQueuesFlow = MutableStateFlow<List<QueueItem>>(emptyList())
    override val dailyQueuesFlow: StateFlow<List<QueueItem>> = _dailyQueuesFlow.asStateFlow()


    override suspend fun checkForLatePatients(doctorId: String) {
        val now = Date().time
        _dailyQueuesFlow.update { currentList ->
            // 1. Pisahkan daftar menjadi dua: yang terlambat dan yang tepat waktu
            val (latePatients, onTimePatients) = currentList.partition {
                it.status == QueueStatus.DIPANGGIL &&
                        it.calledAt != null &&
                        (now - it.calledAt!!.time > FIFTEEN_MINUTES_IN_MS)
            }

            // 2. Jika ada pasien yang terlambat...
            if (latePatients.isNotEmpty()) {
                // Ubah status mereka kembali ke MENUNGGU
                val updatedLatePatients = latePatients.map {
                    it.copy(status = QueueStatus.MENUNGGU, calledAt = null)
                }
                // 3. Gabungkan kembali: yang tepat waktu di depan, yang terlambat di belakang
                onTimePatients + updatedLatePatients
            } else {
                // Jika tidak ada yang terlambat, jangan lakukan apa-apa
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

        // =======================================================
        // PERBAIKI LOGIKA PEMILIHAN PASIEN DI SINI
        // =======================================================
        // Cara lama (salah): .minByOrNull { it.queueNumber }
        // Cara baru (benar): ambil item pertama yang statusnya MENUNGGU dari daftar yang sudah diurutkan
        val nextPatient = relevantQueue.firstOrNull { it.status == QueueStatus.MENUNGGU }
        // =======================================================

        if (nextPatient == null) return Result.failure(Exception("Tidak ada antrian berikutnya."))

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
                    it.copy(status = QueueStatus.SELESAI)
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
        delay(500)
        return DummyHistoryDatabase.history
    }

    // TAMBAHKAN IMPLEMENTASI FUNGSI BARU INI
    override suspend fun addManualQueue(patientName: String, complaint: String): Result<QueueItem> {
        delay(500)
        val doctorId = "doc_123"
        val currentStatus = _practiceStatusFlow.value[doctorId] ?: return Result.failure(Exception("Dokter tidak ditemukan"))

        // Cek batasan kuota
        if (currentStatus.lastQueueNumber >= currentStatus.dailyPatientLimit) {
            return Result.failure(Exception("Antrian penuh untuk hari ini."))
        }

        val newQueueNumber = currentStatus.lastQueueNumber + 1
        // Buat user ID palsu untuk pasien manual
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
}