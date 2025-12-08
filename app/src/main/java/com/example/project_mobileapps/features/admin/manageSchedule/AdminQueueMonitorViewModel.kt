// File: features/admin/manageSchedule/AdminQueueMonitorViewModel.kt
package com.example.project_mobileapps.features.admin.manageSchedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.ui.components.ToastManager
import com.example.project_mobileapps.ui.components.ToastType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PatientQueueDetails(
    val queueItem: com.example.project_mobileapps.data.model.QueueItem,
    val user: User?
)

data class DoctorQueueUiState(
    val isLoading: Boolean = true,
    val currentlyServing: PatientQueueDetails? = null,
    val patientCalled: PatientQueueDetails? = null,
    val nextInLine: PatientQueueDetails? = null,
    val totalWaitingCount: Int = 0,
    val fullQueueList: List<PatientQueueDetails> = emptyList(),
    val selectedFilter: QueueStatus? = null,
    val filterOptions: List<QueueStatus> = QueueStatus.values().toList(),
    val practiceStatus: PracticeStatus? = null
)

class AdminQueueMonitorViewModel(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val clinicId = AppContainer.CLINIC_ID
    private val _selectedFilter = MutableStateFlow<QueueStatus?>(null)

    val uiState: StateFlow<DoctorQueueUiState> = combine(
        queueRepository.dailyQueuesFlow,
        queueRepository.practiceStatusFlow,
        _selectedFilter
    ) { queues, statuses, selectedFilter ->

        // 1. AMBIL STATUS KLINIK (Supaya data waktu tunggu ter-update)
        val myPracticeStatus = statuses[clinicId]

        // 2. Mapping User
        val allUsers = authRepository.getAllUsers()
        val detailedList = queues.map { queueItem ->
            PatientQueueDetails(
                queueItem = queueItem,
                user = allUsers.find { it.uid == queueItem.userId }
            )
        }

        // 3. Filter & Sort Position
        val serving = detailedList.find { it.queueItem.status == QueueStatus.DILAYANI }
        val called = detailedList.find { it.queueItem.status == QueueStatus.DIPANGGIL }
        val next = detailedList.filter { it.queueItem.status == QueueStatus.MENUNGGU }.minByOrNull { it.queueItem.queueNumber }
        val waitingCount = queues.count { it.status == QueueStatus.MENUNGGU }

        val filteredList = if (selectedFilter == null) detailedList else detailedList.filter { it.queueItem.status == selectedFilter }

        DoctorQueueUiState(
            isLoading = false,
            currentlyServing = serving,
            patientCalled = called,
            nextInLine = next,
            totalWaitingCount = waitingCount,
            fullQueueList = filteredList,
            selectedFilter = selectedFilter,

            // Masukkan data status yang sudah diambil di atas
            practiceStatus = myPracticeStatus
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DoctorQueueUiState()
    )

    // --- ACTIONS ---

    fun callNextPatient() {
        viewModelScope.launch {
            val result = queueRepository.callNextPatient(clinicId)
            if (result.isSuccess) {
                ToastManager.showToast("✅ Pasien berhasil dipanggil.", ToastType.SUCCESS)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Gagal memanggil pasien."
                ToastManager.showToast(errorMsg, ToastType.ERROR)
            }
        }
    }

    fun processQrCode(qrContent: String) {
        viewModelScope.launch {
            val result = queueRepository.confirmArrivalByQr(qrContent)
            if (result.isSuccess) {
                ToastManager.showToast("✅ Berhasil! Pasien hadir.", ToastType.SUCCESS)
            } else {
                val msg = result.exceptionOrNull()?.message ?: "QR Invalid."
                ToastManager.showToast("❌ Gagal: $msg", ToastType.ERROR)
            }
        }
    }

    fun confirmPatientArrival(queueNumber: Int) {
        viewModelScope.launch {
            val result = queueRepository.confirmPatientArrival(queueNumber, clinicId)
            if (result.isSuccess) {
                ToastManager.showToast("✅ Pasien No. $queueNumber hadir.", ToastType.SUCCESS)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Gagal konfirmasi kehadiran."
                ToastManager.showToast(errorMsg, ToastType.ERROR)
            }
        }
    }

    fun submitMedicalRecord(
        queueId: String, weight: Double, height: Double, bp: String, temp: Double,
        physical: String, diagnosis: String, prescription: String, notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val medicalData = mapOf(
                "weightKg" to weight, "heightCm" to height, "bloodPressure" to bp,
                "temperature" to temp, "physicalExam" to physical, "diagnosis" to diagnosis,
                "prescription" to prescription, "doctorNotes" to notes
            )
            val result = queueRepository.submitMedicalRecord(queueId, medicalData)
            if (result.isSuccess) {
                ToastManager.showToast("✅ Rekam Medis Tersimpan", ToastType.SUCCESS)
                onSuccess()
            } else {
                ToastManager.showToast("❌ Gagal menyimpan data", ToastType.ERROR)
            }
        }
    }

    fun finishConsultationWithData(
        queueNumber: Int, diagnosis: String, treatment: String, prescription: String, notes: String, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val result = queueRepository.finishConsultation(
                queueNumber, clinicId, diagnosis, treatment, prescription, notes
            )
            if (result.isSuccess) {
                ToastManager.showToast("✅ Rekam Medis tersimpan.", ToastType.SUCCESS)
                onSuccess()
            } else {
                ToastManager.showToast("❌ Gagal menyimpan.", ToastType.ERROR)
            }
        }
    }

    fun filterByStatus(status: QueueStatus?) { _selectedFilter.value = status }

    fun cancelPatientQueue(patientDetails: PatientQueueDetails) {
        viewModelScope.launch {
            val result = queueRepository.cancelQueue(patientDetails.queueItem.userId, patientDetails.queueItem.doctorId)
            if (result.isSuccess) ToastManager.showToast("✅ Antrian dibatalkan", ToastType.SUCCESS)
            else ToastManager.showToast("Gagal membatalkan", ToastType.ERROR)
        }
    }
}

class AdminQueueMonitorViewModelFactory(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminQueueMonitorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminQueueMonitorViewModel(queueRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}