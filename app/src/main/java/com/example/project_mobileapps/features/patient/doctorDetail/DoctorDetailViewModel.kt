package com.example.project_mobileapps.features.patient.doctorDetail

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
/**
 * ViewModel untuk [DoctorDetailScreen].
 * Bertanggung jawab untuk mengambil data profil, jadwal, dan status praktik
 * dari seorang dokter berdasarkan `doctorId` yang diterima melalui navigasi.
 *
 * @param savedStateHandle Handle untuk mengakses argumen navigasi (dalam hal ini `doctorId`).
 */
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

    init {
        if (doctorId.isNotBlank()) {
            fetchDoctorById(doctorId)
            fetchDoctorSchedule(doctorId)
        }
    }
    /**
     * Mengambil data profil dokter dari [DoctorRepository] secara asinkron.
     * @param id ID dokter yang akan diambil datanya.
     */
    private fun fetchDoctorById(id: String) {
        viewModelScope.launch {
            _doctor.value = repository.getDoctorById(id)
        }
    }
    /**
     * [StateFlow] yang diekspos ke UI untuk status praktik.
     * Aliran ini *berasal* (derived) dari `practiceStatusFlow` milik [queueRepository].
     * Menggunakan `.map` untuk mem-filter status hanya untuk `doctorId` yang relevan.
     * `.stateIn` mengubahnya menjadi StateFlow yang efisien.
     */
    val practiceStatus: StateFlow<PracticeStatus?> =
        queueRepository.practiceStatusFlow.map { it[doctorId] }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    /**
     * Mengambil data jadwal mingguan dokter dari [queueRepository] secara asinkron.
     * @param id ID dokter yang jadwalnya akan diambil.
     */
    private fun fetchDoctorSchedule(id: String) {
        viewModelScope.launch {
            _schedule.value = queueRepository.getDoctorSchedule(id)
        }
    }
}