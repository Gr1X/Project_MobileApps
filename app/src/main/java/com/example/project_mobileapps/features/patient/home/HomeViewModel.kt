package com.example.project_mobileapps.features.patient.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.Doctor
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.DoctorRepository
import com.example.project_mobileapps.data.repo.NotificationRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
/**
 * Model data (UI State) untuk [HomeScreen].
 * Menggabungkan semua data yang diperlukan oleh UI Home Pasien.
 *
 * @property greeting Salam sapaan (misal: "Selamat Pagi").
 * @property userName Nama pengguna yang sedang login.
 * @property doctor Objek [Doctor] yang akan ditampilkan di kartu utama.
 * @property activeQueue Antrian [QueueItem] milik pengguna yang sedang aktif (status MENUNGGU).
 * @property practiceStatus Objek [PracticeStatus] dokter (buka/tutup, nomor saat ini, dll).
 * @property currentlyServingPatient Pasien [QueueItem] yang sedang dilayani saat ini.
 * @property upcomingQueue Daftar antrian yang akan datang (Menunggu, Dipanggil, Dilayani).
 * @property availableSlots Jumlah slot antrian yang masih tersisa untuk hari ini.
 * @property isLoading Menandakan apakah data awal sedang dimuat.
 */
data class HomeUiState(
    val greeting: String = "Selamat Datang",
    val userName: String = "Pengguna",
    val doctor: Doctor? = null,
    val activeQueue: QueueItem? = null,
    val practiceStatus: PracticeStatus? = null,
    val currentlyServingPatient: QueueItem? = null,
    val upcomingQueue: List<QueueItem> = emptyList(),
    val availableSlots: Int = 0,
    val isLoading: Boolean = true
)

