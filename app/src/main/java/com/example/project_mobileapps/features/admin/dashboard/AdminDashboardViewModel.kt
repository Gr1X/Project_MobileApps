package com.example.project_mobileapps.features.admin.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.DailyScheduleData
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.repo.QueueRepository
import com.example.project_mobileapps.di.AppContainer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * State UI Lengkap untuk Dashboard Admin
 */
data class AdminDashboardUiState(
    val practiceStatus: PracticeStatus? = null,
    val isLoading: Boolean = true,
    val totalPatientsToday: Int = 0,
    val patientsWaiting: Int = 0,
    val patientsFinished: Int = 0,
    val top5ActiveQueue: List<QueueItem> = emptyList(),
    val doctorScheduleToday: DailyScheduleData? = null,

    // Chart Data
    val chartData: List<Int> = emptyList(),
    val chartLabels: List<String> = emptyList(),
    val chartTitle: String = "",
    val isLoadingChart: Boolean = false,

    // Trend Data
    val trendLabel: String = "0%",
    val isTrendPositive: Boolean = true,

    // Demografi Data
    val maleCount: Int = 0,
    val femaleCount: Int = 0
)

// Helper class internal
private data class ChartState(
    val data: List<Int> = emptyList(),
    val labels: List<String> = emptyList(),
    val title: String = "",
    val isLoading: Boolean = false,
    val trendLabel: String = "0%",
    val isPositive: Boolean = true
)

// Helper class internal
private data class GenderState(
    val male: Int = 0,
    val female: Int = 0
)

class AdminDashboardViewModel(private val queueRepository: QueueRepository) : ViewModel() {

    private val clinicId = AppContainer.CLINIC_ID

    private val _chartState = MutableStateFlow(ChartState())
    private val _genderState = MutableStateFlow(GenderState())

    init {
        loadChartData("Harian")
    }

    val uiState: StateFlow<AdminDashboardUiState> = combine(
        queueRepository.dailyQueuesFlow,
        queueRepository.practiceStatusFlow,
        _chartState,
        _genderState
    ) { queues, statuses, chartState, genderState ->

        val today = Calendar.getInstance()
        val isSameDay = { d1: Calendar, d2: Calendar ->
            d1.get(Calendar.YEAR) == d2.get(Calendar.YEAR) &&
                    d1.get(Calendar.DAY_OF_YEAR) == d2.get(Calendar.DAY_OF_YEAR)
        }
        val queuesToday = queues.filter {
            val qd = Calendar.getInstance().apply { time = it.createdAt }
            isSameDay(today, qd)
        }

        val practiceStatus = statuses[clinicId]
        val weeklySchedule = queueRepository.getDoctorSchedule(clinicId)
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
            top5ActiveQueue = activeQueues,

            // Mapping Chart
            chartData = chartState.data,
            chartLabels = chartState.labels,
            chartTitle = chartState.title,
            isLoadingChart = chartState.isLoading,
            trendLabel = chartState.trendLabel,
            isTrendPositive = chartState.isPositive,

            // Mapping Gender
            maleCount = genderState.male,
            femaleCount = genderState.female
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdminDashboardUiState()
    )

    fun loadChartData(filterType: String) {
        viewModelScope.launch {
            _chartState.update { it.copy(isLoading = true) }

            val daysBackToFetch = when (filterType) {
                "Harian" -> 7
                "Mingguan" -> 30
                "Bulanan" -> 365
                else -> 7
            }

            // 1. Chart Data
            val rawStats = queueRepository.getPatientStatistics(daysBackToFetch)
            val (processedLabels, processedData) = processDataForChart(rawStats, filterType)

            // 2. Hitung Trend
            val (trendTxt, isPos) = calculateTrend(processedData)

            val title = when(filterType) {
                "Harian" -> "7 Hari Terakhir"
                "Mingguan" -> "Bulan Ini"
                "Bulanan" -> "Tahun Ini"
                else -> ""
            }

            _chartState.update {
                it.copy(
                    data = processedData,
                    labels = processedLabels,
                    title = title,
                    isLoading = false,
                    trendLabel = trendTxt,
                    isPositive = isPos
                )
            }

            // 3. Gender Data
            val genderStats = queueRepository.getGenderStatistics(daysBackToFetch)
            _genderState.update {
                it.copy(
                    male = genderStats["Laki-laki"] ?: 0,
                    female = genderStats["Perempuan"] ?: 0
                )
            }
        }
    }

    private fun processDataForChart(rawStats: Map<String, Int>, filterType: String): Pair<List<String>, List<Int>> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        return when (filterType) {
            "Harian" -> {
                val labels = mutableListOf<String>()
                val data = mutableListOf<Int>()
                val dayFormat = SimpleDateFormat("EEE", Locale("id", "ID"))
                val tempCal = Calendar.getInstance()
                tempCal.add(Calendar.DAY_OF_YEAR, -6)

                for (i in 0..6) {
                    val dateKey = dateFormat.format(tempCal.time)
                    labels.add(dayFormat.format(tempCal.time))
                    data.add(rawStats[dateKey] ?: 0)
                    tempCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                Pair(labels, data)
            }
            "Mingguan" -> {
                val labels = listOf("Mg 1", "Mg 2", "Mg 3", "Mg 4")
                val data = IntArray(4) { 0 }
                rawStats.forEach { (dateString, count) ->
                    try {
                        val date = dateFormat.parse(dateString)
                        val cal = Calendar.getInstance().apply { time = date }
                        if (cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth) {
                            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                            val weekIndex = ((dayOfMonth - 1) / 7).coerceAtMost(3)
                            data[weekIndex] += count
                        }
                    } catch (e: Exception) {}
                }
                Pair(labels, data.toList())
            }
            "Bulanan" -> {
                val labels = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
                val data = IntArray(12) { 0 }
                rawStats.forEach { (dateString, count) ->
                    try {
                        val date = dateFormat.parse(dateString)
                        val cal = Calendar.getInstance().apply { time = date }
                        if (cal.get(Calendar.YEAR) == currentYear) {
                            data[cal.get(Calendar.MONTH)] += count
                        }
                    } catch (e: Exception) {}
                }
                Pair(labels, data.toList())
            }
            else -> Pair(emptyList(), emptyList())
        }
    }

    private fun calculateTrend(data: List<Int>): Pair<String, Boolean> {
        if (data.isEmpty()) return Pair("0%", true)
        val totalPoints = data.size
        if (totalPoints < 2) return Pair("0%", true)

        val midPoint = totalPoints / 2
        val pastSum = data.take(midPoint).sum().toFloat()
        val currentSum = data.takeLast(totalPoints - midPoint).sum().toFloat()

        if (pastSum == 0f) {
            return if (currentSum > 0) Pair("100%", true) else Pair("0%", true)
        }

        val diff = currentSum - pastSum
        val percentage = (diff / pastSum) * 100
        val sign = if (percentage >= 0) "+" else ""
        val formatted = String.format(Locale.US, "%s%.1f%%", sign, percentage)

        return Pair(formatted, percentage >= 0)
    }
}

class AdminDashboardViewModelFactory(private val repository: QueueRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminDashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminDashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}