package com.example.project_mobileapps.features.doctorDetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.Doctor
import com.example.project_mobileapps.data.repo.DoctorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DoctorDetailViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val repository = DoctorRepository()
    private val doctorId: String = savedStateHandle.get<String>("doctorId")!!
    private val _doctor = MutableStateFlow<Doctor?>(null)
    val doctor: StateFlow<Doctor?> = _doctor

    init {
        if (doctorId.isNotBlank()) {
            fetchDoctorById(doctorId)
        }
    }

    private fun fetchDoctorById(id: String) {
        viewModelScope.launch {
            _doctor.value = repository.getDoctorById(id)
        }
    }
}