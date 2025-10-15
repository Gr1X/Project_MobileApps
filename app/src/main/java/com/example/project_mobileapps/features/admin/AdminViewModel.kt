package com.example.project_mobileapps.features.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.launch


class AdminViewModel(private val queueRepository: QueueRepository) : ViewModel() {
    fun addManualQueue(patientName: String, complaint: String, onResult: (Result<*>) -> Unit) {
        viewModelScope.launch {
            val result = queueRepository.addManualQueue(patientName, complaint)
            onResult(result)
        }
    }
}

class AdminViewModelFactory(
    private val queueRepository: QueueRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminViewModel(queueRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}