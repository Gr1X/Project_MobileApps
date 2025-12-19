package com.example.project_mobileapps.features.admin.manualQueue

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.Gender
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * State UI Lengkap
 */
data class AddManualQueueUiState(
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val isSearching: Boolean = false,
    val selectedUser: User? = null,

    // --- Walk-in / New Patient Form State ---
    val newPatientName: String = "",
    val newPatientEmail: String = "",
    val newPatientPhone: String = "",
    val newPatientGender: Gender = Gender.PRIA,
    val newPatientDob: String = "",

    // Validation state
    val nameError: String? = null,
    val phoneError: String? = null
    // Email error dihapus karena tidak wajib
)

class AddManualQueueViewModel(
    private val authRepository: AuthRepository,
    private val queueRepository: QueueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddManualQueueUiState())
    val uiState: StateFlow<AddManualQueueUiState> = _uiState.asStateFlow()

    // --- State Handlers ---
    fun onNewPatientNameChange(name: String) {
        _uiState.update { it.copy(newPatientName = name, nameError = null) }
    }
    fun onNewPatientEmailChange(email: String) {
        _uiState.update { it.copy(newPatientEmail = email) }
    }
    fun onNewPatientPhoneChange(phone: String) {
        // Izinkan angka, spasi, atau tanda tambah (untuk +62)
        if (phone.all { it.isDigit() || it == '+' || it == ' ' }) {
            _uiState.update { it.copy(newPatientPhone = phone, phoneError = null) }
        }
    }
    fun onNewPatientGenderChange(gender: Gender) {
        _uiState.update { it.copy(newPatientGender = gender) }
    }
    fun onNewPatientDobChange(dob: String) {
        _uiState.update { it.copy(newPatientDob = dob) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearching = true, selectedUser = null) }
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        viewModelScope.launch {
            val results = authRepository.searchUsersByName(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }
    fun onUserSelected(user: User) {
        _uiState.update { it.copy(selectedUser = user, searchQuery = user.name, searchResults = emptyList()) }
    }

    fun clearSelection() {
        _uiState.update { AddManualQueueUiState() }
    }

    // --- Aksi 1: Pasien Ditemukan (Existing User) ---
    fun addQueueForSelectedUser(complaint: String, onResult: (Result<QueueItem>) -> Unit) {
        viewModelScope.launch {
            val user = _uiState.value.selectedUser ?: return@launch
            val result = queueRepository.addQueueByUserId(user.uid, user.name, complaint)
            onResult(result)
        }
    }

    // --- Aksi 2: Pasien Baru / Walk-in (Ghost Account) ---
    fun addQueueForNewPatient(complaint: String, onResult: (Result<QueueItem>) -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val name = state.newPatientName.trim()
            val phone = state.newPatientPhone.trim()
            val gender = state.newPatientGender.name
            val dob = state.newPatientDob

            // [PERBAIKAN] Hanya Validasi Nama & HP
            // Email TIDAK divalidasi regex karena boleh kosong
            if (name.isBlank() || phone.isBlank()) {
                _uiState.update { it.copy(
                    nameError = if (name.isBlank()) "Nama wajib diisi" else null,
                    phoneError = if (phone.isBlank()) "Nomor telepon wajib diisi" else null
                ) }
                onResult(Result.failure(Exception("Nama dan Nomor HP wajib diisi.")))
                return@launch
            }

            // Panggil Repository (Pastikan Repo sudah support 5 parameter ini)
            val result = queueRepository.addManualQueue(
                patientName = name,
                complaint = complaint,
                phoneNumber = phone,
                gender = gender,
                dob = dob
            )
            onResult(result)
        }
    }
}

class AddManualQueueViewModelFactory(
    private val authRepository: AuthRepository,
    private val queueRepository: QueueRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddManualQueueViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddManualQueueViewModel(authRepository, queueRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}