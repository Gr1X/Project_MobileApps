package com.example.project_mobileapps.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.Gender
import com.example.project_mobileapps.data.model.Role
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true
)

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        // ViewModel ini sekarang akan selalu mendapat data terbaru dari AuthRepository
        viewModelScope.launch {
            AuthRepository.currentUser.collect { user ->
                _uiState.update { it.copy(user = user, isLoading = false) }
            }
        }
    }

    // --- FUNGSI BARU UNTUK MENYIMPAN DATA ---
    fun updateUser(name: String, phone: String, gender: Gender, dob: String) {
        viewModelScope.launch {
            val currentUser = _uiState.value.user ?: return@launch
            val updatedUser = currentUser.copy(
                name = name,
                phoneNumber = phone,
                gender = gender,
                dateOfBirth = dob
            )
            AuthRepository.updateUser(updatedUser)
        }
    }

    fun switchRole(newRole: Role) {
        viewModelScope.launch {
            AuthRepository.switchUserRole(newRole)
        }
    }
}