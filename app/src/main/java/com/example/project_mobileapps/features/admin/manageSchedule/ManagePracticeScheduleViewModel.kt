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
    val estimatedServiceTime: Int = 15
)

class ManagePracticeScheduleViewModel(private val queueRepository: QueueRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeScheduleUiState())
    val uiState: StateFlow<PracticeScheduleUiState> = _uiState.asStateFlow()

    init {
        loadSchedules()
    }

    private fun loadSchedules() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val schedules = queueRepository.getDoctorSchedule("doc_123")
            val practiceStatus = queueRepository.practiceStatusFlow.first()["doc_123"]
            _uiState.update {
                it.copy(
                    isLoading = false,
                    schedules = schedules,
                    estimatedServiceTime = practiceStatus?.estimatedServiceTimeInMinutes ?: 15
                )
            }
        }
    }

    fun onStatusChange(day: String, isOpen: Boolean) {
        _uiState.update { currentState ->
            val updatedSchedules = currentState.schedules.map {
                if (it.dayOfWeek == day) it.copy(isOpen = isOpen) else it
            }
            currentState.copy(schedules = updatedSchedules)
        }
    }

    fun onTimeChange(day: String, hour: Int, minute: Int, isStartTime: Boolean) {
        val formattedTime = String.format("%02d:%02d", hour, minute)
        _uiState.update { currentState ->
            val updatedSchedules = currentState.schedules.map {
                if (it.dayOfWeek == day) {
                    if (isStartTime) it.copy(startTime = formattedTime) else it.copy(endTime = formattedTime)
                } else {
                    it
                }
            }
            currentState.copy(schedules = updatedSchedules)
        }
    }

    fun onServiceTimeChange(change: Int) {
        viewModelScope.launch {
            val newTime = (_uiState.value.estimatedServiceTime + change).coerceIn(5, 60)
            _uiState.update { it.copy(estimatedServiceTime = newTime) }
            queueRepository.updateEstimatedServiceTime("doc_123", newTime)
        }
    }

    fun saveSchedule(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = queueRepository.updateDoctorSchedule("doc_123", _uiState.value.schedules)
            if (result.isSuccess) {
                Toast.makeText(context, "Jadwal berhasil disimpan!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal menyimpan jadwal.", Toast.LENGTH_SHORT).show()
            }
            _uiState.update { it.copy(isSaving = false) }
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