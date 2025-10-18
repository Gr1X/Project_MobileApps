package com.example.project_mobileapps.features.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.launch

/**
 * ViewModel associated with the main admin flow.
 * Note: While this ViewModel exists, more specific logic for admin features
 * has been delegated to other ViewModels like [AdminDashboardViewModel], etc.
 *
 * @property queueRepository The repository for handling queue-related data operations.
 */
class AdminViewModel(private val queueRepository: QueueRepository) : ViewModel() {
    /**
     * Handles the business logic for adding a patient to the queue manually.
     * This function is asynchronous and uses a callback to return the result.
     *
     * @param patientName The name of the patient to be added.
     * @param complaint The initial complaint or reason for the visit.
     * @param onResult A callback function that will be invoked with the [Result] of the operation.
     */
    fun addManualQueue(patientName: String, complaint: String, onResult: (Result<*>) -> Unit) {
        viewModelScope.launch {
            val result = queueRepository.addManualQueue(patientName, complaint)
            onResult(result)
        }
    }
}

/**
 * Factory for creating an instance of [AdminViewModel].
 * This is necessary because the ViewModel has a constructor parameter ([queueRepository]),
 * and this factory handles providing that dependency during the ViewModel's creation.
 */
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