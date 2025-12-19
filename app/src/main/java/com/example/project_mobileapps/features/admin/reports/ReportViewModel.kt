// File: features/admin/reports/ReportViewModel.kt
package com.example.project_mobileapps.features.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.DailyReport
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

enum class ReportPeriod(val displayName: String) {
    HARIAN("Harian"),     // Senin - Minggu
    MINGGUAN("Mingguan"), // W1 - W4
    BULANAN("Bulanan"),   // Jan - Des
    TAHUNAN("Tahunan")    // 2024 - 2028
}

data class ReportUiState(
    val isLoading: Boolean = true,
    val totalPatientsServed: Int = 0,
    val avgPatientsPerDay: Double = 0.0,
    val avgServiceTimeMinutes: Long = 0,
    val cancellationRate: Float = 0f,
    val chartData: List<DailyReport> = emptyList(),
    val uniquePatients: List<User> = emptyList(),
    val selectedPeriod: ReportPeriod = ReportPeriod.HARIAN,
    // Generate tahun dari 5 tahun lalu sampai 5 tahun ke depan
    val availableYears: List<Int> = ((Calendar.getInstance().get(Calendar.YEAR) - 5)..(Calendar.getInstance().get(Calendar.YEAR) + 5)).toList(),
    val availableMonths: List<String> = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember"),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
)

