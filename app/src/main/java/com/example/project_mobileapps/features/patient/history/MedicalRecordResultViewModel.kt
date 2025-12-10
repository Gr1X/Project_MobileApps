// File: features/patient/history/MedicalRecordResultViewModel.kt
package com.example.project_mobileapps.features.patient.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MedicalRecordResultViewModel(
    private val queueRepository: QueueRepository,
    private val visitId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<MedicalRecordUiState>(MedicalRecordUiState.Loading)
    val uiState: StateFlow<MedicalRecordUiState> = _uiState.asStateFlow()

    init {
        fetchRecord()
    }

    private fun fetchRecord() {
        viewModelScope.launch {
            _uiState.value = MedicalRecordUiState.Loading
            val item = queueRepository.getQueueById(visitId)
            if (item != null) {
                _uiState.value = MedicalRecordUiState.Success(item)
            } else {
                _uiState.value = MedicalRecordUiState.Error("Data rekam medis tidak ditemukan.")
            }
        }
    }
}

sealed class MedicalRecordUiState {
    object Loading : MedicalRecordUiState()
    data class Success(val data: QueueItem) : MedicalRecordUiState()
    data class Error(val message: String) : MedicalRecordUiState()
}

class MedicalRecordResultViewModelFactory(
    private val queueRepository: QueueRepository,
    private val visitId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicalRecordResultViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MedicalRecordResultViewModel(queueRepository, visitId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}