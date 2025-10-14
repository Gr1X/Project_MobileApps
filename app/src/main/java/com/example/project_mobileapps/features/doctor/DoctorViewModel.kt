package com.example.project_mobileapps.features.doctor // <-- sesuaikan nama package jika perlu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Data yang dibutuhkan oleh UI
data class DoctorUiState(
    val queueList: List<QueueItem> = emptyList(),
    val practiceStatus: PracticeStatus? = null,
    val isLoading: Boolean = true
)

class DoctorViewModel(private val queueRepository: QueueRepository) : ViewModel() {
    val uiState: StateFlow<DoctorUiState> = combine(
        queueRepository.dailyQueuesFlow,
        queueRepository.practiceStatusFlow
    ) { queues, statuses ->
        val doctorId = "doc_123"
        DoctorUiState(
            queueList = queues.filter { it.doctorId == doctorId },
            practiceStatus = statuses[doctorId],
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DoctorUiState()
    )

    fun callNextPatient() {
        viewModelScope.launch {
            val doctorId = "doc_123"
            queueRepository.callNextPatient(doctorId)
        }
    }

    fun togglePracticeStatus() {
        viewModelScope.launch {
            val doctorId = "doc_123"
            val currentStatus = uiState.value.practiceStatus?.isPracticeOpen ?: false
            queueRepository.setPracticeOpen(doctorId, !currentStatus)
        }
    }

    fun confirmArrival(queueId: Int) {
        viewModelScope.launch {
            val doctorId = "doc_123"
            queueRepository.confirmPatientArrival(queueId, doctorId)
        }
    }

    fun checkForLatePatients() {
        viewModelScope.launch {
            val doctorId = "doc_123"
            // We assume the repository can now check for late patients
            queueRepository.checkForLatePatients(doctorId)
        }
    }

    fun finishConsultation(queueId: Int) {
        viewModelScope.launch {
            val doctorId = "doc_123"
            queueRepository.finishConsultation(queueId, doctorId)
        }
    }
}

class DoctorViewModelFactory(
    private val queueRepository: QueueRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DoctorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DoctorViewModel(queueRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}