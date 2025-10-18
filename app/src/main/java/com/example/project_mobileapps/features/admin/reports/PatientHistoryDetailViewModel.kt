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

/**
 * Merepresentasikan state UI untuk layar [PatientHistoryDetailScreen].
 *
 * @property isLoading True jika data sedang dimuat, false jika sudah siap atau terjadi error.
 * @property patient Objek [User] yang berisi detail profil pasien yang sedang dilihat.
 * @property visitHistory Daftar riwayat kunjungan ([HistoryItem]) milik pasien tersebut.
 */
data class PatientHistoryUiState(
    val isLoading: Boolean = true,
    val patient: User? = null,
    val visitHistory: List<HistoryItem> = emptyList()
)

/**
 * ViewModel untuk [PatientHistoryDetailScreen]. Bertanggung jawab untuk mengambil dan menyediakan
 * data detail seorang pasien beserta seluruh riwayat kunjungannya.
 *
 * @param savedStateHandle Handle untuk mengakses argumen yang dilewatkan melalui navigasi, dalam hal ini `patientId`.
 * @param queueRepository Repository untuk mengambil data riwayat kunjungan.
 * @param authRepository Repository untuk mengambil data detail profil pengguna.
 */
class PatientHistoryDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    // Mengambil `patientId` dari argumen navigasi. Tanda `!!` digunakan karena ID ini
    // dijamin ada saat menavigasi ke layar ini.
    private val patientId: String = savedStateHandle.get<String>("patientId")!!

    private val _uiState = MutableStateFlow(PatientHistoryUiState())
    val uiState: StateFlow<PatientHistoryUiState> = _uiState.asStateFlow()

    init {
        // Langsung memuat data saat ViewModel diinisialisasi.
        loadPatientHistory()
    }

    /**
     * Memuat detail profil dan riwayat kunjungan pasien dari repository.
     * Fungsi ini bersifat asynchronous dan mengupdate [uiState] setelah data diterima.
     */
    private fun loadPatientHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Mengambil detail profil pasien berdasarkan patientId.
            val patientDetails = authRepository.getAllUsers().find { it.uid == patientId }
            // Mengambil semua riwayat kunjungan untuk patientId yang sama.
            val allHistory = queueRepository.getVisitHistory(patientId)

            // Memperbarui state UI dengan data yang telah diambil.
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

/**
 * Factory untuk membuat instance [PatientHistoryDetailViewModel].
 * Diperlukan karena ViewModel ini memiliki dependensi dan memerlukan [SavedStateHandle]
 * yang disediakan oleh sistem navigasi.
 */
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