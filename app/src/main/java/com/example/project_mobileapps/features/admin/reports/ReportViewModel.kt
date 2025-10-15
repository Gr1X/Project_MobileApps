// File: features/admin/reports/ReportViewModel.kt
package com.example.project_mobileapps.features.admin.reports

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.local.DailyReport
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

enum class ReportRange(val days: Int, val displayName: String) {
    TODAY(1, "Hari Ini"),
    LAST_7_DAYS(7, "7 Hari Terakhir"),
    LAST_30_DAYS(30, "30 Hari Terakhir")
}

data class ReportUiState(
    val selectedRange: ReportRange = ReportRange.LAST_7_DAYS,
    val isLoading: Boolean = true,
    val totalPatientsServed: Int = 0,
    val avgPatientsPerDay: Double = 0.0,
    val avgServiceTimeMinutes: Long = 0,
    val cancellationRate: Float = 0f,
    val busiestDay: String = "-",
    val dailyPatientTrend: List<DailyReport> = emptyList(),
    val peakHoursDistribution: List<Pair<String, Int>> = emptyList(),
    val detailedQueues: List<QueueItem> = emptyList()
)

class ReportViewModel(private val queueRepository: QueueRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private val _selectedRange = MutableStateFlow(ReportRange.LAST_7_DAYS)

    init {
        viewModelScope.launch {
            combine(
                queueRepository.dailyQueuesFlow,
                _selectedRange
            ) { allQueues, range ->
                processReportData(allQueues, range)
            }.collect { newState ->
                _uiState.update { newState }
            }
        }
    }

    fun setReportRange(range: ReportRange) {
        _selectedRange.value = range
    }

    private fun processReportData(queues: List<QueueItem>, range: ReportRange): ReportUiState {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, -(range.days - 1))
        val startDate = calendar.time
        val filteredQueues = queues.filter { it.createdAt.after(startDate) }
        val servedQueues = filteredQueues.filter { it.status == QueueStatus.SELESAI }
        val totalServed = servedQueues.size
        val avgPatients = if (range.days > 0) totalServed.toDouble() / range.days else 0.0
        val avgServiceTime = servedQueues
            .filter { it.startedAt != null && it.finishedAt != null }
            .map { TimeUnit.MILLISECONDS.toMinutes(it.finishedAt!!.time - it.startedAt!!.time) }
            .average()
            .toLong()

        val totalCancelled = filteredQueues.count { it.status == QueueStatus.DIBATALKAN }
        val cancellationRate = if (filteredQueues.isNotEmpty()) {
            (totalCancelled.toFloat() / filteredQueues.size) * 100
        } else 0f

        val dailyCounts = filteredQueues
            .groupBy {
                val cal = Calendar.getInstance().apply { time = it.createdAt }
                cal.get(Calendar.DAY_OF_WEEK)
            }
            .mapValues { it.value.size }

        val dayNames = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
        val dailyTrend = dayNames.mapIndexed { index, dayName ->
            DailyReport(dayName, dailyCounts[index + 1] ?: 0)
        }

        val busiestDayName = dailyCounts.maxByOrNull { it.value }?.key?.let { dayIndex ->
            dayNames.getOrElse(dayIndex - 1) { "-" }
        } ?: "-"

        val hourlyCounts = filteredQueues
            .groupBy {
                val cal = Calendar.getInstance().apply { time = it.createdAt }
                cal.get(Calendar.HOUR_OF_DAY)
            }
            .mapValues { it.value.size }

        val peakHoursData = (8..17).map { hour ->
            val hourString = String.format("%02d:00", hour)
            hourString to (hourlyCounts[hour] ?: 0)
        }

        return ReportUiState(
            selectedRange = range,
            isLoading = false,
            totalPatientsServed = totalServed,
            avgPatientsPerDay = avgPatients,
            avgServiceTimeMinutes = avgServiceTime,
            cancellationRate = cancellationRate,
            busiestDay = busiestDayName,
            dailyPatientTrend = dailyTrend,
            peakHoursDistribution = peakHoursData,
            detailedQueues = filteredQueues
        )
    }
}


class ReportViewModelFactory(private val queueRepository: QueueRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportViewModel(queueRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}