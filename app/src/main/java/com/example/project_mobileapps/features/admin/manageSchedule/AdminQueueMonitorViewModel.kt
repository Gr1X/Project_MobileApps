package com.example.project_mobileapps.features.admin.manageSchedule

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PatientQueueDetails(
    val queueItem: com.example.project_mobileapps.data.model.QueueItem,
    val user: User?
)

data class DoctorQueueUiState(
    val isLoading: Boolean = true,
    val currentlyServing: PatientQueueDetails? = null,
    val patientCalled: PatientQueueDetails? = null,
    val nextInLine: PatientQueueDetails? = null,
    val totalWaitingCount: Int = 0,
    val fullQueueList: List<PatientQueueDetails> = emptyList(),
    val selectedFilter: QueueStatus? = null,
    val filterOptions: List<QueueStatus> = QueueStatus.values().toList(),
    val practiceStatus: PracticeStatus? = null
)

class AdminQueueMonitorViewModel(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow<QueueStatus?>(null)

    val uiState: StateFlow<DoctorQueueUiState> = combine(
        queueRepository.dailyQueuesFlow,
        queueRepository.practiceStatusFlow,
        _selectedFilter
    ) { queues, statuses, selectedFilter ->
        val allUsers = authRepository.getAllUsers()
        val detailedList = queues.map { queueItem ->
            PatientQueueDetails(
                queueItem = queueItem,
                user = allUsers.find { it.uid == queueItem.userId }
            )
        }

        val serving = detailedList.find { it.queueItem.status == QueueStatus.DILAYANI }
        val called = detailedList.find { it.queueItem.status == QueueStatus.DIPANGGIL }
        val next = detailedList.filter { it.queueItem.status == QueueStatus.MENUNGGU }.minByOrNull { it.queueItem.queueNumber }
        val waitingCount = queues.count { it.status == QueueStatus.MENUNGGU }
        val filteredList = if (selectedFilter == null) detailedList else detailedList.filter { it.queueItem.status == selectedFilter }

        DoctorQueueUiState(
            isLoading = false,
            currentlyServing = serving,
            patientCalled = called,
            nextInLine = next,
            totalWaitingCount = waitingCount,
            fullQueueList = filteredList,
            selectedFilter = selectedFilter,
            practiceStatus = statuses["doc_123"]
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DoctorQueueUiState()
    )

    fun callNextPatient(context: Context) {
        viewModelScope.launch {
            val result = queueRepository.callNextPatient("doc_123")
            if (result.isFailure) {
                Toast.makeText(context, result.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun confirmPatientArrival(queueNumber: Int, context: Context) {
        viewModelScope.launch {
            queueRepository.confirmPatientArrival(queueNumber, "doc_123")
            Toast.makeText(context, "Pasien No. $queueNumber hadir.", Toast.LENGTH_SHORT).show()
        }
    }

    fun finishConsultation(queueNumber: Int, context: Context) {
        viewModelScope.launch {
            queueRepository.finishConsultation(queueNumber, "doc_123")
            Toast.makeText(context, "Konsultasi No. $queueNumber selesai.", Toast.LENGTH_SHORT).show()
        }
    }

    fun filterByStatus(status: QueueStatus?) {
        _selectedFilter.value = status
    }

    fun cancelPatientQueue(patientDetails: PatientQueueDetails, context: Context) {
        viewModelScope.launch {
            val result = queueRepository.cancelQueue(
                userId = patientDetails.queueItem.userId,
                doctorId = patientDetails.queueItem.doctorId
            )
            if (result.isSuccess) {
                Toast.makeText(context, "Antrian No. ${patientDetails.queueItem.queueNumber} dibatalkan", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal membatalkan antrian", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class AdminQueueMonitorViewModelFactory(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminQueueMonitorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminQueueMonitorViewModel(queueRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}