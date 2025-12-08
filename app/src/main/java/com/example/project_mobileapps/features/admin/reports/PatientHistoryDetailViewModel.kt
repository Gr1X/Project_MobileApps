// File: features/admin/reports/PatientHistoryDetailViewModel.kt
package com.example.project_mobileapps.features.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.HistoryItem
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PatientHistoryDetailViewModel(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository,
    private val patientId: String // ID Pasien dikirim via Factory
) : ViewModel() {

    // --- STATE FLOWS (WAJIB ADA) ---
    private val _historyList = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyList: StateFlow<List<HistoryItem>> = _historyList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _patientProfile = MutableStateFlow<User?>(null)
    val patientProfile: StateFlow<User?> = _patientProfile.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true

            // 1. Ambil Profil Pasien (User) - Asumsi ada fungsi getUserById di AuthRepo atau QueueRepo
            // Jika belum ada, kita skip dulu atau ambil dummy.
            // IDEALNYA: val user = authRepository.getUserById(patientId)
            // _patientProfile.value = user

            // 2. Ambil History Kunjungan
            val history = queueRepository.getVisitHistory(patientId)
            _historyList.value = history

            _isLoading.value = false
        }
    }
}

// Factory untuk inject patientId
class PatientHistoryDetailViewModelFactory(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository,
    private val patientId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PatientHistoryDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PatientHistoryDetailViewModel(queueRepository, authRepository, patientId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}