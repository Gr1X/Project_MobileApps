// File: features/doctor/DoctorViewModel.kt
package com.example.project_mobileapps.features.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.features.admin.manageSchedule.PatientQueueDetails
import com.example.project_mobileapps.ui.components.ToastManager
import com.example.project_mobileapps.ui.components.ToastType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

data class DoctorUiState(
    val greeting: String = "Halo,",
    val doctorName: String = "Dokter",
    val isLoading: Boolean = true,
    val practiceStatus: PracticeStatus? = null,
    val operatingHours: String = "--:-- s/d --:--",
    val currentlyServing: PatientQueueDetails? = null,
    val patientCalled: PatientQueueDetails? = null,
    val topQueueList: List<PatientQueueDetails> = emptyList(),
    val nextQueueNumber: Int = 0,
    val waitingCount: Int = 0,
    val finishedCount: Int = 0
)

class DoctorViewModel(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorUiState())
    val uiState: StateFlow<DoctorUiState> = _uiState.asStateFlow()

    private val clinicId = AppContainer.CLINIC_ID

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val user = authRepository.currentUser.value
        _uiState.update { it.copy(greeting = getGreeting(), doctorName = user?.name ?: "Dokter") }

        viewModelScope.launch {
            combine(
                queueRepository.dailyQueuesFlow,
                queueRepository.practiceStatusFlow
            ) { queues, statusMap ->

                // 1. Ambil Status Praktik (Buka/Tutup Realtime)
                val clinicStatus = statusMap[clinicId] ?: statusMap.values.firstOrNull()

                // 2. [PERBAIKAN] Ambil Jadwal Hari Ini (Jam Operasional)
                // Kita ambil dari getDoctorSchedule seperti di Admin
                val weeklySchedule = queueRepository.getDoctorSchedule(clinicId)
                val todaySchedule = getTodaySchedule(weeklySchedule)

                // Format Jam (Contoh: "08:00 - 15:00")
                val timeString = if (todaySchedule != null && todaySchedule.isOpen) {
                    "${todaySchedule.startTime} - ${todaySchedule.endTime}"
                } else {
                    "Libur"
                }

                // 3. Mapping Data Antrian
                val allDetails = queues.map { item ->
                    val patientUser = authRepository.getUserById(item.userId)
                    PatientQueueDetails(item, patientUser)
                }

                val serving = allDetails.find { it.queueItem.status == QueueStatus.DILAYANI }
                val called = allDetails.find { it.queueItem.status == QueueStatus.DIPANGGIL }

                val waitingList = allDetails
                    .filter { it.queueItem.status == QueueStatus.MENUNGGU }
                    .sortedBy { it.queueItem.queueNumber }

                val finishedCount = allDetails.count { it.queueItem.status == QueueStatus.SELESAI }

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        practiceStatus = clinicStatus,
                        operatingHours = timeString, // Sekarang data ini benar (String jam)
                        currentlyServing = serving,
                        patientCalled = called,
                        topQueueList = waitingList,
                        nextQueueNumber = waitingList.firstOrNull()?.queueItem?.queueNumber ?: 0,
                        waitingCount = waitingList.size,
                        finishedCount = finishedCount
                    )
                }
            }.collect()
        }
    }

    // Helper: Mencari jadwal hari ini (Senin, Selasa, dll)
    private fun getTodaySchedule(schedules: List<com.example.project_mobileapps.data.model.DailyScheduleData>): com.example.project_mobileapps.data.model.DailyScheduleData? {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // Mapping Calendar Java ke Nama Hari di Database Anda
        val dayName = when (dayOfWeek) {
            Calendar.SUNDAY -> "Minggu"
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> ""
        }

        return schedules.find { it.dayOfWeek.equals(dayName, ignoreCase = true) }
    }

    fun toggleQueue(isOpen: Boolean) {
        viewModelScope.launch {
            queueRepository.updatePracticeStatus(clinicId, isOpen)
        }
    }

    fun callNextPatient() {
        val nextPatient = _uiState.value.topQueueList.firstOrNull() ?: return
        viewModelScope.launch {
            queueRepository.updateQueueStatus(nextPatient.queueItem.id, QueueStatus.DIPANGGIL)
            ToastManager.showToast("Memanggil A-${nextPatient.queueItem.queueNumber}", ToastType.INFO)
        }
    }

    fun startConsultation(queueItem: QueueItem) {
        viewModelScope.launch {
            queueRepository.updateQueueStatus(queueItem.id, QueueStatus.DILAYANI)
        }
    }

    private fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..10 -> "Selamat Pagi,"
            in 11..14 -> "Selamat Siang,"
            in 15..18 -> "Selamat Sore,"
            else -> "Selamat Malam,"
        }
    }
}

class DoctorViewModelFactory(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DoctorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DoctorViewModel(queueRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}