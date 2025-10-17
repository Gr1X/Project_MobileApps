// Salin dan ganti seluruh isi file: features/admin/dashboard/AdminDashboardViewModel.kt

package com.example.project_mobileapps.features.admin.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.local.DailyScheduleData
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.flow.*
import java.util.Calendar

// UI State yang sudah disederhanakan (tanpa data laporan)
data class AdminDashboardUiState(
    val practiceStatus: PracticeStatus? = null,
    val isLoading: Boolean = true,
    val totalPatientsToday: Int = 0,
    val patientsWaiting: Int = 0,
    val patientsFinished: Int = 0,
    val top5ActiveQueue: List<QueueItem> = emptyList(),
    val doctorScheduleToday: DailyScheduleData? = null
)

class AdminDashboardViewModel(private val queueRepository: QueueRepository) : ViewModel() {

    // Hanya combine data yang dibutuhkan, tanpa filter laporan
    val uiState: StateFlow<AdminDashboardUiState> = combine(
        queueRepository.dailyQueuesFlow,
        queueRepository.practiceStatusFlow
    ) { queues, statuses ->

        val today = Calendar.getInstance()
        val isSameDay = { d1: Calendar, d2: Calendar -> d1.get(Calendar.YEAR) == d2.get(Calendar.YEAR) && d1.get(Calendar.DAY_OF_YEAR) == d2.get(Calendar.DAY_OF_YEAR) }
        val queuesToday = queues.filter { val qd = Calendar.getInstance().apply { time = it.createdAt }; isSameDay(today, qd) }

        val practiceStatus = statuses["doc_123"]
        val weeklySchedule = queueRepository.getDoctorSchedule("doc_123")
        val dayOfWeekInt = today.get(Calendar.DAY_OF_WEEK)
        val dayMapping = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
        val currentDayString = dayMapping[dayOfWeekInt - 1]
        val todaySchedule = weeklySchedule.find { it.dayOfWeek.equals(currentDayString, ignoreCase = true) }

        val activeQueues = queuesToday
            .filter { it.status in listOf(QueueStatus.DILAYANI, QueueStatus.DIPANGGIL, QueueStatus.MENUNGGU) }
            .sortedBy { it.queueNumber }
            .take(5)

        AdminDashboardUiState(
            isLoading = false,
            practiceStatus = practiceStatus,
            doctorScheduleToday = todaySchedule,
            totalPatientsToday = queuesToday.size,
            patientsWaiting = queuesToday.count { it.status in listOf(QueueStatus.MENUNGGU, QueueStatus.DIPANGGIL) },
            patientsFinished = queuesToday.count { it.status == QueueStatus.SELESAI },
            top5ActiveQueue = activeQueues
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdminDashboardUiState()
    )
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