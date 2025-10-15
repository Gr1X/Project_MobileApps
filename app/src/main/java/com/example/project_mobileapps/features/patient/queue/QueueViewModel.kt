package com.example.project_mobileapps.features.patient.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


data class QueueUiState(
    val myQueueItem: QueueItem? = null,
    val practiceStatus: PracticeStatus? = null,
    val queuesAhead: Int = 0,
    val estimatedWaitTime: Int = 0 // dalam menit
)

class QueueViewModel(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // StateFlow untuk mengirimkan data ke UI secara real-time
    val uiState: StateFlow<QueueUiState> = combine(
        queueRepository.dailyQueuesFlow,
        queueRepository.practiceStatusFlow,
        authRepository.currentUser
    ) { queues, statuses, currentUser ->
        // Cari antrian milik user yang sedang login dan masih menunggu
        val myQueue = queues.find { it.userId == currentUser?.uid && it.status == QueueStatus.MENUNGGU }
        // Ambil status praktek dokter dari antrian tersebut
        val status = myQueue?.let { statuses[it.doctorId] }

        // Hitung jumlah pasien di depan user
        val queuesAhead = if (myQueue != null && status != null) {
            queues.count {
                it.doctorId == myQueue.doctorId &&
                        it.queueNumber > status.currentServingNumber &&
                        it.queueNumber < myQueue.queueNumber &&
                        it.status == QueueStatus.MENUNGGU
            }
        } else 0

        // Hitung estimasi waktu tunggu
        val estimatedWaitTime = queuesAhead * (status?.estimatedServiceTimeInMinutes ?: 0)

        // Buat objek state untuk UI
        QueueUiState(
            myQueueItem = myQueue,
            practiceStatus = status,
            queuesAhead = queuesAhead,
            estimatedWaitTime = estimatedWaitTime
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QueueUiState()
    )

    fun cancelMyQueue() {
        // Launch a new coroutine in the viewModelScope
        viewModelScope.launch {
            val myQueue = uiState.value.myQueueItem
            val userId = authRepository.currentUser.value?.uid
            if (myQueue != null && userId != null) {
                // Now this call is inside a coroutine, so it's valid
                queueRepository.cancelQueue(userId, myQueue.doctorId)
            }
        }
    }
}

// Factory-nya juga perlu diubah untuk menerima interface
class QueueViewModelFactory(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QueueViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QueueViewModel(queueRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}