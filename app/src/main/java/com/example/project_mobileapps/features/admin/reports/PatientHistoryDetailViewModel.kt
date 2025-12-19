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
    private val patientId: String
) : ViewModel() {

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

            try {
                // 1. Ambil History Kunjungan
                val history = queueRepository.getVisitHistory(patientId)
                _historyList.value = history

                // 2. [PERBAIKAN] Ambil Profil Pasien
                // Kita ambil semua user lalu cari yang ID-nya cocok
                val allUsers = authRepository.getAllUsers()
                val user = allUsers.find { it.uid == patientId }

                if (user != null) {
                    _patientProfile.value = user
                } else {
                    // Jika user tidak ditemukan (mungkin terhapus), buat dummy agar loading hilang
                    // atau biarkan null tapi handle di UI agar tidak stuck "Memuat"
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Pastikan loading berhenti apapun yang terjadi
                _isLoading.value = false
            }
        }
    }
}

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