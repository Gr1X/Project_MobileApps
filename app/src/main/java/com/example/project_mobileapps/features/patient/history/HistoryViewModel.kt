package com.example.project_mobileapps.features.profile // atau .features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.HistoryItem
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
/**
 * Model data (UI State) untuk [HistoryScreen].
 *
 * @property isLoading Menandakan apakah data riwayat sedang dimuat.
 * @property historyList Daftar [HistoryItem] yang akan ditampilkan.
 * @property errorMessage Pesan error jika terjadi kegagalan load data.
 */
// State untuk UI
data class HistoryUiState(
    val isLoading: Boolean = false,
    val historyList: List<HistoryItem> = emptyList(),
    val errorMessage: String? = null
)
/**
 * ViewModel untuk [HistoryScreen].
 * Bertanggung jawab untuk mengambil daftar riwayat kunjungan milik
 * pengguna yang sedang login saat ini.
 *
 * @param queueRepository Repository untuk mengambil data riwayat.
 * @param authRepository Repository untuk mendapatkan data user yang sedang login.
 */
class HistoryViewModel(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    /**
     * Blok inisialisasi. Dipanggil saat ViewModel dibuat.
     * Langsung memanggil [loadHistory] untuk mengambil data.
     */
    init {
        loadHistory()
    }
    /**
     * Fungsi privat untuk memuat data riwayat dari repository.
     * Dijalankan dalam [viewModelScope].
     */
    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Ambil ID user yang sedang login
                val userId = authRepository.currentUser.value?.uid ?: ""
                val history = queueRepository.getVisitHistory(userId)
                _uiState.update { it.copy(isLoading = false, historyList = history) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
}

// Factory untuk membuat ViewModel
/**
 * Factory untuk [HistoryViewModel].
 * Diperlukan untuk meng-inject [QueueRepository] dan [AuthRepository]
 * ke dalam ViewModel saat pembuatannya.
 */
class HistoryViewModelFactory(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(queueRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}