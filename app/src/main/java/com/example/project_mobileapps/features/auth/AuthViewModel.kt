// File: features/auth/AuthViewModel.kt
package com.example.project_mobileapps.features.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Model data (UI State) untuk layar Auth.
 * Disesuaikan untuk alur Verifikasi Email Auto-Detect.
 */
data class AuthState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val loggedInUser: User? = null,

    // Login State
    val loginEmail: String = "",
    val loginPassword: String = "",
    val loginEmailError: String? = null,
    val loginPasswordError: String? = null,

    // Register State
    val registerName: String = "",
    val registerEmail: String = "",
    val registerPassword: String = "",
    val registerPhone: String = "", // Tetap disimpan untuk data profil

    val registerNameError: String? = null,
    val registerEmailError: String? = null,
    val registerPasswordError: String? = null,
    val registerPhoneError: String? = null,

    // --- STATE VERIFIKASI EMAIL (BARU) ---
    val showVerificationDialog: Boolean = false, // Menampilkan layar tunggu
    val isVerifying: Boolean = false,            // Status sedang polling/menunggu klik
    val isVerifiedSuccess: Boolean = false       // Trigger animasi sukses (Checklist)
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // --- HANDLER INPUT ---
    fun onLoginEmailChange(v: String) {
        _authState.update { it.copy(loginEmail = v, loginEmailError = null, error = null) }
    }
    fun onLoginPasswordChange(v: String) {
        _authState.update { it.copy(loginPassword = v, loginPasswordError = null, error = null) }
    }
    fun onRegisterNameChange(v: String) {
        _authState.update { it.copy(registerName = v, registerNameError = null, error = null) }
    }
    fun onRegisterEmailChange(v: String) {
        _authState.update { it.copy(registerEmail = v, registerEmailError = null, error = null) }
    }
    fun onRegisterPasswordChange(v: String) {
        _authState.update { it.copy(registerPassword = v, registerPasswordError = null, error = null) }
    }
    fun onRegisterPhoneChange(v: String) {
        if (v.all { it.isDigit() || it == '+' }) {
            _authState.update { it.copy(registerPhone = v, registerPhoneError = null, error = null) }
        }
    }

    // --- GOOGLE SIGN IN ---
    fun handleGoogleSignIn(idToken: String) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.signInWithGoogle(idToken)
            result.fold(
                onSuccess = { user -> _authState.update { it.copy(isLoading = false, loggedInUser = user) } },
                onFailure = { error -> _authState.update { it.copy(isLoading = false, error = error.message) } }
            )
        }
    }

    // --- LOGIN BIASA ---
    fun loginUser() {
        val email = _authState.value.loginEmail.trim()
        val pass = _authState.value.loginPassword.trim()

        if (email.isBlank() || pass.isBlank()) {
            _authState.update {
                it.copy(
                    loginEmailError = if(email.isBlank()) "Email wajib diisi" else null,
                    loginPasswordError = if(pass.isBlank()) "Password wajib diisi" else null
                )
            }
            return
        }

        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.login(email, pass)
            result.fold(
                onSuccess = { user -> _authState.update { it.copy(isLoading = false, loggedInUser = user) } },
                onFailure = { exception -> _authState.update { it.copy(isLoading = false, error = exception.message) } }
            )
        }
    }

    // --- REGISTER (VERIFIKASI EMAIL AUTO-DETECT) ---
    fun registerUser() {
        val state = _authState.value
        val name = state.registerName.trim()
        val email = state.registerEmail.trim()
        val pass = state.registerPassword.trim()
        val phone = state.registerPhone.trim()

        // Validasi
        val isNameValid = name.isNotBlank()
        val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
        val isPasswordValid = pass.length >= 6

        if (!isNameValid || !isEmailValid || !isPasswordValid) {
            _authState.update {
                it.copy(
                    registerNameError = if (!isNameValid) "Nama wajib diisi" else null,
                    registerEmailError = if (!isEmailValid) "Format email salah" else null,
                    registerPasswordError = if (!isPasswordValid) "Password min 6 karakter" else null
                )
            }
            return
        }

        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }

            // Panggil Repository: Buat Akun & Kirim Email Link
            val result = authRepository.register(
                name = name,
                email = email,
                pass = pass,
                phone = phone
            )

            result.fold(
                onSuccess = {
                    // SUKSES: Tampilkan layar tunggu & Mulai Polling
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            showVerificationDialog = true, // Tampilkan VerificationWaitingScreen
                            isVerifying = true             // Flag untuk loop polling
                        )
                    }
                    startVerificationCheck() // Mulai cek status di background
                },
                onFailure = { e ->
                    _authState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    // --- LOGIKA POLLING (MAGIC LOGIN) ---
    private fun startVerificationCheck() {
        viewModelScope.launch {
            // Loop selama status isVerifying masih true
            while (_authState.value.isVerifying) {
                delay(3000) // Cek setiap 3 detik

                val user = authRepository.reloadUser() // Cek ke Firebase apakah sudah verified
                if (user != null) {
                    // USER SUDAH KLIK LINK!
                    _authState.update {
                        it.copy(
                            isVerifying = false,       // Stop looping
                            isVerifiedSuccess = true,  // Trigger Animasi Centang
                            loggedInUser = user        // Data user siap
                        )
                    }
                    break // Keluar loop
                }
            }
        }
    }

    // Dipanggil jika user menekan "Kembali" atau "Salah Email" di layar tunggu
    fun stopVerificationCheck() {
        _authState.update {
            it.copy(
                isVerifying = false,
                showVerificationDialog = false,
                isVerifiedSuccess = false
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