// File: features/admin/manageSchedule/ManageScheduleViewModel.kt
package com.example.project_mobileapps.features.admin.manageSchedule

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PatientQueueDetails(
    val queueItem: com.example.project_mobileapps.data.model.QueueItem,
    val user: User?
)

// =======================================================
// 1. Sederhanakan UI State
// =======================================================
data class ManageScheduleUiState(
    // Hanya ada satu list untuk ditampilkan di UI
    val patientQueueList: List<PatientQueueDetails> = emptyList(),
    val selectedPatient: PatientQueueDetails? = null,
    val isLoading: Boolean = true,
    val selectedFilter: QueueStatus? = null, // null berarti "Semua"
    val filterOptions: List<QueueStatus> = QueueStatus.values().toList()
)

class ManageScheduleViewModel(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // =======================================================
    // 2. Pisahkan State Internal (Bahan Mentah)
    // =======================================================
    private val _selectedFilter = MutableStateFlow<QueueStatus?>(null)
    private val _selectedPatient = MutableStateFlow<PatientQueueDetails?>(null)

    // =======================================================
    // 3. Gabungkan Semua Sumber Data untuk Membuat uiState (Kue Jadi)
    // =======================================================
    val uiState: StateFlow<ManageScheduleUiState> = combine(
        queueRepository.dailyQueuesFlow, // Sumber data 1: Daftar antrian
        _selectedFilter,                 // Sumber data 2: Filter yang dipilih
        _selectedPatient                 // Sumber data 3: Pasien yang dipilih
    ) { queues, selectedFilter, selectedPatient ->

        val allUsers = authRepository.getAllUsers()
        val detailedList = queues.map { queueItem ->
            PatientQueueDetails(
                queueItem = queueItem,
                user = allUsers.find { it.uid == queueItem.userId }
            )
        }

        val filteredList = if (selectedFilter == null) {
            detailedList
        } else {
            detailedList.filter { it.queueItem.status == selectedFilter }
        }

        // Buat UiState final dari semua data yang sudah diolah
        ManageScheduleUiState(
            patientQueueList = filteredList,
            isLoading = false,
            selectedFilter = selectedFilter,
            selectedPatient = selectedPatient
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ManageScheduleUiState() // State awal saat loading
    )

    // Blok init sekarang kosong, semua sudah deklaratif
    init {}

    // =======================================================
    // 4. Perbarui Fungsi untuk Mengubah State Internal
    // =======================================================
    fun filterByStatus(status: QueueStatus?) {
        _selectedFilter.value = status
    }

    fun selectPatient(patient: PatientQueueDetails) {
        _selectedPatient.value = patient
    }

    fun clearSelectedPatient() {
        _selectedPatient.value = null
    }

    fun cancelPatientQueue(patientDetails: PatientQueueDetails, context: Context) {
        viewModelScope.launch {
            val result = queueRepository.cancelQueue(
                userId = patientDetails.queueItem.userId,
                doctorId = patientDetails.queueItem.doctorId
            )
            if (result.isSuccess) {
                Toast.makeText(context, "Antrian No. ${patientDetails.queueItem.queueNumber} dibatalkan", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal membatalkan antrian", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


class ManageScheduleViewModelFactory(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManageScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ManageScheduleViewModel(queueRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}