// Salin dan ganti seluruh isi file: features/admin/reports/ReportViewModel.kt

package com.example.project_mobileapps.features.admin.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.local.DailyReport
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.flow.*
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

// Enum Filter Primer
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
    val availableYears: List<Int> = emptyList(),
    val availableMonths: List<String> = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember"),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
)

class ReportViewModel(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.HARIAN)
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))

    val uiState: StateFlow<ReportUiState> = combine(
        queueRepository.dailyQueuesFlow,
        _selectedPeriod,
        _selectedYear,
        _selectedMonth
    ) { allQueues, period, year, month ->
        processReportData(allQueues, period, year, month)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportUiState()
    )

    fun setPeriod(period: ReportPeriod) { _selectedPeriod.value = period }
    fun setYear(year: Int) { _selectedYear.value = year }
    fun setMonth(month: Int) { _selectedMonth.value = month }

    private fun processReportData(
        queues: List<QueueItem>,
        period: ReportPeriod,
        year: Int,
        month: Int
    ): ReportUiState {
        val calendar = Calendar.getInstance()
        val allUsers = authRepository.getAllUsers()

        // --- Tentukan Rentang Tanggal ---
        val (startDate, endDate) = when (period) {
            ReportPeriod.HARIAN -> {
                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val start = calendar.time
                calendar.add(Calendar.DAY_OF_YEAR, 6)
                val end = calendar.time
                Pair(start, end)
            }
            ReportPeriod.MINGGUAN -> {
                calendar.set(year, month, 1)
                val start = calendar.time
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val end = calendar.time
                Pair(start, end)
            }
            ReportPeriod.BULANAN -> {
                calendar.set(year, 0, 1)
                val start = calendar.time
                calendar.add(Calendar.YEAR, 1)
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val end = calendar.time
                Pair(start, end)
            }
            ReportPeriod.TAHUNAN -> {
                // Ambil semua data untuk perbandingan tahunan
                Pair(Date(0), Date())
            }
        }

        // --- Filter Antrian & Kalkulasi KPI ---
        val filteredQueues = queues.filter { it.createdAt.after(startDate) && it.createdAt.before(endDate) }
        val servedQueues = filteredQueues.filter { it.status == QueueStatus.SELESAI }
        val totalServed = servedQueues.size


        // --- Agregasi Data Grafik ---
        val chartData = when (period) {
            ReportPeriod.HARIAN -> {
                val dailyCounts = servedQueues.groupBy { Calendar.getInstance().apply { time = it.createdAt }.get(Calendar.DAY_OF_WEEK) }
                listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab").mapIndexed { index, dayName ->
                    DailyReport(dayName, dailyCounts[index + 1]?.size ?: 0)
                }
            }
            ReportPeriod.MINGGUAN -> {
                val weeklyCounts = servedQueues.groupBy { Calendar.getInstance().apply { time = it.createdAt }.get(Calendar.WEEK_OF_MONTH) }
                (1..5).map { weekNum -> DailyReport("W$weekNum", weeklyCounts[weekNum]?.size ?: 0) }
            }
            ReportPeriod.BULANAN -> {
                val monthlyCounts = servedQueues.groupBy { Calendar.getInstance().apply { time = it.createdAt }.get(Calendar.MONTH) }
                uiState.value.availableMonths.mapIndexed { index, monthName ->
                    DailyReport(monthName.take(3), monthlyCounts[index]?.size ?: 0)
                }
            }
            ReportPeriod.TAHUNAN -> {
                val yearlyCounts = servedQueues.groupBy { Calendar.getInstance().apply { time = it.createdAt }.get(Calendar.YEAR) }
                yearlyCounts.map { DailyReport(it.key.toString(), it.value.size) }.sortedBy { it.day }
            }
        }


        val uniquePatientIds = filteredQueues.map { it.userId }.distinct()
        val uniquePatients = allUsers.filter { it.uid in uniquePatientIds }

        return ReportUiState(
            isLoading = false,
            totalPatientsServed = totalServed,
            chartData = chartData,
            uniquePatients = uniquePatients,
            selectedPeriod = period,
            selectedYear = year,
            selectedMonth = month,
            availableYears = queues.map { Calendar.getInstance().apply { time = it.createdAt }.get(Calendar.YEAR) }.distinct().sorted()
        )
    }
}

class ReportViewModelFactory(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository // <-- Pastikan ini ada
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportViewModel(queueRepository, authRepository) as T // <-- Pastikan authRepository diteruskan
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}