package com.example.project_mobileapps.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.AuthRepository // <-- Perhatikan Repository yang digunakan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true
)

class ProfileViewModel : ViewModel() {
    // Kita tidak butuh UserRepository untuk dummy
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        // Ambil user yang sedang login dari AuthRepository
        val currentUser = AuthRepository.currentUser.value
        if (currentUser != null) {
            _uiState.value = ProfileUiState(user = currentUser, isLoading = false)
        } else {
            _uiState.value = ProfileUiState(isLoading = false) // Tetap gagal jika tidak ada user
        }
    }
}