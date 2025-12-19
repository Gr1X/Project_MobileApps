package com.example.project_mobileapps.features.admin.manageSchedule

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.DailyScheduleData
import com.example.project_mobileapps.data.repo.QueueRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.ui.components.ToastManager
import com.example.project_mobileapps.ui.components.ToastType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PracticeScheduleUiState(
    val schedules: List<DailyScheduleData> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val estimatedServiceTime: Int = 15,
    val isPracticeOpen: Boolean = false,
    val callTimeLimit: Int = 15
)

class ManagePracticeScheduleViewModel(private val queueRepository: QueueRepository) : ViewModel() {

    private val clinicId = AppContainer.CLINIC_ID
    private val _isSaving = MutableStateFlow(false)
    private val _editedSchedules = MutableStateFlow<List<DailyScheduleData>>(emptyList())

    val uiState: StateFlow<PracticeScheduleUiState> = combine(
        queueRepository.practiceStatusFlow,
        _editedSchedules,
        _isSaving
    ) { statuses, schedules, isSaving ->
        val practiceStatus = statuses[clinicId]
        PracticeScheduleUiState(
            schedules = schedules,
            isLoading = false,
            isSaving = isSaving,
            estimatedServiceTime = practiceStatus?.estimatedServiceTimeInMinutes ?: 15,
            callTimeLimit = practiceStatus?.patientCallTimeLimitMinutes ?: 15
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PracticeScheduleUiState(isLoading = true)
    )

    init {
        loadSchedules()
    }

    private fun loadSchedules() {
        viewModelScope.launch {
            _editedSchedules.value = queueRepository.getDoctorSchedule(clinicId)
        }
    }

    // =========================================================================
    // PERBAIKAN: MENYESUAIKAN DENGAN NAMA FUNGSI DI SCREEN LAMA ANDA
    // =========================================================================

    /**
     * 1. Mengganti onScheduleChange -> onStatusChange
     * Sesuai error: Unresolved reference: onStatusChange
     */
    fun onStatusChange(day: String, isOpen: Boolean) {
        _editedSchedules.update { currentSchedules ->
            currentSchedules.map {
                if (it.dayOfWeek == day) it.copy(isOpen = isOpen) else it
            }
        }
    }

    /**
     * 2. Menambahkan Parameter Context
     * Sesuai error: Too many arguments for public final fun saveSchedule()
     */
    fun saveSchedule(context: Context) { // <-- Parameter context ditambahkan agar tidak error
        viewModelScope.launch {
            _isSaving.value = true

            // Simpan Jadwal
            val result = queueRepository.updateDoctorSchedule(clinicId, _editedSchedules.value)

            // Simpan Data Lain (Estimasi & Limit)
            queueRepository.updateEstimatedServiceTime(clinicId, uiState.value.estimatedServiceTime)
            queueRepository.updatePatientCallTimeLimit(clinicId, uiState.value.callTimeLimit)

            if (result.isSuccess) {
                ToastManager.showToast("Jadwal Berhasil Disimpan", ToastType.SUCCESS)
            } else {
                ToastManager.showToast("Gagal menyimpan jadwal", ToastType.ERROR)
            }
            _isSaving.value = false
        }
    }

    /**
     * 3. Menambahkan onServiceTimeChange (Logic +1 / -1)
     * Sesuai error: Unresolved reference: onServiceTimeChange
     */
    fun onServiceTimeChange(change: Int) {
        val current = uiState.value.estimatedServiceTime
        val newTime = (current + change).coerceIn(5, 60)

        viewModelScope.launch {
            // Update langsung ke repo agar Flow bereaksi
            queueRepository.updateEstimatedServiceTime(clinicId, newTime)
        }
    }

    /**
     * 4. Menambahkan onCallTimeLimitChange
     * Sesuai error: Unresolved reference: onCallTimeLimitChange
     */
    // Fungsi ini dipanggil oleh TOMBOL +/-
    fun onCallTimeLimitChange(change: Int) {
        val current = uiState.value.callTimeLimit
        val newTime = (current + change).coerceIn(1, 30) // Limit 1-10 menit

        viewModelScope.launch {
            // Pastikan memanggil updatePatientCallTimeLimit
            queueRepository.updatePatientCallTimeLimit(AppContainer.CLINIC_ID, newTime)
        }
    }

    // --- FUNGSI UPDATE SLIDER (Jaga-jaga jika pakai Slider baru) ---
    fun updateServiceTime(minutes: Int) {
        viewModelScope.launch {
            queueRepository.updateEstimatedServiceTime(AppContainer.CLINIC_ID, minutes)
        }
    }

    // --- FUNGSI UPDATE JAM (Tetap) ---
    fun onTimeChange(day: String, hour: Int, minute: Int, isStartTime: Boolean) {
        val formattedTime = String.format("%02d:%02d", hour, minute)
        _editedSchedules.update { currentSchedules ->
            currentSchedules.map {
                if (it.dayOfWeek == day) {
                    if (isStartTime) it.copy(startTime = formattedTime) else it.copy(endTime = formattedTime)
                } else {
                    it
                }
            }
        }
    }

    // --- FUNGSI ISTIRAHAT (Tetap) ---
    fun onBreakStatusChange(day: String, isEnabled: Boolean) {
        _editedSchedules.update { current ->
            current.map { if (it.dayOfWeek == day) it.copy(isBreakEnabled = isEnabled) else it }
        }
    }

    fun onBreakTimeChange(day: String, hour: Int, minute: Int, isStartTime: Boolean) {
        val formattedTime = String.format("%02d:%02d", hour, minute)
        _editedSchedules.update { current ->
            current.map {
                if (it.dayOfWeek == day) {
                    if (isStartTime) it.copy(breakStartTime = formattedTime)
                    else it.copy(breakEndTime = formattedTime)
                } else it
            }
        }
    }
}

class ManagePracticeScheduleViewModelFactory(private val queueRepository: QueueRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManagePracticeScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ManagePracticeScheduleViewModel(queueRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}