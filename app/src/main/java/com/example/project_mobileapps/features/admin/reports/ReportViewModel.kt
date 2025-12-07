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
    HARIAN("Harian"),
    MINGGUAN("Mingguan"),
    BULANAN("Bulanan"),
    TAHUNAN("Tahunan")
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
    val availableYears: List<Int> = listOf(Calendar.getInstance().get(Calendar.YEAR)),
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

    // Job untuk membatalkan fetch sebelumnya jika filter berubah cepat
    private var fetchJob: Job? = null

    init {
        // Load awal (Harian hari ini)
        fetchReportData()
    }

    // --- USER ACTIONS ---
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

    // --- MAIN LOGIC ---
    private fun fetchReportData() {
        // Cancel job sebelumnya jika ada
        fetchJob?.cancel()

        fetchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val currentState = _uiState.value
            val (startDate, endDate) = calculateDateRange(currentState)

            // 1. Ambil Data Antrian (History)
            val queues = queueRepository.getQueuesByDateRange(startDate, endDate)

            // 2. Ambil Data User (untuk detail nama pasien)
            val allUsers = authRepository.getAllUsers()

            // 3. Proses Data
            val processedData = processStats(queues, currentState.selectedPeriod, allUsers)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    totalPatientsServed = processedData.totalServed,
                    avgPatientsPerDay = processedData.avgPerDay,
                    avgServiceTimeMinutes = processedData.avgServiceTime,
                    cancellationRate = processedData.cancellationRate,
                    chartData = processedData.chartData,
                    uniquePatients = processedData.uniquePatients
                )
            }
        }
    }

    private fun calculateDateRange(state: ReportUiState): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY

        // Reset jam ke 00:00:00
        fun resetStart(cal: Calendar) {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        // Set jam ke 23:59:59
        fun resetEnd(cal: Calendar) {
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
        }

        val start: Date
        val end: Date

        when (state.selectedPeriod) {
            ReportPeriod.HARIAN -> {
                // Harian di sini maksudnya "Minggu Ini" (Senin - Minggu) agar grafik terlihat
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                resetStart(calendar)
                start = calendar.time

                calendar.add(Calendar.DAY_OF_YEAR, 6)
                resetEnd(calendar)
                end = calendar.time
            }
            ReportPeriod.MINGGUAN -> {
                // 1 Bulan Penuh untuk grafik mingguan (Week 1 - 4)
                calendar.set(Calendar.YEAR, state.selectedYear)
                calendar.set(Calendar.MONTH, state.selectedMonth)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                resetStart(calendar)
                start = calendar.time

                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                resetEnd(calendar)
                end = calendar.time
            }
            ReportPeriod.BULANAN, ReportPeriod.TAHUNAN -> { // Disatukan agar logikanya per tahun
                calendar.set(Calendar.YEAR, state.selectedYear)
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                resetStart(calendar)
                start = calendar.time

                calendar.set(Calendar.MONTH, 11) // Desember
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
        val chartData: List<DailyReport>,
        val uniquePatients: List<User>
    )

    private fun processStats(queues: List<QueueItem>, period: ReportPeriod, allUsers: List<User>): ProcessedStats {
        val servedQueues = queues.filter { it.status == QueueStatus.SELESAI }
        val cancelledQueues = queues.filter { it.status == QueueStatus.DIBATALKAN }

        // KPI 1: Total & Rate
        val totalServed = servedQueues.size
        val totalAll = queues.size
        val cancellationRate = if (totalAll > 0) (cancelledQueues.size.toFloat() / totalAll) * 100 else 0f

        // KPI 2: Rata-rata Waktu Layanan
        // Menghitung selisih finishedAt - startedAt
        val totalServiceTimeMs = servedQueues.sumOf {
            if (it.startedAt != null && it.finishedAt != null) {
                it.finishedAt!!.time - it.startedAt!!.time
            } else 0L
        }
        val avgServiceTime = if (totalServed > 0) TimeUnit.MILLISECONDS.toMinutes(totalServiceTimeMs / totalServed) else 0L

        // KPI 3: Grafik
        val chartData = when (period) {
            ReportPeriod.HARIAN -> {
                // Group by Day of Week
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
                // Group by Week of Month
                val weeklyCounts = servedQueues.groupBy {
                    val c = Calendar.getInstance(); c.time = it.createdAt; c.get(Calendar.WEEK_OF_MONTH)
                }
                (1..5).map { week -> DailyReport("M$week", weeklyCounts[week]?.size ?: 0) }
            }
            else -> {
                // Group by Month
                val monthlyCounts = servedQueues.groupBy {
                    val c = Calendar.getInstance(); c.time = it.createdAt; c.get(Calendar.MONTH)
                }
                val months = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
                months.mapIndexed { idx, label -> DailyReport(label, monthlyCounts[idx]?.size ?: 0) }
            }
        }

        // KPI 4: Rata-rata Pasien/Hari (Pembagi dinamis)
        val divisor = when(period) {
            ReportPeriod.HARIAN -> 7.0
            ReportPeriod.MINGGUAN -> 30.0
            else -> 365.0
        }
        val avgPerDay = totalServed / divisor

        // KPI 5: List Riwayat
        val uniquePatientIds = queues.map { it.userId }.distinct()
        val uniquePatients = allUsers.filter { it.uid in uniquePatientIds }

        return ProcessedStats(totalServed, avgPerDay, avgServiceTime, cancellationRate, chartData, uniquePatients)
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