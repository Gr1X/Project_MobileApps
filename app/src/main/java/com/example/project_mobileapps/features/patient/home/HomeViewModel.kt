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
import com.example.project_mobileapps.data.repo.NewsRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import com.example.project_mobileapps.data.repo.NotificationRepository // Pastikan ada repository ini
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.features.patient.news.NewsArticleUI
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
    val isLoading: Boolean = true,
    val topNews: List<NewsArticleUI> = emptyList()
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
    private val newsRepository = NewsRepository()
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Variabel State untuk Mencegah Spam Notifikasi
    private var lastNotifiedStatus: QueueStatus? = null
    private var lastNotifiedQueueNumber: Int = -1

    init {
        fetchAllHomeData()
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

                val newsFromApi = newsRepository.getHealthNews().take(3).map { apiArticle ->
                    // Lakukan mapping yang sama seperti di NewsViewModel
                    NewsArticleUI(
                        title = apiArticle.title ?: "Tanpa Judul",
                        source = apiArticle.source?.name ?: "N/A",
                        imageUrl = apiArticle.urlToImage,
                        articleUrl = apiArticle.url
                    )
                }

                _uiState.update {
                    it.copy(
                        topNews = newsFromApi,
                        greeting = getGreetingBasedOnTime(),
                        userName = user?.username?.ifBlank { user.name } ?: "Pengguna",
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
}