// File: data/repo/FirestoreQueueRepository.kt
package com.example.project_mobileapps.data.repo

import android.util.Log
import com.example.project_mobileapps.data.model.DailyReport
import com.example.project_mobileapps.data.model.DailyScheduleData
import com.example.project_mobileapps.data.model.HistoryItem
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.di.AppContainer
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object FirestoreQueueRepository : QueueRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val doctorId = AppContainer.CLINIC_ID
    private val practiceStatusDoc = firestore.collection("practice_status").document(doctorId)
    private val scheduleDoc = firestore.collection("schedules").document(doctorId)
    private val queuesCollection = firestore.collection("queues")

    private val repositoryScope = CoroutineScope(Dispatchers.Default)

    // =================================================================
    // 1. DATA FLOWS (REAL-TIME READ)
    // =================================================================

    @OptIn(ExperimentalCoroutinesApi::class)
    override val practiceStatusFlow: StateFlow<Map<String, PracticeStatus>> = callbackFlow<Map<String, PracticeStatus>> {
        val listener = practiceStatusDoc.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreRepo", "Error Practice Status", error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                try {
                    val status = snapshot.toObject(PracticeStatus::class.java)
                    if (status != null) {
                        trySend(mapOf(doctorId to status))
                    }
                } catch (e: Exception) {
                    Log.e("FirestoreRepo", "Error parsing status", e)
                }
            } else {
                trySend(emptyMap<String, PracticeStatus>())
            }
            Unit
        }
        awaitClose { listener.remove() }
    }.stateIn(repositoryScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    override val dailyQueuesFlow: StateFlow<List<QueueItem>> = callbackFlow<List<QueueItem>> {
        val (startOfDay, endOfDay) = getTodayRange()

        val query = queuesCollection
            .whereEqualTo("doctorId", doctorId)
            .whereGreaterThanOrEqualTo("createdAt", startOfDay)
            .whereLessThanOrEqualTo("createdAt", endOfDay)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val queues = snapshot.toObjects(QueueItem::class.java)

                // LOGIKA SORTING BARU:
                val sortedList = queues.sortedWith(
                    compareBy<QueueItem> {
                        // 1. Urutkan Status: DILAYANI > DIPANGGIL > MENUNGGU
                        when (it.status) {
                            QueueStatus.DILAYANI -> 1
                            QueueStatus.DIPANGGIL -> 2
                            QueueStatus.MENUNGGU -> 3
                            else -> 4
                        }
                    }.thenBy {
                        // 2. Penalty: Jika MENUNGGU & Pernah Telat -> Lempar ke Belakang (Value 1)
                        if (it.status == QueueStatus.MENUNGGU && it.hasBeenLate) 1 else 0
                    }.thenBy {
                        // 3. Sisanya urut nomor antrian biasa
                        it.queueNumber
                    }
                )
                trySend(sortedList)
            } else {
                trySend(emptyList<QueueItem>())
            }
            Unit
        }
        awaitClose { listener.remove() }
    }.stateIn(repositoryScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // =================================================================
    // 2. USER ACTIONS (WRITE)
    // =================================================================

    override suspend fun takeQueueNumber(doctorId: String, userId: String, userName: String, keluhan: String): Result<QueueItem> {
        return try {
            val (startOfDay, endOfDay) = getTodayRange()

            // 1. Cek Jadwal Praktik (Validasi Waktu)
            val scheduleSnapshot = scheduleDoc.get().await()
            val rawList = scheduleSnapshot.get("dailySchedules") as? List<*> ?: emptyList<Any>()
            val scheduleList = parseScheduleList(rawList)

            if (!isCurrentlyOpen(scheduleList)) {
                return Result.failure(Exception("Pendaftaran gagal: Klinik sedang tutup (Di luar jam praktik)."))
            }

            // 2. Cek Duplikasi Harian
            val existingQuery = queuesCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("doctorId", doctorId)
                .whereGreaterThanOrEqualTo("createdAt", startOfDay)
                .whereLessThanOrEqualTo("createdAt", endOfDay)
                .whereIn("status", listOf("MENUNGGU", "DIPANGGIL", "DILAYANI"))
                .get().await()

            if (!existingQuery.isEmpty) {
                return Result.failure(Exception("Anda sudah terdaftar dalam antrian hari ini."))
            }

            // 3. LOGIKA Hitung Nomor Antrian Baru (Reset Harian)
            val lastQueueSnapshot = queuesCollection
                .whereEqualTo("doctorId", doctorId)
                .whereGreaterThanOrEqualTo("createdAt", startOfDay)
                .whereLessThanOrEqualTo("createdAt", endOfDay)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get().await()

            val nextQueueNumber = if (lastQueueSnapshot.isEmpty) {
                1
            } else {
                val lastItem = lastQueueSnapshot.documents[0].toObject(QueueItem::class.java)
                (lastItem?.queueNumber ?: 0) + 1
            }

            // 4. Simpan Data
            val newQueueItem = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(practiceStatusDoc)
                val status = snapshot.toObject(PracticeStatus::class.java)
                    ?: throw Exception("Data praktik tidak ditemukan.")

                if (!status.isPracticeOpen) throw Exception("Pendaftaran sedang ditutup oleh Admin.")
                if (nextQueueNumber > status.dailyPatientLimit) throw Exception("Kuota antrian hari ini sudah penuh.")

                val newQueueRef = queuesCollection.document()
                val newItem = QueueItem(
                    id = newQueueRef.id,
                    queueNumber = nextQueueNumber,
                    userId = userId,
                    userName = userName,
                    doctorId = doctorId,
                    keluhan = keluhan,
                    status = QueueStatus.MENUNGGU,
                    createdAt = Date()
                )

                transaction.update(practiceStatusDoc, "lastQueueNumber", nextQueueNumber)
                transaction.set(newQueueRef, newItem)

                newItem
            }.await()

            Result.success(newQueueItem)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Gagal ambil antrian", e)
            Result.failure(e)
        }
    }

    override suspend fun cancelQueue(userId: String, doctorId: String): Result<Unit> {
        return try {
            val (startOfDay, endOfDay) = getTodayRange()
            val snapshot = queuesCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("doctorId", doctorId)
                .whereGreaterThanOrEqualTo("createdAt", startOfDay)
                .whereLessThanOrEqualTo("createdAt", endOfDay)
                .whereIn("status", listOf("MENUNGGU", "DIPANGGIL"))
                .get().await()

            if (snapshot.documents.isNotEmpty()) {
                val docRef = snapshot.documents[0].reference
                docRef.update("status", QueueStatus.DIBATALKAN.name).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Antrian aktif tidak ditemukan."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // =================================================================
    // 3. ADMIN/DOCTOR ACTIONS (SCAN & MANAGE)
    // =================================================================

    override suspend fun confirmArrivalByQr(queueId: String): Result<Unit> {
        return try {
            val docRef = queuesCollection.document(queueId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) throw Exception("QR Code tidak valid.")

                val currentStatus = snapshot.getString("status")
                val queueNumber = snapshot.getLong("queueNumber")?.toInt() ?: 0

                if (currentStatus == "SELESAI" || currentStatus == "DIBATALKAN" || currentStatus == "DILAYANI")
                    throw Exception("Status pasien tidak valid untuk konfirmasi.")

                transaction.update(docRef, "status", QueueStatus.DILAYANI.name)
                transaction.update(docRef, "startedAt", Date())
                if (snapshot.getDate("calledAt") == null) {
                    transaction.update(docRef, "calledAt", Date())
                }
                transaction.update(practiceStatusDoc, "currentServingNumber", queueNumber)

            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun callNextPatient(doctorId: String): Result<Unit> {
        return try {
            // 1. Cek & Tindak Pasien Telat TERLEBIH DAHULU (Synchronous)
            // Ini memastikan data bersih sebelum memanggil orang baru
            checkForLatePatients(doctorId)

            val (startOfDay, endOfDay) = getTodayRange()

            // 2. Ambil SEMUA pasien yang statusnya MENUNGGU
            // Kita tidak pakai .limit(1) di sini, karena kita butuh sorting custom
            val snapshot = queuesCollection
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("status", QueueStatus.MENUNGGU.name)
                .whereGreaterThanOrEqualTo("createdAt", startOfDay)
                .whereLessThanOrEqualTo("createdAt", endOfDay)
                .get().await()

            val candidates = snapshot.toObjects(QueueItem::class.java)

            if (candidates.isNotEmpty()) {
                // 3. SORTING PRIORITAS (THE MAGIC FIX 🪄 )
                // Urutan:
                // A. Yang 'hasBeenLate' = false (Belum pernah telat) DULUAN
                // B. Baru urutkan berdasarkan Nomor Antrian
                val nextPatient = candidates.sortedWith(
                    compareBy<QueueItem> { it.hasBeenLate } // false (0) naik, true (1) turun
                        .thenBy { it.queueNumber }          // urut nomor kecil ke besar
                ).firstOrNull()

                if (nextPatient != null) {
                    val docRef = queuesCollection.document(nextPatient.id)

                    firestore.runTransaction { transaction ->
                        // Double check status di dalam transaksi agar tidak bentrok
                        val freshSnapshot = transaction.get(docRef)
                        if (freshSnapshot.getString("status") == QueueStatus.MENUNGGU.name) {
                            transaction.update(docRef, "status", QueueStatus.DIPANGGIL.name)
                            transaction.update(docRef, "calledAt", Date()) // Set waktu panggil SEKARANG
                        }
                    }.await()

                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Tidak ada antrian yang valid."))
                }
            } else {
                Result.failure(Exception("Tidak ada antrian menunggu."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun confirmPatientArrival(queueNumber: Int, doctorId: String): Result<Unit> {
        return try {
            val (startOfDay, endOfDay) = getTodayRange()
            val snapshot = queuesCollection
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("queueNumber", queueNumber)
                .whereGreaterThanOrEqualTo("createdAt", startOfDay)
                .whereLessThanOrEqualTo("createdAt", endOfDay)
                .limit(1)
                .get().await()

            if (!snapshot.isEmpty) {
                confirmArrivalByQr(snapshot.documents[0].id)
            } else {
                Result.failure(Exception("Antrian tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun finishConsultation(
        queueNumber: Int, doctorId: String, diagnosis: String, treatment: String, prescription: String, notes: String
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun submitMedicalRecord(queueId: String, medicalData: Map<String, Any>): Result<Unit> {
        return try {
            val docRef = queuesCollection.document(queueId)

            firestore.runTransaction { transaction ->
                // [PERBAIKAN] 1. BACA DULU (READ)
                // Pindahkan ini ke paling atas sebelum melakukan update apa pun
                val practiceSnapshot = transaction.get(practiceStatusDoc)
                val totalServed = practiceSnapshot.getLong("totalServed") ?: 0

                // [PERBAIKAN] 2. BARU LAKUKAN TULIS (WRITE)
                val updates = medicalData.toMutableMap()
                updates["status"] = QueueStatus.SELESAI.name
                updates["finishedAt"] = Date()

                // Update dokumen antrian
                transaction.update(docRef, updates)

                // Update status praktik
                transaction.update(practiceStatusDoc, "totalServed", totalServed + 1)

                // Opsional: Cek dulu apakah currentServing memang nomor ini sebelum di-nol-kan,
                // tapi untuk sekarang di-nol-kan langsung aman.
                transaction.update(practiceStatusDoc, "currentServingNumber", 0)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================================================================
    // 4. ADMIN FEATURES (SCHEDULE & SETTINGS)
    // =================================================================

    override suspend fun getDoctorSchedule(doctorId: String): List<DailyScheduleData> {
        if (doctorId != AppContainer.CLINIC_ID) return emptyList()
        return try {
            val snapshot = scheduleDoc.get().await()
            val rawList = snapshot.get("dailySchedules") as? List<*> ?: return emptyList()
            parseScheduleList(rawList)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseScheduleList(rawList: List<*>): List<DailyScheduleData> {
        return rawList.mapNotNull { item ->
            val map = item as? Map<String, Any> ?: return@mapNotNull null
            val isOpenVal = map["isOpen"] ?: map["open"]
            val isOpenBool = when(isOpenVal) {
                is Boolean -> isOpenVal
                is String -> isOpenVal.toBoolean()
                else -> false
            }
            DailyScheduleData(
                dayOfWeek = (map["dayOfWeek"] as? String)?.trim() ?: "N/A",
                isOpen = isOpenBool,
                startTime = map["startTime"] as? String ?: "00:00",
                endTime = map["endTime"] as? String ?: "00:00"
            )
        }
    }

    override suspend fun updateDoctorSchedule(doctorId: String, newSchedule: List<DailyScheduleData>): Result<Unit> {
        return try {
            scheduleDoc.update("dailySchedules", newSchedule).await()
            // Setelah update jadwal, paksa cek ulang agar status langsung berubah jika perlu
            runAutoScheduleCheck()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun setPracticeOpen(doctorId: String, isOpen: Boolean): Result<Unit> {
        return try {
            practiceStatusDoc.update("isPracticeOpen", isOpen).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateEstimatedServiceTime(doctorId: String, minutes: Int): Result<Unit> {
        return try {
            practiceStatusDoc.update("estimatedServiceTimeInMinutes", minutes).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updatePatientCallTimeLimit(doctorId: String, minutes: Int): Result<Unit> {
        return try {
            practiceStatusDoc.update("patientCallTimeLimitMinutes", minutes).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun addManualQueue(patientName: String, complaint: String): Result<QueueItem> {
        return try {
            val newItem = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(practiceStatusDoc)
                val status = snapshot.toObject(PracticeStatus::class.java) ?: throw Exception("Error")

                val newNumber = status.lastQueueNumber + 1
                val newQueueRef = queuesCollection.document()
                val newItem = QueueItem(
                    id = newQueueRef.id,
                    queueNumber = newNumber,
                    userId = "manual_${System.currentTimeMillis()}",
                    userName = "$patientName (Manual)",
                    doctorId = doctorId,
                    keluhan = complaint,
                    status = QueueStatus.MENUNGGU,
                    createdAt = Date()
                )
                transaction.update(practiceStatusDoc, "lastQueueNumber", newNumber)
                transaction.set(newQueueRef, newItem)
                newItem
            }.await()
            Result.success(newItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================================================================
    // 5. HELPER / AUTOMATION (JANTUNG SISTEM OTOMATIS)
    // =================================================================

    // Fungsi Utama: Cek Jam -> Buka/Tutup/Batal Otomatis
    private suspend fun runAutoScheduleCheck() {
        try {
            val schedule = getDoctorSchedule(doctorId)
            if (schedule.isEmpty()) return

            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
            val dayMapping = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
            val currentDay = dayMapping[calendar.get(Calendar.DAY_OF_WEEK) - 1]
            val nowMinutes = (calendar.get(Calendar.HOUR_OF_DAY) * 60) + calendar.get(Calendar.MINUTE)

            val todaySchedule = schedule.find { it.dayOfWeek.trim().equals(currentDay, ignoreCase = true) }
            val updates = mutableMapOf<String, Any>()

            val snapshot = practiceStatusDoc.get().await()
            val currentIsOpenInDb = snapshot.getBoolean("isPracticeOpen") ?: false

            if (todaySchedule != null && todaySchedule.isOpen) {
                val (sh, sm) = todaySchedule.startTime.split(":").map { it.toInt() }
                val (eh, em) = todaySchedule.endTime.split(":").map { it.toInt() }
                val startMins = sh * 60 + sm
                val endMins = eh * 60 + em

                // Selalu update info jam agar UI sinkron
                updates["startTime"] = todaySchedule.startTime
                updates["endTime"] = todaySchedule.endTime

                if (nowMinutes in startMins..endMins) {
                    // MASUK JAM PRAKTIK: Buka Otomatis jika masih tutup
                    if (!currentIsOpenInDb) {
                        updates["isPracticeOpen"] = true
                    }
                } else {
                    // LEWAT JAM PRAKTIK: Tutup Otomatis jika masih buka
                    if (currentIsOpenInDb) {
                        updates["isPracticeOpen"] = false
                        // BATALKAN SISA ANTRIAN KARENA SUDAH TUTUP
                        cancelRemainingQueues(doctorId)
                    }
                }
            } else {
                // HARI LIBUR: Pastikan Tutup
                if (currentIsOpenInDb) {
                    updates["isPracticeOpen"] = false
                    cancelRemainingQueues(doctorId)
                }
            }

            if (updates.isNotEmpty()) {
                practiceStatusDoc.update(updates).await()
            }
        } catch (e: Exception) { Log.e("Repo", "Auto Schedule Error", e) }
    }

    private suspend fun cancelRemainingQueues(doctorId: String) {
        try {
            val (start, end) = getTodayRange()
            val activeQueues = queuesCollection
                .whereEqualTo("doctorId", doctorId)
                .whereGreaterThanOrEqualTo("createdAt", start)
                .whereLessThanOrEqualTo("createdAt", end)
                .whereIn("status", listOf("MENUNGGU", "DIPANGGIL"))
                .get().await()

            if (!activeQueues.isEmpty) {
                val batch = firestore.batch()
                activeQueues.documents.forEach {
                    batch.update(it.reference, mapOf(
                        "status" to QueueStatus.DIBATALKAN.name,
                        "doctorNotes" to "Sistem: Dibatalkan otomatis (Klinik Tutup)"
                    ))
                }
                batch.commit().await()
                Log.d("FirestoreRepo", "🚫 Auto-Cancel ${activeQueues.size()} antrian sisa.")
            }
        } catch (e: Exception) { Log.e("FirestoreRepo", "Cancel error", e) }
    }

    // Override yang sempat hilang (FIX ERROR)
    override suspend fun resetQueue() {
        // Implementasi kosong karena reset harian sudah dihandle di takeQueueNumber
    }

    override suspend fun getVisitHistory(userId: String): List<HistoryItem> {
        return try {
            val snapshot = queuesCollection
                .whereEqualTo("userId", userId)
                .whereIn("status", listOf("SELESAI", "DIBATALKAN"))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()

            snapshot.toObjects(QueueItem::class.java).map { queue ->
                HistoryItem(
                    visitId = queue.id,
                    userId = queue.userId,
                    doctorName = "Dr. Budi Santoso",
                    visitDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(queue.createdAt),
                    initialComplaint = queue.keluhan,
                    status = queue.status,
                    diagnosis = queue.diagnosis,
                    treatment = queue.treatment,
                    prescription = queue.prescription,
                    doctorNotes = queue.doctorNotes
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun getWeeklyReport(): List<DailyReport> {
        return try {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
            calendar.add(Calendar.DAY_OF_YEAR, -6)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            val startOfPeriod = calendar.time

            val snapshot = queuesCollection
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("status", "SELESAI")
                .whereGreaterThanOrEqualTo("createdAt", startOfPeriod)
                .get().await()

            val historyList = snapshot.toObjects(QueueItem::class.java)
            val reportMap = linkedMapOf<String, Int>()
            val dayFormat = SimpleDateFormat("EEE", Locale("id", "ID"))

            val tempCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
            tempCal.time = startOfPeriod
            for (i in 0..6) {
                reportMap[dayFormat.format(tempCal.time)] = 0
                tempCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            historyList.forEach { item ->
                val dayLabel = dayFormat.format(item.createdAt)
                if (reportMap.containsKey(dayLabel)) {
                    reportMap[dayLabel] = reportMap[dayLabel]!! + 1
                }
            }
            reportMap.map { (day, count) -> DailyReport(day, count) }
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun getQueuesByDateRange(start: Date, end: Date): List<QueueItem> {
        return try {
            val snapshot = queuesCollection
                .whereEqualTo("doctorId", doctorId)
                .whereGreaterThanOrEqualTo("createdAt", start)
                .whereLessThanOrEqualTo("createdAt", end)
                .get().await()
            snapshot.toObjects(QueueItem::class.java)
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun getQueueById(queueId: String): QueueItem? {
        return try {
            val snapshot = queuesCollection.document(queueId).get().await()
            snapshot.toObject(QueueItem::class.java)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error getQueueById", e)
            null
        }
    }

    private suspend fun cleanupOldQueues(doctorId: String) {
        try {
            val (startOfToday, _) = getTodayRange()

            val snapshot = queuesCollection
                .whereEqualTo("doctorId", doctorId)
                .whereLessThan("createdAt", startOfToday)
                .whereIn("status", listOf("MENUNGGU", "DIPANGGIL"))
                .get().await()

            if (!snapshot.isEmpty) {
                val batch = firestore.batch()
                snapshot.documents.forEach { doc ->
                    batch.update(doc.reference, mapOf(
                        "status" to QueueStatus.DIBATALKAN.name,
                        "doctorNotes" to "Sistem: Dibatalkan otomatis"
                    ))
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Gagal cleanup antrian lama", e)
        }
    }

    override suspend fun checkForLatePatients(doctorId: String) {
        try {
            val (start, end) = getTodayRange()
            val pStatSnapshot = practiceStatusDoc.get().await()
            val pStat = pStatSnapshot.toObject(PracticeStatus::class.java) ?: return

            val limitMinutes = if (pStat.patientCallTimeLimitMinutes > 0) pStat.patientCallTimeLimitMinutes else 15
            val limitMs = limitMinutes * 60 * 1000L // Konversi ke Milidetik
            val now = Date().time

            // Cari pasien yang sedang DIPANGGIL
            val snapshot = queuesCollection
                .whereEqualTo("doctorId", doctorId) // Pastikan filter dokter
                .whereEqualTo("status", QueueStatus.DIPANGGIL.name)
                .whereGreaterThanOrEqualTo("createdAt", start)
                .whereLessThanOrEqualTo("createdAt", end)
                .get().await()

            val batch = firestore.batch()
            var hasUpdates = false

            for (doc in snapshot.documents) {
                val calledAt = doc.getDate("calledAt")
                val isAlreadyLate = doc.getBoolean("hasBeenLate") ?: false

                // Jika calledAt null, anggap baru dipanggil sekarang (safety)
                val callTime = calledAt?.time ?: now

                // LOGIKA WAKTU HABIS
                if (now - callTime > limitMs) {
                    hasUpdates = true

                    if (isAlreadyLate) {
                        // KASUS 2: SUDAH PERNAH TELAT -> SEKARANG TELAT LAGI -> BATALKAN
                        // "Kesempatan kedua sudah habis"
                        batch.update(doc.reference, mapOf(
                            "status" to QueueStatus.DIBATALKAN.name,
                            "doctorNotes" to "Sistem: Dibatalkan otomatis (Tidak hadir setelah 2x pemanggilan)"
                        ))
                        Log.d("FirestoreRepo", "🚫 Pasien ${doc.id} dibatalkan (Telat 2x).")
                    } else {
                        // KASUS 1: BARU PERTAMA KALI TELAT -> LEMPAR KE BELAKANG
                        // Kembalikan ke MENUNGGU, tapi tandai hasBeenLate = true
                        // Reset calledAt jadi null agar tidak memicu timer di UI
                        batch.update(doc.reference, mapOf(
                            "status" to QueueStatus.MENUNGGU.name,
                            "hasBeenLate" to true,
                            "calledAt" to null
                        ))
                        Log.d("FirestoreRepo", "⚠️ Pasien ${doc.id} telat pertama. Pindah ke belakang.")
                    }
                }
            }

            // Commit semua perubahan sekaligus (Atomic)
            if (hasUpdates) {
                batch.commit().await()
            }

        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Check Late Error", e)
        }
    }

    private fun getTodayRange(): Pair<Date, Date> {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.time
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val end = calendar.time
        return Pair(start, end)
    }

    private fun isCurrentlyOpen(scheduleList: List<DailyScheduleData>): Boolean {
        if (scheduleList.isEmpty()) return false
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
        val dayMapping = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
        val currentDay = dayMapping[calendar.get(Calendar.DAY_OF_WEEK) - 1]
        val nowMinutes = (calendar.get(Calendar.HOUR_OF_DAY) * 60) + calendar.get(Calendar.MINUTE)

        val today = scheduleList.find { it.dayOfWeek.trim().equals(currentDay, ignoreCase = true) } ?: return false
        if (!today.isOpen) return false

        return try {
            val (sh, sm) = today.startTime.split(":").map { it.toInt() }
            val (eh, em) = today.endTime.split(":").map { it.toInt() }
            nowMinutes in (sh * 60 + sm)..(eh * 60 + em)
        } catch (e: Exception) { false }
    }

    // --- INIT: Start Automation ---
    init {
        repositoryScope.launch {
            cleanupOldQueues(doctorId) // Jalankan sekali saat app dibuka

            while (true) {
                // LOGIKA OTOMATIS AKTIF KEMBALI DI SINI
                runAutoScheduleCheck()
                checkForLatePatients(doctorId)
                delay(5_000L) // Cek setiap 30 detik
            }
        }
    }
}