class ReportViewModel(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        fetchReportData()
    }

    fun setPeriod(period: ReportPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        fetchReportData()
    }

    fun setYear(year: Int) {
        _uiState.update { it.copy(selectedYear = year) }
        fetchReportData()
    }

    fun setMonth(month: Int) {
        _uiState.update { it.copy(selectedMonth = month) }
        fetchReportData()
    }

    private fun fetchReportData() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val currentState = _uiState.value
            val (startDate, endDate) = calculateDateRange(currentState)

            // 1. Ambil Data
            val queues = queueRepository.getQueuesByDateRange(startDate, endDate)
            val allUsers = authRepository.getAllUsers()

            // 2. Proses Data
            val processedData = processStats(queues, currentState.selectedPeriod, currentState.selectedYear)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    totalPatientsServed = processedData.totalServed,
                    avgPatientsPerDay = processedData.avgPerDay,
                    avgServiceTimeMinutes = processedData.avgServiceTime,
                    cancellationRate = processedData.cancellationRate,
                    chartData = processedData.chartData,
                    uniquePatients = processedData.uniquePatients(allUsers, queues)
                )
            }
        }
    }

    private fun calculateDateRange(state: ReportUiState): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY // Penting: Senin hari pertama

        fun resetStart(cal: Calendar) {
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        }
        fun resetEnd(cal: Calendar) {
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
        }

        val start: Date
        val end: Date

        when (state.selectedPeriod) {
            ReportPeriod.HARIAN -> {
                // Senin - Minggu (Minggu ini)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                resetStart(calendar)
                start = calendar.time

                calendar.add(Calendar.DAY_OF_YEAR, 6) // +6 hari = Minggu
                resetEnd(calendar)
                end = calendar.time
            }
            ReportPeriod.MINGGUAN -> {
                // 1 Bulan penuh (untuk dipecah per minggu)
                calendar.set(Calendar.YEAR, state.selectedYear)
                calendar.set(Calendar.MONTH, state.selectedMonth)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                resetStart(calendar)
                start = calendar.time

                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_YEAR, -1) // Hari terakhir bulan
                resetEnd(calendar)
                end = calendar.time
            }
            ReportPeriod.BULANAN -> {
                // 1 Tahun penuh (Jan - Des)
                calendar.set(Calendar.YEAR, state.selectedYear)
                calendar.set(Calendar.DAY_OF_YEAR, 1) // 1 Jan
                resetStart(calendar)
                start = calendar.time

                calendar.set(Calendar.MONTH, 11) // 31 Des
                calendar.set(Calendar.DAY_OF_MONTH, 31)
                resetEnd(calendar)
                end = calendar.time
            }
            ReportPeriod.TAHUNAN -> {
                // Range 5 tahun (SelectedYear - 2 sampai SelectedYear + 2)
                // Contoh: Jika pilih 2025, akan tampil 2023, 2024, 2025, 2026, 2027
                val centerYear = state.selectedYear
                calendar.set(Calendar.YEAR, centerYear - 2)
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                resetStart(calendar)
                start = calendar.time

                calendar.set(Calendar.YEAR, centerYear + 2)
                calendar.set(Calendar.MONTH, 11)
                calendar.set(Calendar.DAY_OF_MONTH, 31)
                resetEnd(calendar)
                end = calendar.time
            }
        }
        return Pair(start, end)
    }

    private data class ProcessedStats(
        val totalServed: Int,
        val avgPerDay: Double,
        val avgServiceTime: Long,
        val cancellationRate: Float,
        val chartData: List<DailyReport>
    ) {
        // Helper function untuk filter unique patients
        fun uniquePatients(allUsers: List<User>, queues: List<QueueItem>): List<User> {
            val uniqueIds = queues.map { it.userId }.distinct()
            return allUsers.filter { it.uid in uniqueIds }
        }
    }

    private fun processStats(queues: List<QueueItem>, period: ReportPeriod, selectedYear: Int): ProcessedStats {
        val servedQueues = queues.filter { it.status == QueueStatus.SELESAI }
        val cancelledQueues = queues.filter { it.status == QueueStatus.DIBATALKAN }

        // --- KPI Calculation ---
        val totalServed = servedQueues.size
        val totalAll = queues.size
        val cancellationRate = if (totalAll > 0) (cancelledQueues.size.toFloat() / totalAll) * 100 else 0f

        val totalServiceTimeMs = servedQueues.sumOf {
            if (it.startedAt != null && it.finishedAt != null) it.finishedAt!!.time - it.startedAt!!.time else 0L
        }
        val avgServiceTime = if (totalServed > 0) TimeUnit.MILLISECONDS.toMinutes(totalServiceTimeMs / totalServed) else 0L

        // --- Chart Data Calculation ---
        val chartData = when (period) {
            ReportPeriod.HARIAN -> {
                // Group by Hari (Senin - Minggu)
                val dailyCounts = servedQueues.groupBy {
                    val c = Calendar.getInstance(); c.time = it.createdAt; c.get(Calendar.DAY_OF_WEEK)
                }
                val days = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)
                val labels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

                days.mapIndexed { idx, dayInt ->
                    DailyReport(labels[idx], dailyCounts[dayInt]?.size ?: 0)
                }
            }
            ReportPeriod.MINGGUAN -> {
                // Group by Minggu ke- (W1 - W5)
                val weeklyCounts = servedQueues.groupBy {
                    val c = Calendar.getInstance(); c.time = it.createdAt; c.get(Calendar.WEEK_OF_MONTH)
                }
                (1..5).map { week -> DailyReport("W$week", weeklyCounts[week]?.size ?: 0) }
            }
            ReportPeriod.BULANAN -> {
                // Group by Bulan (Jan - Des)
                val monthlyCounts = servedQueues.groupBy {
                    val c = Calendar.getInstance(); c.time = it.createdAt; c.get(Calendar.MONTH)
                }
                val months = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
                months.mapIndexed { idx, label -> DailyReport(label, monthlyCounts[idx]?.size ?: 0) }
            }
            ReportPeriod.TAHUNAN -> {
                // Group by Tahun (2023, 2024, 2025...)
                val yearlyCounts = servedQueues.groupBy {
                    val c = Calendar.getInstance(); c.time = it.createdAt; c.get(Calendar.YEAR)
                }
                // Generate range tahun (Selected - 2 sampai Selected + 2)
                val years = (selectedYear - 2)..(selectedYear + 2)
                years.map { year ->
                    DailyReport(year.toString(), yearlyCounts[year]?.size ?: 0)
                }
            }
        }

        // Pembagi rata-rata pasien
        val divisor = when (period) {
            ReportPeriod.HARIAN -> 7.0
            ReportPeriod.MINGGUAN -> 30.0 // Approx
            ReportPeriod.BULANAN -> 365.0
            ReportPeriod.TAHUNAN -> 365.0 * 5 // Range 5 tahun
        }
        val avgPerDay = if (totalServed > 0) totalServed / divisor else 0.0

        return ProcessedStats(totalServed, avgPerDay, avgServiceTime, cancellationRate, chartData)
    }
}

class ReportViewModelFactory(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportViewModel(queueRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}