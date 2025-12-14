package com.example.project_mobileapps.features.admin.medicine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.Medicine
import com.example.project_mobileapps.data.repo.MedicineRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MedicineUiState(
    val medicines: List<Medicine> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ManageMedicineViewModel(private val repository: MedicineRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MedicineUiState())
    val uiState: StateFlow<MedicineUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getMedicinesFlow().collect { list ->
                _uiState.update { it.copy(medicines = list) }
            }
        }
    }

    fun addMedicine(name: String, category: String, form: String) {
        viewModelScope.launch {
            // Tidak perlu konversi stockInt lagi
            val medicine = Medicine(name = name, category = category, form = form)
            repository.addMedicine(medicine)
        }
    }

    fun deleteMedicine(id: String) {
        viewModelScope.launch {
            repository.deleteMedicine(id)
        }
    }
}

// Factory
class ManageMedicineViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManageMedicineViewModel(MedicineRepository()) as T
    }
}