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

    fun switchRole(newRole: Role) {
        viewModelScope.launch {
            AuthRepository.switchUserRole(newRole)
        }
    }
}