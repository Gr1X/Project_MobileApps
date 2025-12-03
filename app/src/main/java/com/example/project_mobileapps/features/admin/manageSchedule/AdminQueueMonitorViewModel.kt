package com.example.project_mobileapps.features.admin.manageSchedule

import android.content.Context
import android.widget.Toast
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

/**
 * Data class helper to combine a [QueueItem] with its corresponding [User] details.
 * This simplifies passing rich patient data to the UI.
 *
 * @property queueItem The original queue data item.
 * @property user The full user profile of the patient in the queue, which can be null if the user is not found.
 */
data class PatientQueueDetails(
    val queueItem: com.example.project_mobileapps.data.model.QueueItem,
    val user: User?
)

/**
 * Represents the complete UI state for the [AdminQueueMonitorScreen].
 * It holds all the necessary data, processed and ready for display.
 *
 * @property isLoading True while the initial data is being loaded.
 * @property currentlyServing The patient who is currently being served (status DILAYANI).
 * @property patientCalled The patient who has been called and is expected to arrive (status DIPANGGIL).
 * @property nextInLine The next patient in line to be called (status MENUNGGU).
 * @property totalWaitingCount The total number of patients with the status MENUNGGU.
 * @property fullQueueList The complete list of patients, potentially filtered by status.
 * @property selectedFilter The currently active status filter. Null means no filter.
 * @property filterOptions The list of all possible statuses that can be used for filtering.
 * @property practiceStatus The current overall status of the doctor's practice.
 */
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

/**
 * ViewModel for the [AdminQueueMonitorScreen]. It orchestrates data flow from repositories
 * and exposes a single state object ([DoctorQueueUiState]) for the UI to observe. It also
 * handles user actions related to queue management.
 *
 * @param queueRepository Repository for queue and practice status data.
 * @param authRepository Repository for user data.
 */
class AdminQueueMonitorViewModel(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // A private mutable state flow to hold the currently selected filter status.
    private val _selectedFilter = MutableStateFlow<QueueStatus?>(null)

    private val clinicId = AppContainer.CLINIC_ID

    /**
     * The main public UI state flow.
     * It uses `combine` to reactively merge data from three different flows: the daily queue,
     * the practice status, and the selected filter. Whenever any of these sources emit a new
     * value, this block re-executes to produce a fresh, consistent UI state.
     */
    val uiState: StateFlow<DoctorQueueUiState> = combine(
        queueRepository.dailyQueuesFlow,
        queueRepository.practiceStatusFlow,
        _selectedFilter
    ) { queues, statuses, selectedFilter ->
        // Fetch all user data once to efficiently map queue items to user details.
        val allUsers = authRepository.getAllUsers()
        val detailedList = queues.map { queueItem ->
            PatientQueueDetails(
                queueItem = queueItem,
                user = allUsers.find { it.uid == queueItem.userId }
            )
        }

        // Process the detailed list to find key patients for the main action cards.
        val serving = detailedList.find { it.queueItem.status == QueueStatus.DILAYANI }
        val called = detailedList.find { it.queueItem.status == QueueStatus.DIPANGGIL }
        val next = detailedList.filter { it.queueItem.status == QueueStatus.MENUNGGU }.minByOrNull { it.queueItem.queueNumber }
        val waitingCount = queues.count { it.status == QueueStatus.MENUNGGU }

        // Apply the filter if one is selected.
        val filteredList = if (selectedFilter == null) detailedList else detailedList.filter { it.queueItem.status == selectedFilter }

        // Construct the final UI state object.
        DoctorQueueUiState(
            isLoading = false,
            currentlyServing = serving,
            patientCalled = called,
            nextInLine = next,
            totalWaitingCount = waitingCount,
            fullQueueList = filteredList,
            selectedFilter = selectedFilter,
            practiceStatus = statuses[clinicId]
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DoctorQueueUiState()
    )

    /** Triggers the repository to call the next patient in the queue. */
    fun callNextPatient() { // Hapus parameter context
        viewModelScope.launch {
            val result = queueRepository.callNextPatient(clinicId)

            if (result.isSuccess) {
                // Tampilkan Toast Sukses (Hijau)
                ToastManager.showToast("✅ Pasien berhasil dipanggil.", ToastType.SUCCESS)
            } else {
                // Tampilkan Toast Error (Merah) dengan pesan dari Exception
                val errorMsg = result.exceptionOrNull()?.message ?: "Gagal memanggil pasien."
                ToastManager.showToast(errorMsg, ToastType.ERROR)
            }
        }
    }

    /**
     * Memproses hasil scan QR Code.
     * Dipanggil dari UI ketika kamera mendeteksi kode QR valid.
     */
    fun processQrCode(qrContent: String) { // Context Dihapus
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

    /** Confirms that a called patient has arrived at the examination room. */
    fun confirmPatientArrival(queueNumber: Int) { // Hapus parameter context
        viewModelScope.launch {
            val result = queueRepository.confirmPatientArrival(queueNumber, clinicId)

            if (result.isSuccess) {
                // Tampilkan Toast Sukses (Hijau)
                ToastManager.showToast("✅ Pasien No. $queueNumber hadir.", ToastType.SUCCESS)
            } else {
                // Tampilkan Toast Error (Merah)
                val errorMsg = result.exceptionOrNull()?.message ?: "Gagal konfirmasi kehadiran."
                ToastManager.showToast(errorMsg, ToastType.ERROR)
            }
        }
    }

    /** Marks a consultation as finished, moving the patient to the history. */
    fun finishConsultation(queueNumber: Int) { // Context Dihapus
        viewModelScope.launch {
            val result = queueRepository.finishConsultation(queueNumber, clinicId)
            if (result.isSuccess) {
                ToastManager.showToast("✅ Konsultasi No. $queueNumber selesai.", ToastType.SUCCESS)
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Gagal update data."
                ToastManager.showToast("❌ Error: $msg", ToastType.ERROR)
            }
        }
    }

    /** Updates the state to filter the patient list by the given status. */
    fun filterByStatus(status: QueueStatus?) {
        _selectedFilter.value = status
    }

    /** Cancels a specific patient's queue item. */
    fun cancelPatientQueue(patientDetails: PatientQueueDetails) { // Hapus parameter context
        viewModelScope.launch {
            val result = queueRepository.cancelQueue(
                userId = patientDetails.queueItem.userId,
                doctorId = patientDetails.queueItem.doctorId
            )

            if (result.isSuccess) {
                ToastManager.showToast(
                    "✅ Antrian No. ${patientDetails.queueItem.queueNumber} dibatalkan",
                    ToastType.SUCCESS
                )
            } else {
                // Toast Error (Merah)
                val errorMsg = result.exceptionOrNull()?.message ?: "Gagal membatalkan antrian"
                ToastManager.showToast(errorMsg, ToastType.ERROR)
            }
        }
    }
}

/**
 * Factory for creating an instance of [AdminQueueMonitorViewModel].
 * This is necessary because the ViewModel has dependencies ([queueRepository], [authRepository])
 * that need to be provided during its construction.
 */
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