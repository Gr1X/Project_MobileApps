// File: features/doctor/DoctorViewModel.kt
package com.example.project_mobileapps.features.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.local.DailyScheduleData
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import com.example.project_mobileapps.features.admin.manageSchedule.PatientQueueDetails
import kotlinx.coroutines.flow.*
import java.util.Calendar

data class DoctorUiState(
    val greeting: String = "Selamat Datang",
    val doctorName: String = "Dokter",
    val topQueueList: List<PatientQueueDetails> = emptyList(),
    val practiceStatus: PracticeStatus? = null,
    val isLoading: Boolean = true,
    val waitingInQueue: Int = 0,
    val nextQueueNumber: String = "-",
    val selectedPatient: PatientQueueDetails? = null,
    val todaySchedule: DailyScheduleData? = null // <-- TAMBAHKAN BARIS INI
)

class DoctorViewModel(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _selectedPatient = MutableStateFlow<PatientQueueDetails?>(null)

    val uiState: StateFlow<DoctorUiState> = combine(
        queueRepository.dailyQueuesFlow,
        queueRepository.practiceStatusFlow,
        authRepository.currentUser,
        _selectedPatient
    ) { queues, statuses, doctorUser, selectedPatient ->

        val doctorId = "doc_123"
        val allUsers = authRepository.getAllUsers()

        // --- LOGIKA BARU DIMULAI DI SINI ---
        val weeklySchedule = queueRepository.getDoctorSchedule(doctorId)
        val calendar = Calendar.getInstance()
        val dayOfWeekInt = calendar.get(Calendar.DAY_OF_WEEK) // Minggu=1, Senin=2, ..
        val dayMapping = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
        val currentDayString = dayMapping[dayOfWeekInt - 1]
        val todaySchedule = weeklySchedule.find { it.dayOfWeek.equals(currentDayString, ignoreCase = true) }
        // --- AKHIR LOGIKA BARU ---

        val activeQueues = queues
            .filter { it.doctorId == doctorId && (it.status == QueueStatus.MENUNGGU || it.status == QueueStatus.DIPANGGIL || it.status == QueueStatus.DILAYANI) }
            .sortedBy { it.queueNumber }
        val topThreeQueues = activeQueues.take(3).map { queueItem ->
            PatientQueueDetails(queueItem = queueItem, user = allUsers.find { it.uid == queueItem.userId })
        }
        val nextPatient = activeQueues.find { it.status == QueueStatus.MENUNGGU }
        val nextQueueNumberString = nextPatient?.queueNumber?.toString() ?: "-"
        val totalWaitingInQueue = activeQueues.size

        DoctorUiState(
            greeting = getGreetingBasedOnTime(),
            doctorName = doctorUser?.name ?: "Dokter",
            topQueueList = topThreeQueues,
            practiceStatus = statuses[doctorId],
            isLoading = false,
            waitingInQueue = totalWaitingInQueue,
            nextQueueNumber = nextQueueNumberString,
            selectedPatient = selectedPatient,
            todaySchedule = todaySchedule // <-- MASUKKAN DATA JADWAL KE STATE
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DoctorUiState()
    )

    private fun getGreetingBasedOnTime(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Selamat Pagi"
            in 12..13 -> "Selamat Siang"
            in 14..17 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }

    fun selectPatient(patient: PatientQueueDetails) {
        _selectedPatient.value = patient
    }

    fun clearSelectedPatient() {
        _selectedPatient.value = null
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