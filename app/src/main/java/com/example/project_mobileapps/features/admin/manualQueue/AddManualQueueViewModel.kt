package com.example.project_mobileapps.features.admin.manualQueue

import android.content.Context
import android.widget.Toast
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

// --- HAPUS newPatientPassword DARI SINI ---
data class AddManualQueueUiState(
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val isSearching: Boolean = false,
    val selectedUser: User? = null,
    val newPatientName: String = "",
    val newPatientEmail: String = "",
    val newPatientGender: Gender = Gender.PRIA,
    val newPatientDob: String = ""
)

class AddManualQueueViewModel(
    private val authRepository: AuthRepository,
    private val queueRepository: QueueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddManualQueueUiState())
    val uiState: StateFlow<AddManualQueueUiState> = _uiState.asStateFlow()

    fun onNewPatientNameChange(name: String) {
        _uiState.update { it.copy(newPatientName = name) }
    }
    fun onNewPatientEmailChange(email: String) {
        _uiState.update { it.copy(newPatientEmail = email) }
    }

    fun onNewPatientGenderChange(gender: Gender) { _uiState.update { it.copy(newPatientGender = gender) } }
    fun onNewPatientDobChange(dob: String) { _uiState.update { it.copy(newPatientDob = dob) } }

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

    fun addQueueForSelectedUser(complaint: String, onResult: (Result<QueueItem>) -> Unit) {
        viewModelScope.launch {
            val user = _uiState.value.selectedUser
            if (user == null) {
                onResult(Result.failure(Exception("Pasien belum dipilih.")))
                return@launch
            }
            val result = queueRepository.addManualQueue(user.name, complaint)
            onResult(result)
        }
    }


    fun registerNewPatientAndAddQueue(complaint: String, onResult: (Result<QueueItem>) -> Unit) {
        viewModelScope.launch {
            val name = _uiState.value.newPatientName
            val email = _uiState.value.newPatientEmail
            val gender = _uiState.value.newPatientGender
            val dob = _uiState.value.newPatientDob

            if (name.isBlank() || email.isBlank()) {
                onResult(Result.failure(Exception("Nama dan Email harus diisi.")))
                return@launch
            }

            // Panggil register dengan data yang lebih lengkap
            val registerResult = authRepository.register(name, email, null, gender, dob)
            if (registerResult.isSuccess) {
                val newPatientUser = registerResult.getOrThrow()
                val queueResult = queueRepository.addManualQueue(newPatientUser.name, complaint)
                onResult(queueResult)
            } else {
                onResult(Result.failure(registerResult.exceptionOrNull() ?: Exception("Gagal mendaftar.")))
            }
        }
    }
}

// Factory tidak perlu diubah
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