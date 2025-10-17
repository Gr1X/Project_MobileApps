// File BARU: features/admin/reports/PatientHistoryDetailViewModel.kt

package com.example.project_mobileapps.features.admin.reports

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.project_mobileapps.data.model.HistoryItem
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PatientHistoryUiState(
    val isLoading: Boolean = true,
    val patient: User? = null,
    val visitHistory: List<HistoryItem> = emptyList()
)

class PatientHistoryDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val patientId: String = savedStateHandle.get<String>("patientId")!!

    private val _uiState = MutableStateFlow(PatientHistoryUiState())
    val uiState: StateFlow<PatientHistoryUiState> = _uiState.asStateFlow()

    init {
        loadPatientHistory()
    }

    private fun loadPatientHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val patientDetails = authRepository.getAllUsers().find { it.uid == patientId }
            val allHistory = queueRepository.getVisitHistory(patientId)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    patient = patientDetails,
                    visitHistory = allHistory
                )
            }
        }
    }
}

class PatientHistoryDetailViewModelFactory(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(PatientHistoryDetailViewModel::class.java)) {
            val savedStateHandle = extras.createSavedStateHandle()
            @Suppress("UNCHECKED_CAST")
            return PatientHistoryDetailViewModel(savedStateHandle, queueRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}