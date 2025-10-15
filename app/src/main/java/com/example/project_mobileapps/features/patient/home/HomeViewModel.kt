package com.example.project_mobileapps.features.patient.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.Doctor
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.repo.AuthRepository

import com.example.project_mobileapps.data.repo.DoctorRepository
import com.example.project_mobileapps.data.repo.QueueRepository
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

class HomeViewModel (
    private val doctorRepository: DoctorRepository,
    private val authRepository: AuthRepository,
    private val queueRepository: QueueRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()


    private fun getGreetingBasedOnTime(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Selamat Pagi"
            in 12..13 -> "Selamat Siang"
            in 14..17 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }

    init {
        fetchAllHomeData()
    }

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

                val slotsLeft = (practiceStatus?.dailyPatientLimit ?: 0) - (practiceStatus?.lastQueueNumber ?: 0)
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
}