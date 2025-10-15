// File: features/doctor/DoctorViewModel.kt
package com.example.project_mobileapps.features.doctor

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.local.DailyReport
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import com.example.project_mobileapps.features.admin.manageSchedule.PatientQueueDetails
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class DoctorUiState(
    val greeting: String = "Selamat Datang",
    val doctorName: String = "Dokter",
    val queueList: List<PatientQueueDetails> = emptyList(),
    val practiceStatus: PracticeStatus? = null,
    val isLoading: Boolean = true,
    val totalPatientsToday: Int = 0,
    val patientsWaiting: Int = 0,
    val patientsFinished: Int = 0,
    val selectedPatient: PatientQueueDetails? = null
)

class DoctorViewModel(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // State terpisah untuk pasien yang dipilih
    private val _selectedPatient = MutableStateFlow<PatientQueueDetails?>(null)

    // uiState sekarang adalah gabungan dari SEMUA sumber data
    val uiState: StateFlow<DoctorUiState> = combine(
        queueRepository.dailyQueuesFlow,
        queueRepository.practiceStatusFlow,
        authRepository.currentUser,
        _selectedPatient // Tambahkan ini sebagai sumber data keempat
    ) { queues, statuses, doctorUser, selectedPatient -> // Sekarang ada 4 parameter

        val doctorId = "doc_123"
        val allUsers = authRepository.getAllUsers()

        // Buat daftar detail dari awal
        val detailedQueueList = queues
            .filter { it.doctorId == doctorId }
            .map { queueItem ->
                PatientQueueDetails(
                    queueItem = queueItem,
                    user = allUsers.find { it.uid == queueItem.userId }
                )
            }

        val total = detailedQueueList.count { it.queueItem.status != QueueStatus.DIBATALKAN }
        val waiting = detailedQueueList.count { it.queueItem.status == QueueStatus.MENUNGGU || it.queueItem.status == QueueStatus.DIPANGGIL }
        val finished = detailedQueueList.count { it.queueItem.status == QueueStatus.SELESAI }

        // Buat UiState yang baru dari hasil combine
        DoctorUiState(
            greeting = getGreetingBasedOnTime(),
            doctorName = doctorUser?.name ?: "Dokter",
            queueList = detailedQueueList, // <-- Gunakan list yang sudah detail
            practiceStatus = statuses[doctorId],
            isLoading = false,
            totalPatientsToday = total,
            patientsWaiting = waiting,
            patientsFinished = finished,
            selectedPatient = selectedPatient // <-- Masukkan pasien yang dipilih
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DoctorUiState()
    )

    // Blok init sekarang kosong
    init {}

    private fun getGreetingBasedOnTime(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Selamat Pagi"
            in 12..13 -> "Selamat Siang"
            in 14..17 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }

    // Fungsi fetchWeeklyReport tidak lagi dibutuhkan di sini karena dashboard dokter tidak menampilkannya
    // private fun fetchWeeklyReport() { ... }

    // Fungsi untuk mengubah state pasien yang dipilih
    fun selectPatient(patient: PatientQueueDetails) {
        _selectedPatient.value = patient
    }

    fun clearSelectedPatient() {
        _selectedPatient.value = null
    }

    fun callNextPatient(context: Context) {
        viewModelScope.launch {
            val doctorId = "doc_123"
            val result = queueRepository.callNextPatient(doctorId)
            if (result.isFailure) {
                Toast.makeText(context, result.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun togglePracticeStatus() {
        viewModelScope.launch {
            val currentStatus = uiState.value.practiceStatus?.isPracticeOpen ?: false
            queueRepository.setPracticeOpen("doc_123", !currentStatus)
        }
    }

    fun confirmArrival(queueId: Int) {
        viewModelScope.launch {
            queueRepository.confirmPatientArrival(queueId, "doc_123")
        }
    }

    fun finishConsultation(queueId: Int) {
        viewModelScope.launch {
            queueRepository.finishConsultation(queueId, "doc_123")
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