package com.example.project_mobileapps.features.admin.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.local.DailyReport
import com.example.project_mobileapps.data.local.DailyScheduleData
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class AdminDashboardUiState(
    val practiceStatus: PracticeStatus? = null,
    val isLoading: Boolean = true,
    val totalPatientsToday: Int = 0,
    val patientsWaiting: Int = 0,
    val patientsFinished: Int = 0,
    val weeklyReport: List<DailyReport> = emptyList(),
    val top5ActiveQueue: List<QueueItem> = emptyList(),
    val doctorScheduleToday: DailyScheduleData? = null,
    val avgServiceTimeMinutes: Long = 0
)

class AdminDashboardViewModel(private val queueRepository: QueueRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        fetchData()
    }

    private fun fetchData() {
        viewModelScope.launch {
            combine(
                queueRepository.dailyQueuesFlow,
                queueRepository.practiceStatusFlow
            ) { queues, statuses ->
                // Buat data sementara di dalam combine
                Pair(queues, statuses)
            }.collect { (queues, statuses) ->
                // Lakukan suspend call di dalam collect
                val report = queueRepository.getWeeklyReport()
                val schedule = queueRepository.getDoctorSchedule("doc_123")

                val finishedQueuesToday = queues.filter { it.status == QueueStatus.SELESAI }
                val avgServiceTime = if (finishedQueuesToday.isNotEmpty()) {
                    finishedQueuesToday
                        .filter { it.startedAt != null && it.finishedAt != null }
                        .map { TimeUnit.MILLISECONDS.toMinutes(it.finishedAt!!.time - it.startedAt!!.time) }
                        .average()
                        .toLong()
                } else {
                    0L
                }
                val total = queues.count { it.status != QueueStatus.DIBATALKAN }
                val waiting = queues.count { it.status == QueueStatus.MENUNGGU || it.status == QueueStatus.DIPANGGIL }
                val finished = queues.count { it.status == QueueStatus.SELESAI }
                val activeQueues = queues
                    .filter { it.status != QueueStatus.SELESAI && it.status != QueueStatus.DIBATALKAN }
                    .sortedBy { it.queueNumber }

                val calendar = Calendar.getInstance()
                val dayOfWeekName = SimpleDateFormat("EEEE", Locale("id", "ID")).format(calendar.time)
                val todaySchedule = schedule.find { it.dayOfWeek.equals(dayOfWeekName, ignoreCase = true) }

                _uiState.update { currentState ->
                    currentState.copy(
                        practiceStatus = statuses["doc_123"],
                        isLoading = false,
                        totalPatientsToday = total,
                        patientsWaiting = waiting,
                        patientsFinished = finished,
                        weeklyReport = report,
                        top5ActiveQueue = activeQueues.take(5),
                        doctorScheduleToday = todaySchedule,
                        avgServiceTimeMinutes = avgServiceTime
                    )
                }
            }
        }
    }
}

class AdminDashboardViewModelFactory(
    private val queueRepository: QueueRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminDashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminDashboardViewModel(queueRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}