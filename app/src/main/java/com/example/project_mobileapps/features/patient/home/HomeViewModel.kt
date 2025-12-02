package com.example.project_mobileapps.features.patient.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.utils.NotificationHelper
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
    application: Application,
    private val doctorRepository: DoctorRepository,
    private val authRepository: AuthRepository,
    private val queueRepository: QueueRepository
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val clinicId = AppContainer.CLINIC_ID
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var hasNotifiedApproaching = false
    private var lastQueueStatus: QueueStatus? = null

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
                val practiceStatus = statuses[clinicId]
                val upcoming = queues.filter {
                    it.status == QueueStatus.MENUNGGU || it.status == QueueStatus.DIPANGGIL || it.status == QueueStatus.DILAYANI
                }
                val totalNonCancelled = queues.count { it.status != QueueStatus.DIBATALKAN }
                val slotsLeft = (practiceStatus?.dailyPatientLimit ?: 0) - totalNonCancelled

                // Cari antrian saya yang aktif
                val activeQueue = queues.find { it.userId == user?.uid && (it.status == QueueStatus.MENUNGGU || it.status == QueueStatus.DIPANGGIL) }

                val servingPatient = queues.find {
                    it.queueNumber == practiceStatus?.currentServingNumber && it.status == QueueStatus.DILAYANI
                }

                _uiState.update {
                    it.copy(
                        greeting = getGreetingBasedOnTime(),
                        userName = user?.name ?: "Pengguna",
                        doctor = doctorRepository.getTheOnlyDoctor(),
                        activeQueue = activeQueue,
                        practiceStatus = practiceStatus,
                        upcomingQueue = upcoming,
                        currentlyServingPatient = servingPatient,
                        availableSlots = slotsLeft.coerceAtLeast(0),
                        isLoading = false
                    )
                }
            }.collect()
        }

        // Loop cek telat (Background task)
        viewModelScope.launch {
            while (true) {
                queueRepository.checkForLatePatients(clinicId)
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
            combine(
                authRepository.currentUser,
                queueRepository.dailyQueuesFlow,
                queueRepository.practiceStatusFlow
            ) { user, queues, statuses -> Triple(user, queues, statuses) }
                .collect { (user, queues, statuses) ->
                    val myId = user?.uid ?: return@collect
                    val practiceStatus = statuses[clinicId] ?: return@collect

                    // Cari antrian saya
                    val myQueue = queues.find { it.userId == myId && (it.status == QueueStatus.MENUNGGU || it.status == QueueStatus.DIPANGGIL) }

                    if (myQueue != null) {

                        // KASUS 1: DIPANGGIL (Status berubah dari MENUNGGU -> DIPANGGIL)
                        if (myQueue.status == QueueStatus.DIPANGGIL && lastQueueStatus == QueueStatus.MENUNGGU) {
                            NotificationHelper.showNotification(
                                context,
                                "Giliran Anda!",
                                "Silakan masuk ke ruang periksa sekarang."
                            )
                            NotificationRepository.addNotification("Giliran Anda dipanggil.")
                        }

                        // KASUS 2: MENDEKATI GILIRAN (Tinggal 2 orang lagi)
                        if (myQueue.status == QueueStatus.MENUNGGU) {
                            val currentServing = practiceStatus.currentServingNumber
                            // Hitung berapa orang di depan (Nomor Saya - Nomor Dilayani - 1)
                            val peopleAhead = myQueue.queueNumber - currentServing

                            // Jika sisa 1 atau 2 orang, DAN belum pernah dikasih notif
                            if (peopleAhead in 1..2 && !hasNotifiedApproaching) {
                                val estimasi = peopleAhead * practiceStatus.estimatedServiceTimeInMinutes
                                NotificationHelper.showNotification(
                                    context,
                                    "Segera Bersiap",
                                    "Giliran Anda $peopleAhead antrian lagi (±$estimasi menit)."
                                )
                                NotificationRepository.addNotification("Giliran Anda segera tiba ($peopleAhead orang lagi).")

                                // Kunci agar tidak notif terus menerus
                                hasNotifiedApproaching = true
                            }
                        }

                        // Update tracker status terakhir
                        lastQueueStatus = myQueue.status

                    } else {
                        // Reset jika antrian selesai/hilang
                        hasNotifiedApproaching = false
                        lastQueueStatus = null
                    }
                }
        }
    }
}