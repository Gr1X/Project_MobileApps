// File: features/auth/AuthViewModel.kt
package com.example.project_mobileapps.features.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.Gender
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.project_mobileapps.ui.components.ToastManager
import com.example.project_mobileapps.ui.components.ToastType
import com.example.project_mobileapps.utils.PasswordStrength
import com.example.project_mobileapps.utils.ValidationResult
import com.example.project_mobileapps.utils.Validator

/**
 * Model data (UI State) untuk layar Auth.
 * Disesuaikan untuk alur Verifikasi Email Auto-Detect.
 */
data class AuthState(
    val isProfileIncomplete: Boolean = false,
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
    val isVerifiedSuccess: Boolean = false,

    val confirmPassword: String = "",
    val confirmPasswordError: String? = null,
    val passwordStrength: PasswordStrength = PasswordStrength.NONE,

    val isPrivacyAccepted: Boolean = false,
    val privacyError: String? = null,

    val cpFullName: String = "",
    val cpPhone: String = "",
    val cpDob: String = "",
    val cpGender: Gender = Gender.PRIA,
    val cpError: String? = null
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
        val strength = Validator.checkPasswordStrength(v)
        _authState.update {
            it.copy(
                registerPassword = v,
                registerPasswordError = null,
                passwordStrength = strength
            )
        }
    }

    fun onRegisterPhoneChange(v: String) {
        if (v.all { it.isDigit() || it == '+' }) {
            _authState.update { it.copy(registerPhone = v, registerPhoneError = null, error = null) }
        }
    }

    fun onConfirmPasswordChange(v: String) {
        _authState.update { it.copy(confirmPassword = v, confirmPasswordError = null) }
    }

    fun onPrivacyChange(isAccepted: Boolean) {
        _authState.update { it.copy(isPrivacyAccepted = isAccepted, privacyError = null) }
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
        val pass = _authState.value.loginPassword

        if (email.isBlank() || pass.isBlank()) {
            _authState.update {
                it.copy(
                    loginEmailError = if(email.isBlank()) "Wajib diisi" else null,
                    loginPasswordError = if(pass.isBlank()) "Wajib diisi" else null
                )
            }
            return
        }

        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true) }
            try {
                val result = authRepository.login(email, pass)
                result.fold(
                    onSuccess = { user ->
                        if (user != null) {
                            // User Lengkap -> Masuk Home
                            _authState.update { it.copy(isLoading = false, loggedInUser = user) }
                        } else {
                            // User Null (Auth sukses, Firestore kosong) -> Masuk Complete Profile
                            _authState.update { it.copy(isLoading = false, isProfileIncomplete = true) }
                        }
                    },
                    onFailure = { e ->
                        val msg = mapFirebaseError(e)
                        _authState.update { it.copy(isLoading = false) }
                        ToastManager.showToast(msg, ToastType.ERROR)
                    }
                )
            } catch (e: Exception) {
                _authState.update { it.copy(isLoading = false) }
                ToastManager.showToast("Koneksi gagal. Periksa internet Anda.", ToastType.ERROR)
            }
        }
    }

    fun registerInitial() {
        val state = _authState.value
        // Validasi simpel (Username, Email, Pass)
        if (state.registerName.isBlank() || state.registerEmail.isBlank() || state.registerPassword.isBlank()) {
            ToastManager.showToast("Semua kolom wajib diisi", ToastType.ERROR)
            return
        }

        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true) }
            val result = authRepository.registerInitial(
                state.registerName, // Di sini ini "Username"
                state.registerEmail,
                state.registerPassword
            )
            result.fold(
                onSuccess = {
                    _authState.update { it.copy(isLoading = false, showVerificationDialog = true, isVerifying = true) }
                    startVerificationCheck()
                },
                onFailure = { e ->
                    _authState.update { it.copy(isLoading = false) }
                    ToastManager.showToast(e.message ?: "Gagal daftar", ToastType.ERROR)
                }
            )
        }
    }

    // [NEW] Fungsi Submit Complete Profile
    // [UPDATE] Fungsi Submit dengan Validasi Akhir yang Lebih Ketat
    fun submitCompleteProfile() {
        val state = _authState.value

        // Validasi Nama
        if (state.cpFullName.trim().length < 3) {
            ToastManager.showToast("Nama terlalu pendek (min 3 huruf)", ToastType.ERROR)
            return
        }

        // Validasi No HP (Minimal 9 digit setelah +62)
        if (state.cpPhone.length < 9) {
            ToastManager.showToast("Nomor HP tidak valid (terlalu pendek)", ToastType.ERROR)
            return
        }

        if (state.cpDob.isBlank()) {
            ToastManager.showToast("Tanggal lahir wajib diisi", ToastType.ERROR)
            return
        }

        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Format No HP untuk disimpan: +62812...
        val formattedPhone = "+62${state.cpPhone}"

        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true) }
            val result = authRepository.completeUserProfile(
                uid = uid,
                fullName = state.cpFullName.trim(),
                gender = state.cpGender,
                dob = state.cpDob,
                phone = formattedPhone // Simpan dengan format internasional
            )
            // ... (kode result fold sama seperti sebelumnya)
            result.fold(
                onSuccess = { user ->
                    _authState.update { it.copy(isLoading = false, isProfileIncomplete = false, loggedInUser = user) }
                },
                onFailure = { e ->
                    _authState.update { it.copy(isLoading = false) }
                    ToastManager.showToast("Gagal simpan: ${e.message}", ToastType.ERROR)
                }
            )
        }
    }

    // Setter untuk Form Complete Profile
    fun onCpNameChange(input: String) {
        // 1. Filter: Hanya izinkan Huruf dan Spasi
        // User tidak bisa mengetik angka atau simbol aneh
        if (input.all { it.isLetter() || it.isWhitespace() }) {
            _authState.update {
                it.copy(
                    cpFullName = input,
                    // Validasi panjang minimal saat mengetik (opsional)
                    cpError = if (input.length > 50) "Nama terlalu panjang" else null
                )
            }
        }
    }
    fun onCpPhoneChange(input: String) {
        // 1. Filter: Hanya Angka
        var sanitized = input.filter { it.isDigit() }

        // 2. UX: Jika user mengetik "08...", hapus "0" di depan
        // Karena di UI sudah ada prefix "+62"
        if (sanitized.startsWith("0")) {
            sanitized = sanitized.removePrefix("0")
        }

        // 3. Limitasi: Maksimal 13 digit (standar no HP Indo tanpa 0)
        if (sanitized.length > 13) return

        _authState.update { it.copy(cpPhone = sanitized) }
    }
    fun onCpDobChange(v: String) { _authState.update { it.copy(cpDob = v) } }
    fun onCpGenderChange(v: Gender) { _authState.update { it.copy(cpGender = v) } }

    fun registerUser() {
        val state = _authState.value

        // 1. Validasi Input
        val nameResult = Validator.validateName(state.registerName)
        val emailResult = Validator.validateEmail(state.registerEmail)
        val phoneResult = Validator.validatePhone(state.registerPhone)
        val passResult = Validator.validatePassword(state.registerPassword)
        val confirmResult = Validator.validateConfirmPassword(state.registerPassword, state.confirmPassword)

        _authState.update {
            it.copy(registerNameError = null, registerEmailError = null, registerPhoneError = null, registerPasswordError = null, confirmPasswordError = null, privacyError = null)
        }

        val hasError = listOf(nameResult, emailResult, phoneResult, passResult, confirmResult).any { it is ValidationResult.Error }

        if (hasError) {
            _authState.update {
                it.copy(
                    registerNameError = (nameResult as? ValidationResult.Error)?.message,
                    registerEmailError = (emailResult as? ValidationResult.Error)?.message,
                    registerPhoneError = (phoneResult as? ValidationResult.Error)?.message,
                    registerPasswordError = (passResult as? ValidationResult.Error)?.message,
                    confirmPasswordError = (confirmResult as? ValidationResult.Error)?.message
                )
            }
            return
        }

        if (!state.isPrivacyAccepted) {
            _authState.update { it.copy(privacyError = "Anda harus menyetujui Syarat & Ketentuan") }
            return
        }

        // 2. Eksekusi
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true) }

            // Tahap 1: Auth Only (Belum simpan ke Firestore)
            val result = authRepository.registerAuthOnly(
                name = state.registerName.trim(),
                email = state.registerEmail.trim(),
                pass = state.registerPassword,
                phone = state.registerPhone.trim(),
                gender = Gender.PRIA, // Default, atau tambahkan input gender di register
                dob = "N/A"
            )

            result.fold(
                onSuccess = {
                    // Sukses Auth -> Tampilkan Dialog Tunggu -> Mulai Polling
                    _authState.update { it.copy(isLoading = false, showVerificationDialog = true, isVerifying = true) }
                    startVerificationCheck()
                },
                onFailure = { e ->
                    // Handle Error (Termasuk Email Already In Use)
                    val msg = mapFirebaseError(e)
                    _authState.update { it.copy(isLoading = false) }
                    ToastManager.showToast(msg, ToastType.ERROR)
                }
            )
        }
    }

    private fun startVerificationCheck() {
        viewModelScope.launch {
            while (_authState.value.isVerifying) {
                delay(3000)
                val isVerified = authRepository.reloadUser()
                if (isVerified) {
                    _authState.update {
                        it.copy(
                            isVerifying = false,
                            showVerificationDialog = false,
                            isProfileIncomplete = true
                        )
                    }
                    break
                }
            }
        }
    }

    private suspend fun saveUserToFirestoreAndLogin() {
        val state = _authState.value
        val firebaseUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return

        val result = authRepository.saveUserToFirestore(
            uid = firebaseUid,
            name = state.registerName.trim(),
            email = state.registerEmail.trim(),
            phone = state.registerPhone.trim(),
            gender = Gender.PRIA,
            dob = "N/A"
        )

        result.fold(
            onSuccess = { user ->
                _authState.update {
                    it.copy(
                        isVerifying = false,
                        isVerifiedSuccess = true,
                        loggedInUser = user // Trigger Navigasi Masuk
                    )
                }
            },
            onFailure = { e ->
                _authState.update { it.copy(isVerifying = false, showVerificationDialog = false) }
                ToastManager.showToast("Gagal menyimpan data: ${e.message}", ToastType.ERROR)
            }
        )
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

    // Helper untuk menerjemahkan error teknis ke bahasa manusia
    private fun mapFirebaseError(e: Throwable): String {
        val msg = e.message?.lowercase() ?: ""
        return when {
            msg.contains("network") -> "Koneksi internet bermasalah."
            msg.contains("email address is already in use") -> "Email sudah terdaftar. Silakan Login."
            msg.contains("badly formatted") -> "Format email salah."
            msg.contains("user-not-found") -> "Akun tidak ditemukan."
            msg.contains("wrong-password") -> "Password salah."
            msg.contains("email belum diverifikasi") -> "Email belum diverifikasi. Cek inbox Anda."
            else -> e.message ?: "Terjadi kesalahan."
        }
    }

    fun resetAuthState() { _authState.value = AuthState() }

    fun resetPassword(email: String, onDismissDialog: () -> Unit) {
        // 1. Validasi Email Kosong
        if (email.isBlank()) {
            ToastManager.showToast("Masukkan email Anda terlebih dahulu", ToastType.ERROR)
            return
        }

        // 2. Validasi Format Email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            ToastManager.showToast("Format email tidak valid", ToastType.ERROR)
            return
        }

        viewModelScope.launch {
            // Gunakan loading global atau buat state baru khusus dialog
            // Disini kita pakai Toast loading sederhana agar UX tidak nge-freeze
            ToastManager.showToast("Mengirim link reset...", ToastType.INFO)

            val result = authRepository.sendPasswordResetEmail(email)

            result.fold(
                onSuccess = {
                    ToastManager.showToast("Link reset terkirim ke email Anda!", ToastType.SUCCESS)
                    onDismissDialog() // Tutup dialog jika sukses
                },
                onFailure = { e ->
                    // Handle jika email tidak terdaftar atau error network
                    val msg = mapFirebaseError(e)
                    ToastManager.showToast(msg, ToastType.ERROR)
                }
            )
        }
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