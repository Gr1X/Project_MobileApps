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
    // 1. DATA FLOWS (REAL-TIME READ) - FIXED ERRORS HERE
    // =================================================================

    @OptIn(ExperimentalCoroutinesApi::class)
    override val practiceStatusFlow: StateFlow<Map<String, PracticeStatus>> = callbackFlow<Map<String, PracticeStatus>> {
        Log.d("DEBUG_FLOW", "Memulai Listener Practice Status...")

        val listener = practiceStatusDoc.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("DEBUG_FLOW", "❌ Firebase Error: ${error.message}", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                try {
                    val status = snapshot.toObject(PracticeStatus::class.java)
                    if (status != null) {
                        trySend(mapOf(doctorId to status))
                    }
                } catch (e: Exception) {
                    Log.e("DEBUG_FLOW", "❌ CRASH saat Konversi: ${e.message}", e)
                }
            } else {
                // FIX: Tambahkan tipe eksplisit <String, PracticeStatus>
                trySend(emptyMap<String, PracticeStatus>())
            }

            // FIX: Tambahkan Unit di akhir lambda agar return type cocok
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
                Log.e("DEBUG_QUEUE", "❌ ERROR LISTENER: ${error.message}", error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val queues = snapshot.toObjects(QueueItem::class.java)
                trySend(queues.sortedBy { it.queueNumber })
            } else {
                // FIX: Tambahkan tipe eksplisit <QueueItem>
                trySend(emptyList<QueueItem>())
            }

            // FIX: Tambahkan Unit di akhir lambda
            Unit
        }
        awaitClose { listener.remove() }
    }.stateIn(repositoryScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // =================================================================
    // 2. USER ACTIONS (WRITE)
    // =================================================================

    override suspend fun takeQueueNumber(doctorId: String, userId: String, userName: String, keluhan: String): Result<QueueItem> {
        if (doctorId != AppContainer.CLINIC_ID) return Result.failure(Exception("ID Klinik Salah"))

        return try {
            val (startOfDay, endOfDay) = getTodayRange()

            // 1. Cek Duplikasi
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

            // 2. LOGIKA RESET HARIAN
            val lastQueueSnapshot = queuesCollection
                .whereEqualTo("doctorId", doctorId)
                .whereGreaterThanOrEqualTo("createdAt", startOfDay)
                .whereLessThanOrEqualTo("createdAt", endOfDay)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val nextQueueNumber = if (lastQueueSnapshot.isEmpty) {
                1
            } else {
                val lastItem = lastQueueSnapshot.documents[0].toObject(QueueItem::class.java)
                (lastItem?.queueNumber ?: 0) + 1
            }

            // 3. Simpan
            val newQueueItem = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(practiceStatusDoc)
                val status = snapshot.toObject(PracticeStatus::class.java)
                    ?: throw Exception("Data praktik tidak ditemukan.")

                if (!status.isPracticeOpen) throw Exception("Pendaftaran antrian sedang ditutup.")
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

    override suspend fun confirmArrivalByQr(documentId: String): Result<Unit> {
        return try {
            val docRef = queuesCollection.document(documentId)

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
            checkForLatePatients(doctorId)

            val (startOfDay, endOfDay) = getTodayRange()
            val snapshot = queuesCollection
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("status", QueueStatus.MENUNGGU.name)
                .whereGreaterThanOrEqualTo("createdAt", startOfDay)
                .whereLessThanOrEqualTo("createdAt", endOfDay)
                .orderBy("queueNumber", Query.Direction.ASCENDING)
                .limit(1)
                .get().await()

            if (snapshot.documents.isNotEmpty()) {
                val nextPatientDoc = snapshot.documents[0]
                firestore.runTransaction { transaction ->
                    val freshSnapshot = transaction.get(nextPatientDoc.reference)
                    if (freshSnapshot.getString("status") == "MENUNGGU") {
                        transaction.update(nextPatientDoc.reference, "status", QueueStatus.DIPANGGIL.name)
                        transaction.update(nextPatientDoc.reference, "calledAt", Date())
                    }
                }.await()
                Result.success(Unit)
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
        return try {
            val (startOfDay, endOfDay) = getTodayRange()
            val snapshot = queuesCollection
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("queueNumber", queueNumber)
                .whereEqualTo("status", QueueStatus.DILAYANI.name)
                .whereGreaterThanOrEqualTo("createdAt", startOfDay)
                .whereLessThanOrEqualTo("createdAt", endOfDay)
                .get().await()

            if (snapshot.documents.isNotEmpty()) {
                val docRef = snapshot.documents[0].reference
                firestore.runTransaction { transaction ->
                    val practiceSnapshot = transaction.get(practiceStatusDoc)
                    val totalServed = practiceSnapshot.getLong("totalServed") ?: 0

                    transaction.update(docRef, mapOf(
                        "status" to QueueStatus.SELESAI.name,
                        "finishedAt" to Date(),
                        "diagnosis" to diagnosis,
                        "treatment" to treatment,
                        "prescription" to prescription,
                        "doctorNotes" to notes
                    ))
                    transaction.update(practiceStatusDoc, "totalServed", totalServed + 1)
                    transaction.update(practiceStatusDoc, "currentServingNumber", 0)
                }.await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Pasien tidak ditemukan / status salah."))
            }
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

            rawList.mapNotNull { item ->
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
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun updateDoctorSchedule(doctorId: String, newSchedule: List<DailyScheduleData>): Result<Unit> {
        return try {
            scheduleDoc.update("dailySchedules", newSchedule).await()
            checkAndUpdatePracticeStatus()
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
            val newQueueItem = firestore.runTransaction { transaction ->
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
            Result.success(newQueueItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================================================================
    // 5. HELPER / AUTOMATION
    // =================================================================

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
                    visitDate = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(queue.createdAt),
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

    override suspend fun resetQueue() {}

    // --- AUTOMATION FUNCTIONS ---

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
            val (startOfDay, endOfDay) = getTodayRange()
            val practiceStatus = practiceStatusFlow.value[doctorId] ?: return
            val limitMs = practiceStatus.patientCallTimeLimitMinutes * 60 * 1000L
            val now = Date().time

            val snapshot = queuesCollection
                .whereEqualTo("status", QueueStatus.DIPANGGIL.name)
                .whereGreaterThanOrEqualTo("createdAt", startOfDay)
                .whereLessThanOrEqualTo("createdAt", endOfDay)
                .get().await()

            for (doc in snapshot.documents) {
                val calledAt = doc.getDate("calledAt")
                val hasBeenLate = doc.getBoolean("hasBeenLate") ?: false
                if (calledAt != null && !hasBeenLate) {
                    if (now - calledAt.time > limitMs) {
                        doc.reference.update(mapOf("status" to QueueStatus.MENUNGGU.name, "hasBeenLate" to true, "calledAt" to null))
                    }
                }
            }
        } catch (e: Exception) { Log.e("FirestoreRepo", "Check late error", e) }
    }

    // --- TIME HELPERS ---

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

    private suspend fun checkAndUpdatePracticeStatus() {
        try {
            val schedule = getDoctorSchedule(doctorId)
            val shouldBeOpen = isCurrentlyOpen(schedule)
            val snapshot = practiceStatusDoc.get().await()
            val currentIsOpenInDb = snapshot.getBoolean("isPracticeOpen") ?: false

            if (currentIsOpenInDb != shouldBeOpen) {
                setPracticeOpen(doctorId, shouldBeOpen)
            }
        } catch (e: Exception) { Log.e("FirestoreRepo", "Gagal check status", e) }
    }

    // --- INIT: Start Automation ---
    init {
        repositoryScope.launch {
            cleanupOldQueues(doctorId) // Jalankan sekali saat app dibuka
            while (true) {
                checkAndUpdatePracticeStatus()
                checkForLatePatients(doctorId)
                delay(30_000L)
            }
        }
    }
}