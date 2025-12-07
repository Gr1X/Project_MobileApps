// File: features/patient/home/HomeViewModel.kt
package com.example.project_mobileapps.features.patient.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.Doctor
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.DoctorRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import com.example.project_mobileapps.data.repo.NotificationRepository // Pastikan ada repository ini
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.utils.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

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

class HomeViewModel(
    application: Application,
    private val doctorRepository: DoctorRepository,
    private val authRepository: AuthRepository,
    private val queueRepository: QueueRepository
) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext
    private val clinicId = AppContainer.CLINIC_ID
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Variabel State untuk Mencegah Spam Notifikasi
    private var lastNotifiedStatus: QueueStatus? = null
    private var lastNotifiedQueueNumber: Int = -1

    init {
        fetchAllHomeData()
        observeQueueForNotifications() // INI YANG PENTING
    }

    private fun getGreetingBasedOnTime(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Selamat Pagi"
            in 12..13 -> "Selamat Siang"
            in 14..17 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }

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

        // Loop Background Check (Opsional jika ingin check late)
        viewModelScope.launch {
            while (true) {
                queueRepository.checkForLatePatients(clinicId)
                delay(30_000L) // Cek tiap 30 detik
            }
        }
    }

    // --- LOGIKA NOTIFIKASI (YANG DITANYAKAN) ---
    private fun observeQueueForNotifications() {
        viewModelScope.launch {
            combine(
                authRepository.currentUser,
                queueRepository.dailyQueuesFlow,
                queueRepository.practiceStatusFlow
            ) { user, queues, statuses ->
                Triple(user, queues, statuses)
            }.collect { (user, queues, statuses) ->

                val myId = user?.uid ?: return@collect
                val practiceStatus = statuses[clinicId] ?: return@collect

                // Cari antrian SAYA yang aktif
                val myQueue = queues.find {
                    it.userId == myId && (it.status == QueueStatus.MENUNGGU || it.status == QueueStatus.DIPANGGIL)
                }

                if (myQueue != null) {
                    val currentServing = practiceStatus.currentServingNumber
                    val myNumber = myQueue.queueNumber
                    val peopleAhead = myNumber - currentServing

                    Log.d("DEBUG_NOTIF", "Antrian Saya: No $myNumber, Status: ${myQueue.status}. Orang di depan: $peopleAhead")

                    // KASUS 1: BARU SAJA DIPANGGIL
                    if (myQueue.status == QueueStatus.DIPANGGIL && lastNotifiedStatus != QueueStatus.DIPANGGIL) {
                        Log.d("DEBUG_NOTIF", "🚀 MENCOBA KIRIM NOTIF: DIPANGGIL")

                        NotificationHelper.showNotification(
                            context,
                            "GILIRAN ANDA!",
                            "Silakan masuk ke ruang periksa sekarang."
                        )
                        // Simpan log ke DB notifikasi internal (Dropdown Lonceng)
                        NotificationRepository.addNotification("Giliran Anda dipanggil! Segera masuk.")

                        // Update state agar tidak spam
                        lastNotifiedStatus = QueueStatus.DIPANGGIL
                    }

                    // KASUS 2: MENDEKATI GILIRAN (3, 2, 1 orang lagi)
                    // Cek: Apakah nomor yang dilayani berubah? Jika ya, cek apakah perlu notif?
                    if (myQueue.status == QueueStatus.MENUNGGU && currentServing != lastNotifiedQueueNumber) {

                        if (peopleAhead == 3) {
                            Log.d("DEBUG_NOTIF", "🚀 MENCOBA KIRIM NOTIF: SISA 3")
                            NotificationHelper.showNotification(context, "Siap-siap!", "Tersisa 3 antrian lagi.")
                            NotificationRepository.addNotification("Tersisa 3 antrian lagi sebelum giliran Anda.")
                            lastNotifiedQueueNumber = currentServing
                        }
                        else if (peopleAhead == 2) {
                            Log.d("DEBUG_NOTIF", "🚀 MENCOBA KIRIM NOTIF: SISA 2")
                            NotificationHelper.showNotification(context, "Mendekati Giliran", "Hanya 2 orang lagi.")
                            NotificationRepository.addNotification("Tersisa 2 antrian lagi.")
                            lastNotifiedQueueNumber = currentServing
                        }
                        else if (peopleAhead == 1) {
                            Log.d("DEBUG_NOTIF", "🚀 MENCOBA KIRIM NOTIF: SISA 1")
                            NotificationHelper.showNotification(context, "Segera Masuk", "Giliran Anda berikutnya!")
                            NotificationRepository.addNotification("Giliran Anda berikutnya! Mohon bersiap.")
                            lastNotifiedQueueNumber = currentServing
                        }
                    }

                    // Reset status notifikasi DIPANGGIL jika status kembali ke MENUNGGU (misal dokter cancel panggil)
                    if (myQueue.status == QueueStatus.MENUNGGU) {
                        lastNotifiedStatus = QueueStatus.MENUNGGU
                    }

                } else {
                    // Jika antrian selesai/batal, reset semua
                    lastNotifiedStatus = null
                    lastNotifiedQueueNumber = -1
                }
            }
        }
    }
}