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
    // Pastikan ID ini SAMA PERSIS dengan dokumen di Firestore (Collection: practice_status)
    private val doctorId = AppContainer.CLINIC_ID

    private val practiceStatusDoc = firestore.collection("practice_status").document(doctorId)
    private val scheduleDoc = firestore.collection("schedules").document(doctorId)
    private val queuesCollection = firestore.collection("queues")

    private val repositoryScope = CoroutineScope(Dispatchers.Default)

    // =================================================================
    // 1. DATA FLOWS (REAL-TIME READ)
    // =================================================================

    @OptIn(ExperimentalCoroutinesApi::class)
    override val practiceStatusFlow: StateFlow<Map<String, PracticeStatus>> = callbackFlow {
        Log.d("DEBUG_FLOW", "Memulai Listener Practice Status...")

        val listener = practiceStatusDoc.addSnapshotListener { snapshot, error ->
            // 1. Cek Error Firebase
            if (error != null) {
                Log.e("DEBUG_FLOW", "❌ Firebase Error: ${error.message}", error)
                return@addSnapshotListener
            }

            // 2. Cek Apakah Dokumen Ada?
            if (snapshot != null && snapshot.exists()) {
                Log.d("DEBUG_FLOW", "✅ Snapshot Diterima! Raw Data: ${snapshot.data}")

                try {
                    // 3. Coba Konversi ke Objek Kotlin
                    val status = snapshot.toObject(PracticeStatus::class.java)

                    if (status != null) {
                        Log.d("DEBUG_FLOW", "✅ Sukses Konversi: Open=${status.isPracticeOpen}, LastNo=${status.lastQueueNumber}")
                        trySend(mapOf(doctorId to status))
                    } else {
                        Log.e("DEBUG_FLOW", "❌ Konversi NULL! Cek apakah @NoArgsConstructor ada?")
                    }
                } catch (e: Exception) {
                    Log.e("DEBUG_FLOW", "❌ CRASH saat Konversi: ${e.message}", e)
                }
            } else {
                Log.w("DEBUG_FLOW", "⚠️ Dokumen tidak ditemukan di path: ${practiceStatusDoc.path}")
                trySend(emptyMap<String, PracticeStatus>())
            }
            Unit
        }
        awaitClose { listener.remove() }
    }.stateIn(repositoryScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    override val dailyQueuesFlow: StateFlow<List<QueueItem>> = callbackFlow {
        val (startOfDay, endOfDay) = getTodayRange()
        Log.d("DEBUG_QUEUE", "Mulai Listen Queue. Start: $startOfDay, End: $endOfDay")

        val query = queuesCollection
            .whereEqualTo("doctorId", doctorId)
            .whereGreaterThanOrEqualTo("createdAt", startOfDay)
            .whereLessThanOrEqualTo("createdAt", endOfDay)

        val listener = query.addSnapshotListener { snapshot, error ->
            // 1. Cek Error Index
            if (error != null) {
                Log.e("DEBUG_QUEUE", "❌ ERROR LISTENER: ${error.message}", error)
                return@addSnapshotListener
            }

            // 2. Cek Data
            if (snapshot != null) {
                val queues = snapshot.toObjects(QueueItem::class.java)
                Log.d("DEBUG_QUEUE", "✅ Data Diterima: ${queues.size} item")
                queues.forEach {
                    Log.d("DEBUG_QUEUE", " - Item: No ${it.queueNumber} (${it.status})")
                }

                // Kirim ke UI
                trySend(queues.sortedBy { it.queueNumber })
            } else {
                Log.w("DEBUG_QUEUE", "⚠️ Snapshot NULL")
            }
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

            // Cek apakah user sudah punya antrian aktif hari ini
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

            // Jalankan Transaksi untuk Data Consistency
            val newQueueItem = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(practiceStatusDoc)
                val status = snapshot.toObject(PracticeStatus::class.java)
                    ?: throw Exception("Data praktik tidak ditemukan.")

                if (!status.isPracticeOpen) throw Exception("Pendaftaran antrian sedang ditutup.")
                if (status.lastQueueNumber >= status.dailyPatientLimit) throw Exception("Kuota antrian hari ini sudah penuh.")

                val newNumber = status.lastQueueNumber + 1
                val newQueueRef = queuesCollection.document()
                val newItem = QueueItem(
                    id = newQueueRef.id,
                    queueNumber = newNumber,
                    userId = userId,
                    userName = userName,
                    doctorId = doctorId,
                    keluhan = keluhan,
                    status = QueueStatus.MENUNGGU,
                    createdAt = Date()
                )

                transaction.update(practiceStatusDoc, "lastQueueNumber", newNumber)
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

                if (!snapshot.exists()) throw Exception("QR Code tidak valid (Data tidak ditemukan).")

                val currentStatus = snapshot.getString("status")
                val queueNumber = snapshot.getLong("queueNumber")?.toInt() ?: 0
                val patientName = snapshot.getString("userName") ?: ""

                if (currentStatus == "SELESAI") throw Exception("Pasien $patientName sudah selesai diperiksa.")
                if (currentStatus == "DIBATALKAN") throw Exception("Antrian $patientName sudah dibatalkan.")
                if (currentStatus == "DILAYANI") throw Exception("Pasien $patientName sedang dilayani.")

                transaction.update(docRef, "status", QueueStatus.DILAYANI.name)
                transaction.update(docRef, "startedAt", Date())
                if (snapshot.getDate("calledAt") == null) {
                    transaction.update(docRef, "calledAt", Date())
                }

                transaction.update(practiceStatusDoc, "currentServingNumber", queueNumber)

            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Gagal Scan QR", e)
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
                val docId = snapshot.documents[0].id
                confirmArrivalByQr(docId)
            } else {
                Result.failure(Exception("Antrian tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun finishConsultation(queueNumber: Int, doctorId: String): Result<Unit> {
        return try {
            val (startOfDay, endOfDay) = getTodayRange()
            // 1. Ambil dokumen antrian dulu (Query biasa)
            val snapshot = queuesCollection
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("queueNumber", queueNumber)
                .whereEqualTo("status", QueueStatus.DILAYANI.name)
                .whereGreaterThanOrEqualTo("createdAt", startOfDay)
                .whereLessThanOrEqualTo("createdAt", endOfDay)
                .get().await()

            if (snapshot.documents.isNotEmpty()) {
                val docRef = snapshot.documents[0].reference

                // 2. Jalankan Transaksi
                firestore.runTransaction { transaction ->
                    // A. BACA DULU (Wajib di awal transaksi)
                    val practiceSnapshot = transaction.get(practiceStatusDoc)
                    val totalServed = practiceSnapshot.getLong("totalServed") ?: 0

                    // B. BARU TULIS/UPDATE
                    transaction.update(docRef, "status", QueueStatus.SELESAI.name)
                    transaction.update(docRef, "finishedAt", java.util.Date())

                    transaction.update(practiceStatusDoc, "totalServed", totalServed + 1)
                    transaction.update(practiceStatusDoc, "currentServingNumber", 0)
                }.await()

                Result.success(Unit)
            } else {
                Result.failure(Exception("Pasien tidak ditemukan atau status salah."))
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
            val rawList = snapshot.get("dailySchedules") as? List<*>

            if (rawList == null) {
                Log.e("DEBUG_CLINIC", "Jadwal kosong atau format salah.")
                return emptyList()
            }

            rawList.mapNotNull { item ->
                val map = item as? Map<String, Any> ?: return@mapNotNull null

                // --- PERBAIKAN PARSING: Menangani Boolean atau String ---
                val isOpenVal = map["isOpen"] ?: map["open"]
                val isOpenBool = when(isOpenVal) {
                    is Boolean -> isOpenVal
                    is String -> isOpenVal.toBoolean()
                    else -> false
                }

                DailyScheduleData(
                    dayOfWeek = (map["dayOfWeek"] as? String)?.trim() ?: "N/A", // Trim spasi
                    isOpen = isOpenBool,
                    startTime = map["startTime"] as? String ?: "00:00",
                    endTime = map["endTime"] as? String ?: "00:00"
                )
            }
        } catch (e: Exception) {
            Log.e("DEBUG_CLINIC", "Error getDoctorSchedule", e)
            emptyList()
        }
    }

    override suspend fun updateDoctorSchedule(doctorId: String, newSchedule: List<DailyScheduleData>): Result<Unit> {
        return try {
            scheduleDoc.update("dailySchedules", newSchedule).await()
            // PENTING: Paksa cek ulang status setelah update agar UI langsung berubah
            checkAndUpdatePracticeStatus()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun setPracticeOpen(doctorId: String, isOpen: Boolean): Result<Unit> {
        return try {
            Log.w("DEBUG_CLINIC", ">>> WRITING TO DB: isPracticeOpen = $isOpen")
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

                if (status.lastQueueNumber >= status.dailyPatientLimit) throw Exception("Penuh")

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
                    status = queue.status
                )
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Gagal ambil history", e)
            emptyList()
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
                        doc.reference.update(
                            mapOf(
                                "status" to QueueStatus.MENUNGGU.name,
                                "hasBeenLate" to true,
                                "calledAt" to null
                            )
                        )
                        Log.d("FirestoreRepo", "Pasien ${doc.id} ditandai TELAT")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Check late error", e)
        }
    }

    // --- IMPLEMENTASI REAL UNTUK GRAFIK REPORT ---
    override suspend fun getWeeklyReport(): List<DailyReport> {
        return try {
            // 1. Tentukan Range: 7 Hari Terakhir
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
            val endOfDay = calendar.time // Hari ini

            calendar.add(Calendar.DAY_OF_YEAR, -6) // Mundur 6 hari
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            val startOfPeriod = calendar.time

            // 2. Query Firestore: Ambil semua antrian yang STATUS-nya SELESAI
            val snapshot = queuesCollection
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("status", "SELESAI")
                .whereGreaterThanOrEqualTo("createdAt", startOfPeriod)
                .get()
                .await()

            val historyList = snapshot.toObjects(QueueItem::class.java)

            // 3. Logic Agregasi (Mengelompokkan Data)
            // Kita siapkan map kosong untuk 7 hari ke depan agar grafik tidak bolong
            val reportMap = linkedMapOf<String, Int>()
            val dayFormat = SimpleDateFormat("EEE", Locale("id", "ID")) // Format: Sen, Sel, Rab

            // Reset calendar ke awal periode
            val tempCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
            tempCal.time = startOfPeriod

            // Isi default 0 untuk setiap hari
            for (i in 0..6) {
                val dayLabel = dayFormat.format(tempCal.time)
                reportMap[dayLabel] = 0
                tempCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            // Hitung jumlah pasien real dari database
            historyList.forEach { item ->
                val dayLabel = dayFormat.format(item.createdAt)
                if (reportMap.containsKey(dayLabel)) {
                    reportMap[dayLabel] = reportMap[dayLabel]!! + 1
                }
            }

            // 4. Konversi ke Model DTO untuk Grafik
            reportMap.map { (day, count) ->
                DailyReport(day, count)
            }

        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Gagal hitung laporan", e)
            emptyList()
        }
    }

    override suspend fun resetQueue() {}

    // --- Helper Timer & Tanggal (FIX TIMEZONE) ---

    private fun getTodayRange(): Pair<Date, Date> {
        // PERBAIKAN: Gunakan TimeZone Jakarta untuk penentuan hari
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

    // --- LOGIKA JAM YANG LEBIH KUAT (TIMEZONE AWARE) ---
    private fun isCurrentlyOpen(scheduleList: List<DailyScheduleData>): Boolean {
        if (scheduleList.isEmpty()) {
            Log.e("DEBUG_CLINIC", "❌ Schedule list kosong!")
            return false
        }

        // 1. Ambil Waktu JAKARTA (WIB)
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
        val dayOfWeekInt = calendar.get(Calendar.DAY_OF_WEEK)
        // Hardcode mapping agar tidak bergantung pada Locale Device
        // Calendar.SUNDAY = 1, MONDAY = 2, ...
        val dayMapping = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
        val currentDay = dayMapping[dayOfWeekInt - 1]

        // Log Detil
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val nowMinutes = (currentHour * 60) + currentMinute

        // 2. Cari jadwal hari ini (Case Insensitive & Trimmed)
        val today = scheduleList.find { it.dayOfWeek.trim().equals(currentDay, ignoreCase = true) }

        if (today == null) {
            Log.e("DEBUG_CLINIC", "❌ Tidak ada jadwal untuk hari: $currentDay (List: ${scheduleList.map { it.dayOfWeek }})")
            return false
        }

        // Cek switch Open/Close
        if (!today.isOpen) {
            Log.w("DEBUG_CLINIC", "🔒 Jadwal Hari $currentDay: SWITCH OFF (Tutup).")
            return false
        }

        // 3. Logika Perbandingan Menit
        return try {
            val startParts = today.startTime.split(":")
            val startMinutes = (startParts[0].toInt() * 60) + startParts[1].toInt()

            val endParts = today.endTime.split(":")
            val endMinutes = (endParts[0].toInt() * 60) + endParts[1].toInt()

            val isOpenNow = nowMinutes in startMinutes..endMinutes

            Log.d("DEBUG_CLINIC", "🕒 WIB Time: $currentHour:$currentMinute ($nowMinutes) | Jadwal: $startMinutes-$endMinutes | Result: $isOpenNow")
            isOpenNow
        } catch (e: Exception) {
            Log.e("DEBUG_CLINIC", "❌ Error parsing waktu: ${e.message}")
            false
        }
    }

    private suspend fun checkAndUpdatePracticeStatus() {
        try {
            val schedule = getDoctorSchedule(doctorId)
            val shouldBeOpen = isCurrentlyOpen(schedule)

            // Cek status saat ini di DB (single fetch, bukan listener)
            val snapshot = practiceStatusDoc.get().await()
            val currentIsOpenInDb = snapshot.getBoolean("isPracticeOpen") ?: false

            Log.d("DEBUG_CLINIC", "🔍 DB: $currentIsOpenInDb vs Logic: $shouldBeOpen")

            // 4. Jika beda, PAKSA update
            if (currentIsOpenInDb != shouldBeOpen) {
                Log.w("DEBUG_CLINIC", ">>> ⚠️ FORCE UPDATE: Mengubah status menjadi $shouldBeOpen <<<")
                setPracticeOpen(doctorId, shouldBeOpen)
            }
        } catch (e: Exception) {
            Log.e("DEBUG_CLINIC", "Gagal check status", e)
        }
    }

    init {
        repositoryScope.launch {
            // Loop Timer
            while (true) {
                checkAndUpdatePracticeStatus()
                checkForLatePatients(doctorId)
                delay(30_000L) // Cek setiap 30 detik agar responsif
            }
        }
    }
}