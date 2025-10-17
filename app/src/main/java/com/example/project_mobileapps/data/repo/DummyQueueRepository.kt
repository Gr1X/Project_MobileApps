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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

object DummyQueueRepository : QueueRepository {

    private val doctorList = mapOf("doc_123" to "Dr. Budi Santoso")
    private val FIFTEEN_MINUTES_IN_MS = 15 * 60 * 1000 // Koreksi: 15 menit

    // --- PINDAHKAN FUNGSI INI KE ATAS ---
    private fun isCurrentlyOpen(doctorId: String): Boolean {
        val schedules = DummyScheduleDatabase.weeklySchedules[doctorId] ?: return false
        val calendar = Calendar.getInstance()
        val dayOfWeekInt = calendar.get(Calendar.DAY_OF_WEEK) // Minggu = 1, Sabtu = 7
        val dayMapping = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
        val currentDayString = dayMapping[dayOfWeekInt - 1]

        val todaySchedule = schedules.find { it.dayOfWeek.equals(currentDayString, ignoreCase = true) } ?: return false

        if (!todaySchedule.isOpen) {
            return false
        }

        try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val now = timeFormat.parse(timeFormat.format(Date()))!!
            val start = timeFormat.parse(todaySchedule.startTime)!!
            val end = timeFormat.parse(todaySchedule.endTime)!!

            return now.after(start) && now.before(end)
        } catch (e: Exception) {
            return false
        }
    }
    // ------------------------------------

    private val _practiceStatusFlow = MutableStateFlow(
        mapOf("doc_123" to PracticeStatus(
            doctorId = "doc_123",
            doctorName = "Dr. Budi Santoso",
            currentServingNumber = 4,
            lastQueueNumber = 8,
            isPracticeOpen = isCurrentlyOpen("doc_123"), // Sekarang panggilan ini valid
            totalServed = 3
            // Anda belum menambahkan properti lain di sini, ini akan menyebabkan error
            // Tambahkan properti yang hilang sesuai definisi PracticeStatus
            , dailyPatientLimit = 50,
            estimatedServiceTimeInMinutes = 30,
            openingHour = 9,
            closingHour = 17
        ))
    )

    private fun getTodaySchedule(doctorId: String): DailyScheduleData? {
        val schedules = DummyScheduleDatabase.weeklySchedules[doctorId] ?: return null
        val calendar = Calendar.getInstance()
        val dayOfWeekInt = calendar.get(Calendar.DAY_OF_WEEK)
        val dayMapping = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
        val currentDayString = dayMapping[dayOfWeekInt - 1]
        return schedules.find { it.dayOfWeek.equals(currentDayString, ignoreCase = true) }
    }

    override val practiceStatusFlow: StateFlow<Map<String, PracticeStatus>> = _practiceStatusFlow.asStateFlow()

    private val _dailyQueuesFlow = MutableStateFlow(
        listOf(
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
        delay(500) // Simulasi jeda jaringan
        val currentStatus = _practiceStatusFlow.value[doctorId] ?: return Result.failure(Exception("Dokter tidak ditemukan"))
        val currentQueue = _dailyQueuesFlow.value

        // 1. Pengecekan dasar: Apakah praktik buka dan kuota harian masih ada
        if (!currentStatus.isPracticeOpen) return Result.failure(Exception("Pendaftaran sudah ditutup."))
        if (currentStatus.lastQueueNumber >= currentStatus.dailyPatientLimit) return Result.failure(Exception("Antrian penuh untuk hari ini."))

        // --- LOGIKA BARU: PENGECEKAN SISA WAKTU PRAKTIK ---
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val todaySchedule = getTodaySchedule(doctorId) ?: return Result.failure(Exception("Jadwal hari ini tidak ditemukan."))
        val closingTimeParts = todaySchedule.endTime.split(":")
        val closingHour = closingTimeParts[0].toInt()
        val closingMinute = closingTimeParts[1].toInt()

        val totalMinutesRemaining = (closingHour * 60 + closingMinute) - ((currentHour * 60) + currentMinute)

        if (totalMinutesRemaining <= 0) {
            return Result.failure(Exception("Waktu praktik sudah habis."))
        }

        val waitingPatientsCount = currentQueue.count {
            it.status == QueueStatus.MENUNGGU || it.status == QueueStatus.DIPANGGIL
        }
        val estimatedTimeForBacklog = waitingPatientsCount * currentStatus.estimatedServiceTimeInMinutes

        // Cek apakah sisa waktu cukup untuk antrian yang ada + 1 pasien baru
        if ((estimatedTimeForBacklog + currentStatus.estimatedServiceTimeInMinutes) > totalMinutesRemaining) {
            return Result.failure(Exception("Pendaftaran ditutup karena waktu praktik tidak mencukupi untuk antrian saat ini."))
        }
        // -----------------------------------------------------

        val existingQueue = currentQueue.find { it.userId == userId && it.status != QueueStatus.DIBATALKAN && it.status != QueueStatus.SELESAI }
        if (existingQueue != null) {
            return Result.failure(Exception("Anda sudah terdaftar dalam antrian."))
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
        var finishedItem: QueueItem? = null

        _dailyQueuesFlow.update { list ->
            list.map {
                if (it.queueNumber == queueId && it.doctorId == doctorId) {
                    finishedItem = it.copy(status = QueueStatus.SELESAI, finishedAt = Date())
                    finishedItem!!
                } else {
                    it
                }
            }
        }
        _practiceStatusFlow.update {
            it.toMutableMap().apply {
                this[doctorId] = currentStatus.copy(
                    totalServed = currentStatus.totalServed + 1,
                    currentServingNumber = 0
                )
            }
        }

        finishedItem?.let {
            val historyList = DummyHistoryDatabase.history.toMutableList()
            historyList.add(
                HistoryItem(
                    visitId = "hist_${System.currentTimeMillis()}",
                    userId = it.userId,
                    doctorName = doctorList[it.doctorId] ?: "Dokter",
                    visitDate = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(it.createdAt),
                    initialComplaint = it.keluhan,
                    status = QueueStatus.SELESAI
                )
            )
            // Anda tidak bisa meng-assign kembali ke val, tapi Anda bisa clear dan addAll jika 'history' adalah MutableList
            // Asumsi DummyHistoryDatabase.history adalah mutableList
            (DummyHistoryDatabase.history as MutableList).clear()
            (DummyHistoryDatabase.history as MutableList).addAll(historyList)
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
        return DummyHistoryDatabase.history.filter { it.userId == userId }
    }

    override suspend fun addManualQueue(patientName: String, complaint: String): Result<QueueItem> {
        delay(500)
        val doctorId = "doc_123"
        val currentStatus = _practiceStatusFlow.value[doctorId] ?: return Result.failure(Exception("Dokter tidak ditemukan"))

        if (currentStatus.lastQueueNumber >= currentStatus.dailyPatientLimit) {
            return Result.failure(Exception("Antrian penuh untuk hari ini."))
        }

        val newQueueNumber = currentStatus.lastQueueNumber + 1
        val manualUserId = "manual_${System.currentTimeMillis()}"

        val newQueueItem = QueueItem(
            queueNumber = newQueueNumber,
            userId = manualUserId,
            userName = "$patientName (Manual)",
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
                    isPracticeOpen = isCurrentlyOpen(doctorId)
                )
            }
        }
    }

    override suspend fun getDoctorSchedule(doctorId: String): List<DailyScheduleData> {
        delay(300)
        return DummyScheduleDatabase.weeklySchedules[doctorId]?.toList() ?: emptyList()
    }

    override suspend fun updateDoctorSchedule(doctorId: String, newSchedule: List<DailyScheduleData>): Result<Unit> {
        delay(500)
        DummyScheduleDatabase.weeklySchedules[doctorId] = newSchedule.toMutableList()
        _practiceStatusFlow.update {
            it.toMutableMap().apply {
                val current = this[doctorId]
                if (current != null) {
                    this[doctorId] = current.copy(isPracticeOpen = isCurrentlyOpen(doctorId))
                }
            }
        }
        return Result.success(Unit)
    }
}