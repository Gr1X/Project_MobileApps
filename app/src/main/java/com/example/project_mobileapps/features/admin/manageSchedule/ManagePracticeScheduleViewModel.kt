package com.example.project_mobileapps.features.admin.manageSchedule

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.local.DailyScheduleData
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PracticeScheduleUiState(
    val schedules: List<DailyScheduleData> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val estimatedServiceTime: Int = 15,
    val isPracticeOpen: Boolean = false,
    val callTimeLimit: Int = 15
)

class ManagePracticeScheduleViewModel(private val queueRepository: QueueRepository) : ViewModel() {
    private val _isSaving = MutableStateFlow(false)
    private val _editedSchedules = MutableStateFlow<List<DailyScheduleData>>(emptyList())

    val uiState: StateFlow<PracticeScheduleUiState> = combine(
        queueRepository.practiceStatusFlow,
        _editedSchedules,
        _isSaving
    ) { statuses, schedules, isSaving ->
        val practiceStatus = statuses["doc_123"]
        PracticeScheduleUiState(
            schedules = schedules,
            isLoading = false,
            isSaving = isSaving,
            estimatedServiceTime = practiceStatus?.estimatedServiceTimeInMinutes ?: 15,
            callTimeLimit = practiceStatus?.patientCallTimeLimitMinutes ?: 15 // <-- AMBIL DATA DARI FLOW
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PracticeScheduleUiState(isLoading = true)
    )

    init {
        loadSchedules()
    }

    fun onCallTimeLimitChange(change: Int) {
        viewModelScope.launch {
            val newTime = (uiState.value.callTimeLimit + change).coerceIn(1, 60) // Batasi 1-60 menit
            queueRepository.updatePatientCallTimeLimit("doc_123", newTime)
        }
    }

    private fun loadSchedules() {
        viewModelScope.launch {
            _editedSchedules.value = queueRepository.getDoctorSchedule("doc_123")
        }
    }

    fun setPracticeOpen(isOpen: Boolean) {
        viewModelScope.launch {
            queueRepository.setPracticeOpen("doc_123", isOpen)
        }
    }

    fun onStatusChange(day: String, isOpen: Boolean) {
        _editedSchedules.update { currentSchedules ->
            currentSchedules.map {
                if (it.dayOfWeek == day) it.copy(isOpen = isOpen) else it
            }
        }
    }

    fun onTimeChange(day: String, hour: Int, minute: Int, isStartTime: Boolean) {
        val formattedTime = String.format("%02d:%02d", hour, minute)
        _editedSchedules.update { currentSchedules ->
            currentSchedules.map {
                if (it.dayOfWeek == day) {
                    if (isStartTime) it.copy(startTime = formattedTime) else it.copy(endTime = formattedTime)
                } else {
                    it
                }
            }
        }
    }

    fun onServiceTimeChange(change: Int) {
        viewModelScope.launch {
            val newTime = (uiState.value.estimatedServiceTime + change).coerceIn(5, 60)
            queueRepository.updateEstimatedServiceTime("doc_123", newTime)
        }
    }

    fun saveSchedule(context: Context) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = queueRepository.updateDoctorSchedule("doc_123", _editedSchedules.value)
            if (result.isSuccess) {
                Toast.makeText(context, "Jadwal berhasil disimpan!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal menyimpan jadwal.", Toast.LENGTH_SHORT).show()
            }
            _isSaving.value = false
        }
    }
}

class ManagePracticeScheduleViewModelFactory(private val queueRepository: QueueRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManagePracticeScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ManagePracticeScheduleViewModel(queueRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}