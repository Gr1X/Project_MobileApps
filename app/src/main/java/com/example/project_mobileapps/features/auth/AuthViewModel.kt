package com.example.project_mobileapps.features.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// State baru untuk mengelola semua state form dan error
data class AuthState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val loggedInUser: User? = null,
    val loginEmail: String = "",
    val loginPassword: String = "",
    val loginEmailError: String? = null,
    val loginPasswordError: String? = null,
    val registerName: String = "",
    val registerEmail: String = "",
    val registerPassword: String = "",
    val registerNameError: String? = null,
    val registerEmailError: String? = null,
    val registerPasswordError: String? = null
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Fungsi untuk menghandle perubahan input dari UI
    fun onLoginEmailChange(email: String) {
        _authState.update { it.copy(loginEmail = email, loginEmailError = null, error = null) }
    }
    fun onLoginPasswordChange(password: String) {
        _authState.update { it.copy(loginPassword = password, loginPasswordError = null, error = null) }
    }
    fun onRegisterNameChange(name: String) {
        _authState.update { it.copy(registerName = name, registerNameError = null, error = null) }
    }
    fun onRegisterEmailChange(email: String) {
        _authState.update { it.copy(registerEmail = email, registerEmailError = null, error = null) }
    }
    fun onRegisterPasswordChange(password: String) {
        _authState.update { it.copy(registerPassword = password, registerPasswordError = null, error = null) }
    }

    // Fungsi utama dengan logika validasi
    fun registerUser() {
        val state = _authState.value
        val name = state.registerName.trim()
        val email = state.registerEmail.trim()
        val pass = state.registerPassword.trim()

        val isNameValid = name.isNotBlank()
        val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
        val isPasswordValid = pass.length >= 6

        if (!isNameValid || !isEmailValid || !isPasswordValid) {
            _authState.update {
                it.copy(
                    registerNameError = if (!isNameValid) "Nama tidak boleh kosong" else null,
                    registerEmailError = if (!isEmailValid) "Format email tidak valid" else null,
                    registerPasswordError = if (!isPasswordValid) "Password minimal 6 karakter" else null
                )
            }
            return
        }

        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.register(name, email, pass)
            result.fold(
                onSuccess = {
                    loginUser(email = state.registerEmail, pass = state.registerPassword)
                },
                onFailure = { exception ->
                    _authState.update { it.copy(isLoading = false, error = exception.message) }
                }
            )
        }
    }

    fun loginUser(
        email: String = _authState.value.loginEmail,
        pass: String = _authState.value.loginPassword
    ) {
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()

        if (trimmedEmail.isBlank() || trimmedPass.isBlank()) {
            _authState.update {
                it.copy(
                    loginEmailError = if(trimmedEmail.isBlank()) "Email tidak boleh kosong" else null,
                    loginPasswordError = if(trimmedPass.isBlank()) "Password tidak boleh kosong" else null
                )
            }
            return
        }

        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.login(trimmedEmail, trimmedPass)
            result.fold(
                onSuccess = { user -> _authState.update { it.copy(isLoading = false, loggedInUser = user) } },
                onFailure = { exception -> _authState.update { it.copy(isLoading = false, error = exception.message) } }
            )
        }
    }

    fun resetAuthState() { _authState.value = AuthState() }
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