// File: features/patient/doctorDetail/DoctorDetailViewModel.kt
package com.example.project_mobileapps.features.patient.doctorDetail

import android.location.Location
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.DailyScheduleData
import com.example.project_mobileapps.data.model.Doctor
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.repo.DoctorRepository
import com.example.project_mobileapps.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DoctorDetailViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val repository = DoctorRepository()
    private val queueRepository = AppContainer.queueRepository
    private val doctorId: String = savedStateHandle.get<String>("doctorId")!!

    private val _doctor = MutableStateFlow<Doctor?>(null)
    val doctor: StateFlow<Doctor?> = _doctor

    private val _schedule = MutableStateFlow<List<DailyScheduleData>>(emptyList())
    val schedule: StateFlow<List<DailyScheduleData>> = _schedule

    // --- NEW: STATE LOKASI ---
    private val _distance = MutableStateFlow<String?>(null) // Contoh: "3.5 km"
    val distance: StateFlow<String?> = _distance

    // KOORDINAT KLINIK (Contoh: Monas Jakarta, silakan ganti sesuai lokasi asli)
    private val clinicLat = -6.175392
    private val clinicLng = 106.827153
    val clinicAddress = "Jl. Merdeka Barat No. 12, Gambir, Jakarta Pusat"

    val practiceStatus: StateFlow<PracticeStatus?> =
        queueRepository.practiceStatusFlow.map { it[doctorId] }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        if (doctorId.isNotBlank()) {
            fetchDoctorById(doctorId)
            fetchDoctorSchedule(doctorId)
        }
    }

    private fun fetchDoctorById(id: String) {
        viewModelScope.launch { _doctor.value = repository.getDoctorById(id) }
    }

    private fun fetchDoctorSchedule(id: String) {
        viewModelScope.launch { _schedule.value = queueRepository.getDoctorSchedule(id) }
    }

    // --- FUNGSI HITUNG JARAK (Native Android Feature) ---
    fun calculateDistanceToClinic(userLat: Double, userLng: Double) {
        val startPoint = Location("user")
        startPoint.latitude = userLat
        startPoint.longitude = userLng

        val endPoint = Location("clinic")
        endPoint.latitude = clinicLat
        endPoint.longitude = clinicLng

        // Hitung jarak dalam meter
        val distanceInMeters = startPoint.distanceTo(endPoint)

        // Format ke KM
        val distKm = distanceInMeters / 1000
        _distance.value = String.format("%.1f km", distKm)
    }

    // Helper untuk Intent Google Maps
    fun getMapIntentUri(): String {
        return "google.navigation:q=$clinicLat,$clinicLng"
    }
}