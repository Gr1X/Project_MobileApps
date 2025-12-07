// Salin dan ganti seluruh isi file: features/profile/ProfileViewModel.kt

package com.example.project_mobileapps.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.Gender
import com.example.project_mobileapps.data.model.Role
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
/**
 * Model data (UI State) untuk [ProfileScreen] dan [EditProfileScreen].
 * Menyimpan data pengguna yang sedang login, serta state untuk form edit profil.
 *
 * @property user Objek [User] yang sedang login, diambil dari [AuthRepository].
 * @property isLoading Menandakan apakah data awal sedang dimuat.
 * @property name State untuk field input Nama di [EditProfileScreen].
 * @property phoneNumber State untuk field input Nomor Telepon di [EditProfileScreen].
 * @property dateOfBirth State untuk field input Tanggal Lahir di [EditProfileScreen].
 * @property gender State untuk field input Jenis Kelamin di [EditProfileScreen].
 * @property nameError Pesan error untuk validasi nama.
 * @property dateOfBirthError Pesan error untuk validasi tanggal lahir (saat ini tidak dipakai).
 * @property phoneError Pesan error untuk validasi nomor telepon.
 */

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val name: String = "",
    val phoneNumber: String = "",
    val dateOfBirth: String = "",
    val gender: Gender = Gender.PRIA,
    val nameError: String? = null,
    val dateOfBirthError: String? = null,
    val phoneError: String? = null
)
/**
 * ViewModel untuk [ProfileScreen] dan [EditProfileScreen].
 * Bertanggung jawab untuk:
 * 1. Mengamati [AuthRepository.currentUser] dan mempublikasikannya ke UI.
 * 2. Menyediakan state dan event handler untuk form Edit Profil.
 * 3. Menjalankan logika validasi dan penyimpanan data (update) profil.
 * 4. Menangani logika untuk 'Role Switcher' (fitur development).
 */
class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        viewModelScope.launch {
            AuthRepository.currentUser.collect { user ->
                _uiState.update {
                    it.copy(
                        user = user,
                        isLoading = false,
                        name = user?.name ?: "",
                        phoneNumber = user?.phoneNumber ?: "",
                        dateOfBirth = user?.dateOfBirth ?: "",
                        gender = user?.gender ?: Gender.PRIA
                    )
                }
            }
        }
    }

    fun onNameChange(newName: String) {
        _uiState.update { it.copy(name = newName, nameError = null) }
    }
    fun onPhoneChange(newPhone: String) {
        if (newPhone.all { it.isDigit() }) {
            _uiState.update { it.copy(phoneNumber = newPhone, phoneError = null) }
        }
    }
    fun onDobChange(newDob: String) {
        _uiState.update { it.copy(dateOfBirth = newDob, dateOfBirthError = null) }
    }
    fun onGenderChange(newGender: Gender) {
        _uiState.update { it.copy(gender = newGender) }
    }
    /**
     * Menjalankan proses validasi dan update profil pengguna.
     * Dipanggil dari [EditProfileScreen] saat "Simpan" dikonfirmasi.
     * @return [Result.success] jika validasi lolos, [Result.failure] jika gagal.
     */
    fun updateUser(): Result<Unit> {
        val currentState = _uiState.value
        val name = currentState.name.trim()
        val phone = currentState.phoneNumber.trim()

        val isNameValid = name.isNotBlank()
        val isPhoneValid = phone.isNotBlank()

        if (!isNameValid || !isPhoneValid) {
            _uiState.update {
                it.copy(
                    nameError = if (!isNameValid) "Nama tidak boleh kosong" else null,
                    phoneError = if (!isPhoneValid) "Nomor telepon tidak boleh kosong" else null
                )
            }
            return Result.failure(Exception("Validasi gagal"))
        }

        viewModelScope.launch {
            val currentUser = _uiState.value.user ?: return@launch
            val updatedUser = currentUser.copy(
                name = name,
                phoneNumber = phone,
                gender = currentState.gender,
                dateOfBirth = currentState.dateOfBirth.trim()
            )
            AuthRepository.updateUser(updatedUser)
        }
        return Result.success(Unit)
    }
    /**
     * Memanggil fungsi 'switchUserRole' di [AuthRepository].
     * Ini adalah fitur development untuk testing.
     * @param newRole Role baru yang akan di-login-kan.
     */
}