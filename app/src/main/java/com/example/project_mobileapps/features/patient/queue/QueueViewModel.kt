package com.example.project_mobileapps.features.patient.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.local.DailyScheduleData
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

// --- PERUBAHAN 1: Tambahkan properti untuk jam praktik hari ini ---
data class QueueUiState(
    val myQueueItem: QueueItem? = null,
    val practiceStatus: PracticeStatus? = null,
    val queuesAhead: Int = 0,
    val estimatedWaitTime: Int = 0,
    val availableSlots: Int = 0,
    val upcomingQueues: List<QueueItem> = emptyList(),
    val activeQueueCount: Int = 0,
    val todaySchedule: DailyScheduleData? = null // <-- TAMBAHKAN INI
)

class QueueViewModel(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // --- PERUBAHAN 2: Ubah cara 'uiState' dibuat agar bisa mengambil jadwal ---
    private val _uiState = MutableStateFlow(QueueUiState())
    val uiState: StateFlow<QueueUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                queueRepository.dailyQueuesFlow,
                queueRepository.practiceStatusFlow,
                authRepository.currentUser
            ) { queues, statuses, currentUser ->
                // Kumpulkan data sementara
                Triple(queues, statuses, currentUser)
            }.collect { (queues, statuses, currentUser) ->
                // Sekarang kita berada di dalam coroutine dan bisa memanggil fungsi suspend
                val doctorId = "doc_123" // Asumsi dokter tunggal
                val weeklySchedule = queueRepository.getDoctorSchedule(doctorId)

                val calendar = Calendar.getInstance()
                val dayOfWeekInt = calendar.get(Calendar.DAY_OF_WEEK)
                val dayMapping = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
                val currentDayString = dayMapping[dayOfWeekInt - 1]
                val todaySchedule = weeklySchedule.find { it.dayOfWeek.equals(currentDayString, ignoreCase = true) }

                // Logika yang sudah ada sebelumnya
                val myQueue = queues.find { it.userId == currentUser?.uid && it.status == QueueStatus.MENUNGGU }
                val status = myQueue?.let { statuses[it.doctorId] } ?: statuses.values.firstOrNull()
                val totalNonCancelledQueues = queues.count { it.status != QueueStatus.DIBATALKAN }
                val slotsLeft = (status?.dailyPatientLimit ?: 0) - totalNonCancelledQueues
                val queuesAhead = if (myQueue != null && status != null) {
                    queues.count { item ->
                        item.queueNumber > (status.currentServingNumber.takeIf { it > 0 } ?: 0) &&
                                item.queueNumber < myQueue.queueNumber &&
                                (item.status == QueueStatus.MENUNGGU || item.status == QueueStatus.DIPANGGIL)
                    }
                } else 0
                val upcoming = queues.filter { it.status == QueueStatus.MENUNGGU || it.status == QueueStatus.DIPANGGIL || it.status == QueueStatus.DILAYANI }
                val activeQueueCount = upcoming.size
                val estimatedWaitTime = queuesAhead * (status?.estimatedServiceTimeInMinutes ?: 0)

                // Update state dengan semua data yang sudah lengkap
                _uiState.update {
                    it.copy(
                        myQueueItem = myQueue,
                        practiceStatus = status,
                        queuesAhead = queuesAhead,
                        estimatedWaitTime = estimatedWaitTime,
                        availableSlots = slotsLeft.coerceAtLeast(0),
                        upcomingQueues = upcoming,
                        activeQueueCount = activeQueueCount,
                        todaySchedule = todaySchedule // <-- KIRIM JADWAL HARI INI KE UI
                    )
                }
            }
        }
    }
    // --------------------------------------------------------------------

    fun cancelMyQueue() {
        viewModelScope.launch {
            val myQueue = uiState.value.myQueueItem
            val userId = authRepository.currentUser.value?.uid
            if (myQueue != null && userId != null) {
                queueRepository.cancelQueue(userId, myQueue.doctorId)
            }
        }
    }
}

// Factory tidak perlu diubah
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