class HomeViewModel (
    private val doctorRepository: DoctorRepository,
    private val authRepository: AuthRepository,
    private val queueRepository: QueueRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    /**
     * Blok inisialisasi. Dipanggil saat ViewModel dibuat.
     * Memulai pengambilan data utama dan pengamat notifikasi.
     */
    init {
        fetchAllHomeData()
        observeQueueForNotifications()
    }
    /**
     * Helper untuk memberikan sapaan berdasarkan jam.
     */
    private fun getGreetingBasedOnTime(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Selamat Pagi"
            in 12..13 -> "Selamat Siang"
            in 14..17 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }
    /**
     * Mengambil dan menggabungkan semua data yang diperlukan untuk Home Screen.
     * Menggunakan `combine` untuk secara reaktif memperbarui [HomeUiState]
     * setiap kali ada perubahan pada salah satu flow sumber.
     */
    private fun fetchAllHomeData() {
        viewModelScope.launch {
            combine(
                authRepository.currentUser,
                queueRepository.dailyQueuesFlow,
                queueRepository.practiceStatusFlow
            ) { user, queues, statuses ->
                val doctorId = "doc_123"
                val practiceStatus = statuses[doctorId]

                val upcoming = queues
                    .filter { it.status == QueueStatus.MENUNGGU ||
                            it.status == QueueStatus.DIPANGGIL ||
                            it.status == QueueStatus.DILAYANI
                    }

                val totalNonCancelledQueues = queues.count { it.status != QueueStatus.DIBATALKAN }
                val slotsLeft = (practiceStatus?.dailyPatientLimit ?: 0) - totalNonCancelledQueues

                val activeQueue = queues.find { it.userId == user?.uid && it.status == QueueStatus.MENUNGGU }
                val currentlyServingPatient = queues
                    .find { it.queueNumber == practiceStatus?.currentServingNumber && it.status == QueueStatus.DILAYANI }

                _uiState.update {
                    it.copy(
                        greeting = getGreetingBasedOnTime(),
                        userName = user?.name ?: "Pengguna",
                        doctor = doctorRepository.getTheOnlyDoctor(),
                        activeQueue = activeQueue,
                        practiceStatus = practiceStatus,
                        upcomingQueue = upcoming,
                        currentlyServingPatient = currentlyServingPatient,
                        availableSlots = slotsLeft.coerceAtLeast(0),
                        isLoading = false
                    )
                }
            }.collect()
        }

        viewModelScope.launch {
            while (true) {
                queueRepository.checkForLatePatients("doc_123")
                delay(10000L)
            }
        }
    }
    /**
     * Mengamati perubahan state (user, antrian, status) untuk memicu notifikasi in-app.
     * Fungsi ini membandingkan `currentState` dengan `previousState`
     * untuk mendeteksi perubahan spesifik.
     */
    private fun observeQueueForNotifications() {
        viewModelScope.launch {
            val combinedFlow = combine(
                authRepository.currentUser,
                queueRepository.dailyQueuesFlow,
                queueRepository.practiceStatusFlow
            ) { user, queues, statuses ->
                Triple(user, queues, statuses)
            }

            var previousState: Triple<User?, List<QueueItem>, Map<String, PracticeStatus>>? = null

            combinedFlow.collect { currentState ->
                if (previousState != null) {
                    val (prevUser, prevQueues, prevStatuses) = previousState!!
                    val (currentUser, currentQueues, currentStatuses) = currentState

                    val myId = currentUser?.uid ?: return@collect
                    val doctorId = "doc_123"
                    val currentPracticeStatus = currentStatuses[doctorId]
                    val prevPracticeStatus = prevStatuses[doctorId]

                    // 1. Notifikasi: Status Praktik Dokter Berubah
                    if (prevPracticeStatus?.isPracticeOpen != currentPracticeStatus?.isPracticeOpen && currentPracticeStatus != null) {
                        val statusText = if (currentPracticeStatus.isPracticeOpen) "dibuka" else "ditutup"
                        NotificationRepository.addNotification("Praktik dokter saat ini telah $statusText.")
                    }

                    // 2. Notifikasi: Berhasil Mengambil Antrian
                    val newQueue = currentQueues.find { it.userId == myId && !prevQueues.any { prev -> prev.queueNumber == it.queueNumber } }
                    newQueue?.let {
                        NotificationRepository.addNotification("Anda berhasil mendapatkan nomor antrian ${it.queueNumber}.")
                    }

                    // --- Logika Perbandingan Antrian Individual ---
                    val myPreviousQueues = prevQueues.filter { it.userId == myId }
                    myPreviousQueues.forEach { prevQueue ->
                        val currentQueue = currentQueues.find { it.queueNumber == prevQueue.queueNumber }
                        if (currentQueue != null) {
                            // 3. Notifikasi: Antrian Dibatalkan
                            if (prevQueue.status == QueueStatus.MENUNGGU && currentQueue.status == QueueStatus.DIBATALKAN) {
                                NotificationRepository.addNotification("Nomor antrian ${currentQueue.queueNumber} telah dibatalkan.")
                            }
                            // 4. Notifikasi: Anda Dipanggil
                            if (prevQueue.status == QueueStatus.MENUNGGU && currentQueue.status == QueueStatus.DIPANGGIL) {
                                NotificationRepository.addNotification("Nomor antrian ${currentQueue.queueNumber} telah dipanggil. Segera menuju ruang periksa.")
                            }
                        }
                    }

                    // 5. NOTIFIKASI DIPERBAIKI: Giliran Anda Sudah Dekat
                    val myCurrentWaitingQueue = currentQueues.find { it.userId == myId && it.status == QueueStatus.MENUNGGU }
                    // Hanya jalankan jika semua data yang dibutuhkan ada (antrian saya, status praktik sekarang & sebelumnya)
                    if (myCurrentWaitingQueue != null && currentPracticeStatus != null && prevPracticeStatus != null) {
                        val currentServing = currentPracticeStatus.currentServingNumber
                        val peopleAhead = myCurrentWaitingQueue.queueNumber - currentServing

                        val prevServing = prevPracticeStatus.currentServingNumber
                        val prevPeopleAhead = myCurrentWaitingQueue.queueNumber - prevServing

                        // Kondisi ini sekarang akan berjalan dengan data yang dijamin akurat
                        if (peopleAhead == 2 && prevPeopleAhead > 2) {
                            val estimatedTime = 2 * currentPracticeStatus.estimatedServiceTimeInMinutes
                            NotificationRepository.addNotification("Giliran Anda 2 antrian lagi (sekitar $estimatedTime menit). Harap bersiap.")
                        }
                    }
                }
                previousState = currentState
            }
        }
    }
}