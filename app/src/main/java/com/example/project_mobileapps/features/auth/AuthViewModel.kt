package com.example.project_mobileapps.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val userRepository = UserRepository()

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState

    fun registerUser(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState(isLoading = true)
            try {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    val newUser = User(
                        uid = firebaseUser.uid,
                        name = name,
                        email = email
                    )
                    // Simpan info tambahan (nama) ke Firestore
                    userRepository.createUser(newUser)
                    _authState.value = AuthState(isSuccess = true)
                } else {
                    _authState.value = AuthState(error = "Gagal membuat user, coba lagi.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState(error = e.message ?: "Terjadi error tidak diketahui")
            }
        }
    }

    fun loginUser(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState(isLoading = true)
            try {
                // Panggil fungsi signIn dari Firebase Auth
                auth.signInWithEmailAndPassword(email, pass).await()
                _authState.value = AuthState(isSuccess = true)
            } catch (e: Exception) {
                _authState.value = AuthState(error = e.message ?: "Login Gagal. Periksa email dan password.")
            }
        }
    }

    // Tambahkan juga fungsi ini untuk mereset state saat pindah halaman
    fun resetAuthState() {
        _authState.value = AuthState()
    }
}