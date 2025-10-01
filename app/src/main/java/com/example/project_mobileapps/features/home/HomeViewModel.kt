package com.example.project_mobileapps.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.Booking
import com.example.project_mobileapps.data.model.Doctor
import com.example.project_mobileapps.data.repo.DoctorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import com.example.project_mobileapps.data.repo.BookingRepository
import com.example.project_mobileapps.data.repo.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * State ini menampung semua data yang dibutuhkan oleh HomeScreen dalam satu objek.
 */

data class HomeUiState(
    val userName: String = "",
    val upcomingAppointment: Booking? = null,
    val recentDoctors: List<Doctor> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel : ViewModel() {

    // StateFlow untuk menyimpan dan mengirimkan HomeUiState ke UI
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Inisialisasi repository
    private val doctorRepository = DoctorRepository()
    private val bookingRepository = BookingRepository()
    private val userRepository = UserRepository()

    init {
        // Otomatis memuat data saat ViewModel dibuat
        fetchAllHomeData()
    }

    private fun fetchAllHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val userId = Firebase.auth.currentUser?.uid
            if (userId == null) {
                // Handle jika tidak ada user, misal logout atau tampilkan error
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            // 1. Ambil daftar dokter (tetap sama)
            val doctorsFromRepo = doctorRepository.getAllDoctors()

            // 2. AMBIL DATA BOOKING ASLI DARI REPOSITORY
            val upcomingBooking = bookingRepository.getUpcomingBookingForUser("user123_abc")

            val user = userRepository.getUser(userId)

            // 3. Perbarui state dengan semua data baru
            _uiState.update { currentState ->
                currentState.copy(
                    userName = user?.name?:"Pengguna",
                    upcomingAppointment = upcomingBooking,
                    recentDoctors = doctorsFromRepo,
                    isLoading = false
                )
            }
        }
    }
}