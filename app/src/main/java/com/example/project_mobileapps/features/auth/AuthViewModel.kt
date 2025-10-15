package com.example.project_mobileapps.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// State baru, sekarang menyimpan data user jika sukses
data class AuthState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val loggedInUser: User? = null
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun registerUser(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState(isLoading = true)
            val result = authRepository.register(name, email, pass)
            result.fold(
                onSuccess = {
                    loginUser(email, pass)
                },
                onFailure = { exception ->
                    _authState.value = AuthState(error = exception.message)
                }
            )
        }
    }

    fun loginUser(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState(isLoading = true)
            val result = authRepository.login(email, pass)
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState(loggedInUser = user)
                },
                onFailure = { exception ->
                    _authState.value = AuthState(error = exception.message)
                }
            )
        }
    }

    fun logOutUser() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState()
    }
}

class AuthViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(AuthRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}