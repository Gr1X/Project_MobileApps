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

                val sortedList = queues.sortedWith(
                    compareBy<QueueItem> {
                        // 1. Prioritas Status (Tetap Sama)
                        when (it.status) {
                            QueueStatus.DILAYANI -> 1
                            QueueStatus.DIPANGGIL -> 2
                            QueueStatus.MENUNGGU -> 3
                            else -> 4
                        }
                    }.thenBy {
                        // 2. Sorting Waktu (Tetap Sama)
                        // Pasien telat (createdAt baru) akan bersaing dengan pasien baru
                        it.createdAt
                    }.thenBy {
                        // [PERBAIKAN] 3. Tie-Breaker (Penentu Seri)
                        // Jika createdAt sama persis (langka, tapi mungkin),
                        // gunakan queueNumber sebagai penentu agar list stabil.
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

    override suspend fun getPatientMedicalHistory(patientId: String): Result<List<QueueItem>> {
        return try {
            val snapshot = queuesCollection
                .whereEqualTo("userId", patientId)
                .whereEqualTo("status", "SELESAI") // Hanya ambil yang sudah selesai
                .orderBy("finishedAt", Query.Direction.DESCENDING) // Urutkan dari yang terbaru
                .get()
                .await()

            val historyList = snapshot.toObjects(QueueItem::class.java)
            Result.success(historyList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================================================================
    // 2. USER ACTIONS (WRITE)
    // =================================================================

    override suspend fun takeQueueNumber(doctorId: String, userId: String, userName: String, keluhan: String): Result<QueueItem> {
        return try {
            val (startOfDay, endOfDay) = getTodayRange()

            // 1. Cek Jadwal (Tetap)
            val scheduleSnapshot = scheduleDoc.get().await()
            val rawList = scheduleSnapshot.get("dailySchedules") as? List<*> ?: emptyList<Any>()
            val scheduleList = parseScheduleList(rawList)
            if (!isCurrentlyOpen(scheduleList)) {
                return Result.failure(Exception("Pendaftaran gagal: Klinik sedang tutup."))
            }

            // 2. Cek Duplikasi Harian (Tetap)
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

            // [PERBAIKAN TOTAL BAGIAN INI]
            // HAPUS query 'lastQueueSnapshot' yang lama di sini.
            // Kita akan hitung nomor di dalam transaction agar Atomic & Aman dari Race Condition.

            val newQueueItem = firestore.runTransaction { transaction ->
                // A. Baca Status Praktik (Lock Document)
                val snapshot = transaction.get(practiceStatusDoc)
                val status = snapshot.toObject(PracticeStatus::class.java)
                    ?: throw Exception("Data praktik tidak ditemukan.")

                if (!status.isPracticeOpen) throw Exception("Pendaftaran sedang ditutup oleh Admin.")

                // B. Hitung Nomor Baru dari Master Counter
                val nextQueueNumber = status.lastQueueNumber + 1

                // C. Validasi Kuota
                if (nextQueueNumber > status.dailyPatientLimit) throw Exception("Kuota antrian hari ini sudah penuh.")

                // D. Siapkan Data
                val newQueueRef = queuesCollection.document()
                val newItem = QueueItem(
                    id = newQueueRef.id,
                    queueNumber = nextQueueNumber, // Gunakan hasil hitungan atomic
                    userId = userId,
                    userName = userName,
                    doctorId = doctorId,
                    keluhan = keluhan,
                    status = QueueStatus.MENUNGGU,
                    createdAt = Date()
                )

                // E. Update Master Counter & Simpan Antrian
                transaction.update(practiceStatusDoc, "lastQueueNumber", nextQueueNumber)
                transaction.set(newQueueRef, newItem)

                newItem // Return
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
            checkForLatePatients(doctorId)
            val (startOfDay, endOfDay) = getTodayRange()

            val snapshot = queuesCollection
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("status", QueueStatus.MENUNGGU.name)
                .whereGreaterThanOrEqualTo("createdAt", startOfDay)
                .whereLessThanOrEqualTo("createdAt", endOfDay)
                .get().await()

            val candidates = snapshot.toObjects(QueueItem::class.java)

            if (candidates.isNotEmpty()) {
                // [PERBAIKAN] Urutkan berdasarkan createdAt, bukan queueNumber
                // Karena queueNumber bisa jadi tidak urut (misal No 1 dilempar ke belakang)
                val nextPatient = candidates.sortedBy { it.createdAt }.firstOrNull()

                if (nextPatient != null) {
                    val docRef = queuesCollection.document(nextPatient.id)
                    firestore.runTransaction { transaction ->
                        val freshSnapshot = transaction.get(docRef)
                        if (freshSnapshot.getString("status") == QueueStatus.MENUNGGU.name) {
                            transaction.update(docRef, "status", QueueStatus.DIPANGGIL.name)
                            transaction.update(docRef, "calledAt", Date())
                        }
                    }.await()
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Tidak ada antrian valid."))
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
    // 5. HELPER / AUTOMATION (PERBAIKAN LOGIC AUTO-CLOSE)
    // =================================================================

    private suspend fun runAutoScheduleCheck() {
        try {
            val schedule = getDoctorSchedule(doctorId)
            if (schedule.isEmpty()) return

            // Gunakan TimeZone Default HP agar sesuai dengan jam yang dilihat user
            val calendar = Calendar.getInstance(TimeZone.getDefault())

            val dayMapping = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
            val dayIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1
            val currentDay = dayMapping[dayIndex]

            // Konversi jam sekarang ke total menit (Contoh: 08:30 -> 510 menit)
            val nowMinutes = (calendar.get(Calendar.HOUR_OF_DAY) * 60) + calendar.get(Calendar.MINUTE)

            val todaySchedule = schedule.find { it.dayOfWeek.trim().equals(currentDay, ignoreCase = true) }

            val updates = mutableMapOf<String, Any>()
            val snapshot = practiceStatusDoc.get().await()
            val currentIsOpenInDb = snapshot.getBoolean("isPracticeOpen") ?: false

            // --- LOGIKA UTAMA ---
            if (todaySchedule != null && todaySchedule.isOpen) {
                // Bersihkan string jam dari spasi atau titik
                val safeStart = todaySchedule.startTime.replace(".", ":").trim()
                val safeEnd = todaySchedule.endTime.replace(".", ":").trim()

                try {
                    // Parsing Jam Buka & Tutup
                    val (sh, sm) = safeStart.split(":").map { it.toInt() }
                    val (eh, em) = safeEnd.split(":").map { it.toInt() }
                    val startMins = sh * 60 + sm
                    val endMins = eh * 60 + em

                    // Update Info Jam di DB agar UI Admin sinkron
                    updates["startTime"] = safeStart
                    updates["endTime"] = safeEnd

                    // [LOGIKA BARU] Cek Apakah Sedang Jam Istirahat?
                    var isCurrentlyBreak = false

                    // Pastikan Model DailyScheduleData sudah punya field isBreakEnabled!
                    if (todaySchedule.isBreakEnabled) {
                        try {
                            val safeBreakStart = todaySchedule.breakStartTime.replace(".", ":").trim()
                            val safeBreakEnd = todaySchedule.breakEndTime.replace(".", ":").trim()
                            val (bSh, bSm) = safeBreakStart.split(":").map { it.toInt() }
                            val (bEh, bEm) = safeBreakEnd.split(":").map { it.toInt() }
                            val breakStartMins = bSh * 60 + bSm
                            val breakEndMins = bEh * 60 + bEm

                            if (nowMinutes >= breakStartMins && nowMinutes < breakEndMins) {
                                isCurrentlyBreak = true
                            }
                        } catch (e: Exception) {
                            Log.e("AutoSchedule", "Format Jam Istirahat Salah", e)
                        }
                    }

                    // --- KEPUTUSAN FINAL: BUKA ATAU TUTUP? ---
                    // Buka JIKA: (Masuk Jam Operasional) DAN (TIDAK Sedang Istirahat)
                    val shouldBeOpen = (nowMinutes in startMins..endMins) && !isCurrentlyBreak

                    if (shouldBeOpen) {
                        // KASUS: HARUSNYA BUKA
                        if (!currentIsOpenInDb) {
                            Log.d("AutoSchedule", "⏰ Auto-Open: Masuk Jam Kerja & Tidak Istirahat")
                            updates["isPracticeOpen"] = true
                        }
                    } else {
                        // KASUS: HARUSNYA TUTUP (Entah karena Istirahat atau Di Luar Jam)
                        if (currentIsOpenInDb) {
                            val reason = if(isCurrentlyBreak) "Sedang Istirahat" else "Di Luar Jam Operasional"
                            Log.d("AutoSchedule", "🔒 Auto-Close: $reason")
                            updates["isPracticeOpen"] = false

                            // Opsional: cancelRemainingQueues(doctorId)
                        }
                    }

                } catch (e: Exception) {
                    Log.e("AutoSchedule", "Format jam operasional salah: $safeStart - $safeEnd", e)
                }
            } else {
                // HARI LIBUR -> Jika DB masih Buka, Paksa Tutup
                if (currentIsOpenInDb) {
                    Log.d("AutoSchedule", "🔒 Auto-Close: Hari Libur")
                    updates["isPracticeOpen"] = false
                }
            }

            // Simpan perubahan ke Firestore jika ada
            if (updates.isNotEmpty()) {
                practiceStatusDoc.update(updates).await()
            }

        } catch (e: Exception) {
            Log.e("AutoSchedule", "Error Fatal di Automation", e)
        }
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

            // 1. Bersihkan antrian sisa kemarin (Tetap)
            val snapshot = queuesCollection
                .whereEqualTo("doctorId", doctorId)
                .whereLessThan("createdAt", startOfToday)
                .whereIn("status", listOf("MENUNGGU", "DIPANGGIL"))
                .get().await()

            val batch = firestore.batch()
            var hasUpdates = false

            if (!snapshot.isEmpty) {
                snapshot.documents.forEach { doc ->
                    batch.update(doc.reference, mapOf(
                        "status" to QueueStatus.DIBATALKAN.name,
                        "doctorNotes" to "Sistem: Dibatalkan otomatis (Ganti Hari)"
                    ))
                }
                hasUpdates = true
            }

            // [PENAMBAHAN PENTING]
            // Reset Counter "lastQueueNumber" dan "totalServed" jika sudah ganti hari
            // Kita cek lastResetDate atau logika sederhana: jika cleaning up old queues, means it's a new day.
            // Namun, agar aman, kita cek apakah 'lastQueueNumber' > 0 padahal antrian hari ini kosong?

            // Cek jumlah antrian hari ini
            val todayCountSnapshot = queuesCollection
                .whereEqualTo("doctorId", doctorId)
                .whereGreaterThanOrEqualTo("createdAt", startOfToday)
                .limit(1) // Cukup cek ada 1 atau tidak
                .get().await()

            if (todayCountSnapshot.isEmpty) {
                // Jika hari ini BELUM ada antrian sama sekali, PAKSA RESET COUNTER
                val statusRef = firestore.collection("practice_status").document(doctorId)
                batch.update(statusRef, mapOf(
                    "lastQueueNumber" to 0,
                    "currentServingNumber" to 0,
                    "totalServed" to 0
                ))
                hasUpdates = true
                Log.d("FirestoreRepo", "🔄 Reset Harian: Counter dikembalikan ke 0")
            }

            if (hasUpdates) {
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
            val limitMs = limitMinutes * 60 * 1000L
            val now = Date().time

            // Cari pasien yang sedang DIPANGGIL
            val snapshot = queuesCollection
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("status", QueueStatus.DIPANGGIL.name)
                .whereGreaterThanOrEqualTo("createdAt", start)
                .whereLessThanOrEqualTo("createdAt", end)
                .get().await()

            val batch = firestore.batch()
            var hasUpdates = false

            for (doc in snapshot.documents) {
                val calledAt = doc.getDate("calledAt")
                // Ambil count saat ini (default 0)
                val currentMissedCount = doc.getLong("missedCallCount")?.toInt() ?: 0
                val callTime = calledAt?.time ?: now

                // JIKA WAKTU HABIS
                if (now - callTime > limitMs) {
                    hasUpdates = true
                    val newCount = currentMissedCount + 1

                    if (newCount >= 3) {
                        // KASUS: SUDAH 3 KALI TELAT -> BATAL OTOMATIS
                        batch.update(doc.reference, mapOf(
                            "status" to QueueStatus.DIBATALKAN.name,
                            "missedCallCount" to newCount,
                            "doctorNotes" to "Sistem: Dibatalkan otomatis (Tidak hadir setelah 3x pemanggilan)"
                        ))
                        Log.d("FirestoreRepo", "🚫 Pasien ${doc.id} dibatalkan (3x Strikes).")
                    } else {
                        // KASUS: TELAT KE-1 atau KE-2 -> LEMPAR KE BELAKANG
                        // 1. Kembalikan status ke MENUNGGU
                        // 2. Tambah missedCount
                        // 3. Reset calledAt
                        // 4. [KUNCI] Update createdAt jadi SEKARANG.
                        //    Ini membuat dia seolah-olah baru daftar detik ini,
                        //    jadi posisinya pindah ke paling belakang antrian SAAT INI.
                        batch.update(doc.reference, mapOf(
                            "status" to QueueStatus.MENUNGGU.name,
                            "missedCallCount" to newCount,
                            "calledAt" to null,
                            "createdAt" to Date() // <--- INI MAGICNYA
                        ))
                        Log.d("FirestoreRepo", "⚠️ Pasien ${doc.id} dipindah ke belakang (Strike $newCount).")
                    }
                }
            }

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
                runAutoScheduleCheck()
                checkForLatePatients(doctorId)
                delay(30_000L) // UBAH JADI 30 DETIK (Lebih aman untuk baterai & kuota)
            }
        }
    }
}