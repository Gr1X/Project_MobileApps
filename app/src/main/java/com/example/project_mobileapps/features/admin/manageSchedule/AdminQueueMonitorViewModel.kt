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

    private val _cachedUsers = MutableStateFlow<List<User>>(emptyList())

    init {
        // Ambil data user sekali saja saat layar dibuka
        refreshUserData()
    }

    private fun refreshUserData() {
        viewModelScope.launch {
            try {
                val users = authRepository.getAllUsers()
                _cachedUsers.value = users
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // [PERBAIKAN 2] Masukkan _cachedUsers ke dalam combine
    val uiState: StateFlow<DoctorQueueUiState> = combine(
        queueRepository.dailyQueuesFlow,
        queueRepository.practiceStatusFlow,
        _selectedFilter,
        _cachedUsers
    ) { queues, statuses, selectedFilter, users ->

        val myPracticeStatus = statuses[clinicId]

        // 1. Mapping User (Cepat dari Cache)
        val detailedList = queues.map { queueItem ->
            PatientQueueDetails(
                queueItem = queueItem,
                user = users.find { it.uid == queueItem.userId }
            )
        }

        // 2. Cari Posisi Penting
        val serving = detailedList.find { it.queueItem.status == QueueStatus.DILAYANI }
        val called = detailedList.find { it.queueItem.status == QueueStatus.DIPANGGIL }

        // [PERBAIKAN UTAMA DI SINI]
        // Jangan gunakan minByOrNull { queueNumber }
        // Gunakan logika yang sama dengan Repository: Sort by CreatedAt, lalu QueueNumber
        val next = detailedList
            .filter { it.queueItem.status == QueueStatus.MENUNGGU }
            .sortedWith(
                compareBy<PatientQueueDetails> { it.queueItem.createdAt } // Prioritas Waktu (Untuk menangani pasien telat)
                    .thenBy { it.queueItem.queueNumber } // Tie-Breaker jika waktu sama persis
            )
            .firstOrNull() // Ambil yang paling atas

        val waitingCount = queues.count { it.status == QueueStatus.MENUNGGU }

        val filteredList = if (selectedFilter == null) detailedList else detailedList.filter { it.queueItem.status == selectedFilter }

        DoctorQueueUiState(
            isLoading = false,
            currentlyServing = serving,
            patientCalled = called,
            nextInLine = next, // Sekarang data 'next' sudah sinkron dengan yang akan dipanggil server
            totalWaitingCount = waitingCount,
            fullQueueList = filteredList,
            selectedFilter = selectedFilter,
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
                // Opsional: Refresh user data jaga-jaga ada user baru daftar
                refreshUserData()
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
        physical: String, diagnosis: String, treatment: String,
        prescription: String, notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (diagnosis.isBlank()) {
                ToastManager.showToast("Diagnosa wajib diisi!", ToastType.ERROR)
                return@launch
            }

            val medicalData = mapOf(
                "weightKg" to weight,
                "heightCm" to height,
                "bloodPressure" to bp,
                "temperature" to temp,
                "physicalExam" to physical,
                "diagnosis" to diagnosis,
                "treatment" to treatment,
                "prescription" to prescription,
                "doctorNotes" to notes
            )

            val result = queueRepository.submitMedicalRecord(queueId, medicalData)

            if (result.isSuccess) {
                ToastManager.showToast("✅ Rekam Medis Berhasil Disimpan", ToastType.SUCCESS)
                onSuccess()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Gagal menyimpan data"
                ToastManager.showToast("❌ Gagal: $errorMsg", ToastType.ERROR)
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

    fun forceCheckLatePatients() {
        viewModelScope.launch {
            queueRepository.checkForLatePatients(clinicId)
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