package com.example.project_mobileapps.features.patient.doctorDetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.local.DailyScheduleData
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

    init {
        if (doctorId.isNotBlank()) {
            fetchDoctorById(doctorId)
            fetchDoctorSchedule(doctorId)
        }
    }

    private fun fetchDoctorById(id: String) {
        viewModelScope.launch {
            _doctor.value = repository.getDoctorById(id)
        }
    }

    val practiceStatus: StateFlow<PracticeStatus?> =
        queueRepository.practiceStatusFlow.map { it[doctorId] }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    private fun fetchDoctorSchedule(id: String) {
        viewModelScope.launch {
            _schedule.value = queueRepository.getDoctorSchedule(id)
        }
    }
}