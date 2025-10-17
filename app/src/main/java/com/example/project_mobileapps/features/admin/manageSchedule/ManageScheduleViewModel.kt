// File: features/admin/manageSchedule/ManageScheduleViewModel.kt
package com.example.project_mobileapps.features.admin.manageSchedule

import android.content.Context
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

// ✅ 1. Definisikan UI State baru yang lebih kaya data
data class DoctorQueueUiState(
    val isLoading: Boolean = true,
    val currentlyServing: PatientQueueDetails? = null,
    val patientCalled: PatientQueueDetails? = null,
    val nextInLine: PatientQueueDetails? = null,
    val totalWaitingCount: Int = 0,
    val fullQueueList: List<PatientQueueDetails> = emptyList(),
    val selectedFilter: QueueStatus? = null,
    val filterOptions: List<QueueStatus> = QueueStatus.values().toList()
)

// Ganti nama ViewModel agar lebih sesuai, tapi file tetap sama
class ManageScheduleViewModel(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow<QueueStatus?>(null)

    // ✅ 2. Ubah total logika 'combine' untuk menyediakan data yang spesifik
    val uiState: StateFlow<DoctorQueueUiState> = combine(
        queueRepository.dailyQueuesFlow,
        _selectedFilter,
    ) { queues, selectedFilter ->

        val allUsers = authRepository.getAllUsers()

        // Buat daftar detail pasien dari semua antrian
        val detailedList = queues.map { queueItem ->
            PatientQueueDetails(
                queueItem = queueItem,
                user = allUsers.find { it.uid == queueItem.userId }
            )
        }

        // Cari pasien berdasarkan statusnya
        val serving = detailedList.find { it.queueItem.status == QueueStatus.DILAYANI }
        val called = detailedList.find { it.queueItem.status == QueueStatus.DIPANGGIL }
        val next = detailedList.filter { it.queueItem.status == QueueStatus.MENUNGGU }.minByOrNull { it.queueItem.queueNumber }
        val waitingCount = queues.count { it.status == QueueStatus.MENUNGGU }

        // Filter daftar lengkap untuk bagian bawah
        val filteredList = if (selectedFilter == null) {
            detailedList
        } else {
            detailedList.filter { it.queueItem.status == selectedFilter }
        }

        DoctorQueueUiState(
            isLoading = false,
            currentlyServing = serving,
            patientCalled = called,
            nextInLine = next,
            totalWaitingCount = waitingCount,
            fullQueueList = filteredList,
            selectedFilter = selectedFilter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DoctorQueueUiState()
    )

    // ✅ 3. Tambahkan kembali semua fungsi aksi yang relevan
    fun callNextPatient(context: Context) {
        viewModelScope.launch {
            val result = queueRepository.callNextPatient("doc_123")
            if (result.isFailure) {
                Toast.makeText(context, result.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun confirmPatientArrival(queueNumber: Int, context: Context) {
        viewModelScope.launch {
            queueRepository.confirmPatientArrival(queueNumber, "doc_123")
            Toast.makeText(context, "Pasien No. $queueNumber hadir.", Toast.LENGTH_SHORT).show()
        }
    }

    fun finishConsultation(queueNumber: Int, context: Context) {
        viewModelScope.launch {
            queueRepository.finishConsultation(queueNumber, "doc_123")
            Toast.makeText(context, "Konsultasi No. $queueNumber selesai.", Toast.LENGTH_SHORT).show()
        }
    }

    fun filterByStatus(status: QueueStatus?) {
        _selectedFilter.value = status
    }

    // Fungsi untuk membatalkan antrian (bisa digunakan dokter jika perlu)
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