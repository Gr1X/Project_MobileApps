// File: features/doctor/MedicalRecordViewModel.kt
package com.example.project_mobileapps.features.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.MedicalRecord
import com.example.project_mobileapps.data.model.Medicine
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.repo.MedicineRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// State Khusus untuk Layar Input Dokter
data class MedicalRecordUiState(
    val medicines: List<Medicine> = emptyList(),      // Untuk Dropdown Obat
    val patientHistory: List<MedicalRecord> = emptyList(),
    val isHistoryLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val errorMessage: String? = null
)

class MedicalRecordViewModel(
    private val queueRepository: QueueRepository,
    private val medicineRepository: MedicineRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicalRecordUiState())
    val uiState: StateFlow<MedicalRecordUiState> = _uiState.asStateFlow()

    init {
        loadMedicines() // Otomatis load obat saat layar dibuka
    }

    // 1. Ambil Data Obat untuk Dropdown
    private fun loadMedicines() {
        viewModelScope.launch {
            medicineRepository.getMedicinesFlow().collect { list ->
                _uiState.update { it.copy(medicines = list) }
            }
        }
    }

    // 2. Ambil Riwayat Pasien (Dipanggil saat tombol History diklik)
    fun loadPatientHistory(patientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isHistoryLoading = true) }
            val result = queueRepository.getPatientMedicalHistory(patientId)

            if (result.isSuccess) {
                _uiState.update {
                    // Sekarang tipe data sudah cocok!
                    it.copy(isHistoryLoading = false, patientHistory = result.getOrDefault(emptyList()))
                }
            } else {
                _uiState.update { it.copy(isHistoryLoading = false) }
            }
        }
    }

    // 3. Simpan Data Rekam Medis (Submit)
    fun submitMedicalRecord(
        queueId: String,
        data: Map<String, Any>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            val result = queueRepository.submitMedicalRecord(queueId, data)

            if (result.isSuccess) {
                _uiState.update { it.copy(isSubmitting = false, submitSuccess = true) }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Gagal menyimpan"
                    )
                }
            }
        }
    }
}

// Factory untuk membuat ViewModel ini
class MedicalRecordViewModelFactory(
    private val queueRepository: QueueRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicalRecordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MedicalRecordViewModel(
                queueRepository,
                MedicineRepository() // Kita inject Repo Obat langsung di sini
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}