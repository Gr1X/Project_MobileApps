package com.example.project_mobileapps.features.admin.manualQueue

import android.util.Patterns
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

/**
 * Merepresentasikan state UI untuk layar [AddManualQueueScreen].
 * Menggabungkan state untuk pencarian, hasil pencarian, form pendaftaran, dan validasi error.
 *
 * @property searchQuery Teks yang sedang diketik pengguna di kolom pencarian.
 * @property searchResults Daftar pengguna ([User]) yang cocok dengan [searchQuery].
 * @property isSearching True jika proses pencarian sedang berlangsung.
 * @property selectedUser Pengguna yang dipilih dari [searchResults]. Jika non-null, UI akan menampilkan info pasien ini.
 * @property newPatientName State untuk input nama pada form pasien baru.
 * @property newPatientEmail State untuk input email pada form pasien baru.
 * @property newPatientPhone State untuk input nomor telepon pada form pasien baru.
 * @property newPatientGender State untuk input jenis kelamin pada form pasien baru.
 * @property newPatientDob State untuk input tanggal lahir pada form pasien baru.
 * @property nameError Pesan error validasi untuk nama.
 * @property emailError Pesan error validasi untuk email.
 * @property phoneError Pesan error validasi untuk nomor telepon.
 */
data class AddManualQueueUiState(
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val isSearching: Boolean = false,
    val selectedUser: User? = null,
    // Form state
    val newPatientName: String = "",
    val newPatientEmail: String = "",
    val newPatientPhone: String = "",
    val newPatientGender: Gender = Gender.PRIA,
    val newPatientDob: String = "",
    // Validation state
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null
)

/**
 * ViewModel untuk [AddManualQueueScreen]. Mengelola logika untuk mencari pasien yang sudah ada
 * atau mendaftarkan pasien baru, kemudian menambahkan mereka ke antrian.
 *
 * @param authRepository Repository untuk otentikasi, pencarian, dan pendaftaran pengguna.
 * @param queueRepository Repository untuk menambahkan entri antrian baru.
 */
class AddManualQueueViewModel(
    private val authRepository: AuthRepository,
    private val queueRepository: QueueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddManualQueueUiState())
    val uiState: StateFlow<AddManualQueueUiState> = _uiState.asStateFlow()

    fun onNewPatientNameChange(name: String) {
        _uiState.update { it.copy(newPatientName = name, nameError = null) }
    }
    fun onNewPatientEmailChange(email: String) {
        _uiState.update { it.copy(newPatientEmail = email, emailError = null) }
    }
    fun onNewPatientPhoneChange(phone: String) {
        if (phone.all { it.isDigit() }) {
            _uiState.update { it.copy(newPatientPhone = phone, phoneError = null) }
        }
    }
    fun onNewPatientGenderChange(gender: Gender) {
        _uiState.update { it.copy(newPatientGender = gender) }
    }
    fun onNewPatientDobChange(dob: String) {
        _uiState.update { it.copy(newPatientDob = dob) }
    }

    /**
     * Dipanggil setiap kali teks di kolom pencarian berubah.
     * Memulai proses pencarian pengguna secara asynchronous.
     *
     * @param query Teks pencarian baru.
     */
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
    /**
     * Dipanggil ketika admin memilih seorang pengguna dari hasil pencarian.
     * Mengunci UI pada info pasien yang dipilih.
     *
     * @param user Pengguna yang dipilih.
     */
    fun onUserSelected(user: User) {
        _uiState.update { it.copy(selectedUser = user, searchQuery = user.name, searchResults = emptyList()) }
    }

    /**
     * Membersihkan semua state dan mengembalikan UI ke kondisi awal.
     * Dipanggil saat admin membatalkan pilihan pasien.
     */
    fun clearSelection() {
        _uiState.update { AddManualQueueUiState() }
    }

    /**
     * Menambahkan antrian untuk pengguna yang sudah dipilih.
     *
     * @param complaint Keluhan awal pasien.
     * @param onResult Callback untuk memberitahu UI hasil dari operasi (sukses/gagal).
     */
    fun addQueueForSelectedUser(complaint: String, onResult: (Result<QueueItem>) -> Unit) {
        viewModelScope.launch {
            val user = _uiState.value.selectedUser ?: return@launch
            val result = queueRepository.addManualQueue(user.name, complaint)
            onResult(result)
        }
    }

    /**
     * Melakukan validasi, mendaftarkan pasien baru, dan kemudian menambahkannya ke antrian.
     *
     * @param complaint Keluhan awal pasien.
     * @param onResult Callback untuk memberitahu UI hasil dari operasi (sukses/gagal).
     */
    fun registerNewPatientAndAddQueue(complaint: String, onResult: (Result<QueueItem>) -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val name = state.newPatientName.trim()
            val email = state.newPatientEmail.trim()
            val phone = state.newPatientPhone.trim()

            val isNameValid = name.isNotBlank()
            val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
            val isPhoneValid = phone.isNotBlank()

            if (!isNameValid || !isEmailValid || !isPhoneValid) {
                _uiState.update {
                    it.copy(
                        nameError = if (!isNameValid) "Nama wajib diisi" else null,
                        emailError = if (!isEmailValid) "Format email tidak valid" else null,
                        phoneError = if (!isPhoneValid) "Nomor telepon wajib diisi" else null
                    )
                }
                onResult(Result.failure(Exception("Data tidak valid.")))
                return@launch
            }

            val registerResult = authRepository.register(name, email, null, state.newPatientGender, state.newPatientDob, phone)
            registerResult.fold(
                onSuccess = { newPatientUser ->
                    val queueResult = queueRepository.addManualQueue(newPatientUser.name, complaint)
                    onResult(queueResult)
                },
                onFailure = { onResult(Result.failure(it)) }
            )
        }
    }
}

/**
 * Factory untuk membuat instance [AddManualQueueViewModel].
 * Diperlukan karena ViewModel memiliki dependensi ganda.
 */
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