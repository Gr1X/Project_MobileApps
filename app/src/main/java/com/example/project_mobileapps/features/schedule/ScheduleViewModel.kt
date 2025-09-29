package com.example.project_mobileapps.features.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.Booking
import com.example.project_mobileapps.data.repo.BookingRepository
import com.example.project_mobileapps.data.repo.DoctorRepository
import com.example.project_mobileapps.data.repo.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase



data class ScheduleUiState(
    val availableTimes: List<String> = emptyList(),
    val selectedTime: String? = null,
    val keluhan: String = "",
    val bookingStatus: String = "Idle", // Idle, Loading, Success, Error
    val isLoadingSchedule: Boolean = true
)

class ScheduleViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookingRepository = BookingRepository()
    private val doctorRepository = DoctorRepository()
    private val userRepository = UserRepository()
    private val doctorId: String = savedStateHandle.get<String>("doctorId")!!

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadAvailableSchedule()
    }

    private fun loadAvailableSchedule() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSchedule = true) }

            val allPossibleSlots = listOf("09:00", "09:30", "10:00", "10:30", "11:00", "13:00", "13:30", "14:00")
            val bookedAppointments = bookingRepository.getTodaysBookingsForDoctor(doctorId)

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val bookedTimes = bookedAppointments.mapNotNull { booking ->
                booking.appointmentTimestamp?.let { timeFormat.format(it) }
            }.toSet()

            val availableSlots = allPossibleSlots.filter { it !in bookedTimes }

            _uiState.update {
                it.copy(
                    availableTimes = availableSlots,
                    isLoadingSchedule = false
                )
            }
        }
    }

    fun onKeluhanChanged(newKeluhan: String) {
        _uiState.update { it.copy(keluhan = newKeluhan) }
    }

    fun onTimeSelected(time: String) {
        _uiState.update { it.copy(selectedTime = time, bookingStatus = "Idle") }
    }

    fun confirmBooking() {
        val selectedTimeString = _uiState.value.selectedTime ?: return
        val keluhanText = _uiState.value.keluhan.takeIf { it.isNotBlank() } ?: "Tidak ada keluhan"

        viewModelScope.launch {
            _uiState.update { it.copy(bookingStatus = "Loading") }

            // Ambil ID pengguna yang sedang login
            val userId = Firebase.auth.currentUser?.uid
            if (userId == null) {
                _uiState.update { it.copy(bookingStatus = "Error") }
                return@launch
            }

            val doctor = doctorRepository.getDoctorById(doctorId)
            val user = userRepository.getUser(userId)

            val calendar = Calendar.getInstance()
            val hour = selectedTimeString.substringBefore(":").toInt()
            val minute = selectedTimeString.substringAfter(":").toInt()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            val appointmentDate = calendar.time

            val newBooking = Booking(
                doctorId = doctorId,
                doctorName = doctor?.name ?: "N/A",
                userId = user?.uid ?: "N/A",
                userName = user?.name ?: "N/A",
                appointmentTimestamp = appointmentDate,
                keluhan = keluhanText,
                status = "Akan Datang"
            )

            val success = bookingRepository.createBooking(newBooking)
            val finalStatus = if (success) "Success" else "Error"
            _uiState.update { it.copy(bookingStatus = finalStatus) }
        }
    }
}