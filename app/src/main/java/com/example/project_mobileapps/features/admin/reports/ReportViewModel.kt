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

/**
 * Enum untuk mendefinisikan pilihan periode waktu pada filter laporan.
 * @property displayName Teks yang akan ditampilkan di UI dropdown.
 */
enum class ReportPeriod(val displayName: String) {
    HARIAN("Harian"),
    MINGGUAN("Mingguan"),
    BULANAN("Bulanan"),
    TAHUNAN("Tahunan")
}

/**
 * Merepresentasikan state UI untuk layar [ReportScreen].
 *
 * @property isLoading True jika data laporan sedang diproses.
 * @property totalPatientsServed Jumlah total pasien yang selesai dilayani dalam periode terpilih.
 * @property avgPatientsPerDay Rata-rata jumlah pasien per hari.
 * @property avgServiceTimeMinutes Rata-rata waktu layanan per pasien (dalam menit).
 * @property cancellationRate Persentase antrian yang dibatalkan.
 * @property chartData Data yang telah diagregasi dan siap untuk ditampilkan di [PatientStatsChart].
 * @property uniquePatients Daftar pasien unik yang memiliki riwayat kunjungan dalam periode terpilih.
 * @property selectedPeriod Periode filter yang sedang aktif.
 * @property availableYears Daftar tahun yang tersedia di dropdown filter.
 * @property availableMonths Daftar nama bulan yang tersedia di dropdown filter.
 * @property selectedYear Tahun yang sedang dipilih di filter.
 * @property selectedMonth Bulan yang sedang dipilih di filter (berbasis indeks 0-11).
 */
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

/**
 * ViewModel untuk [ReportScreen]. Bertanggung jawab untuk memfilter, mengagregasi,
 * dan menghitung data laporan berdasarkan pilihan filter dari pengguna.
 *
 * @param queueRepository Repository untuk mendapatkan data mentah semua antrian.
 * @param authRepository Repository untuk mendapatkan data pengguna.
 */
class ReportViewModel(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // StateFlow internal untuk menyimpan pilihan filter dari UI.
    private val _selectedPeriod = MutableStateFlow(ReportPeriod.HARIAN)
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))

    /**
     * StateFlow publik yang menggabungkan semua data mentah dan filter menjadi satu [ReportUiState].
     * Blok `combine` akan dieksekusi ulang setiap kali data antrian atau salah satu filter berubah,
     * memastikan UI selalu menampilkan laporan yang akurat.
     */
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

    // Fungsi publik untuk mengubah state filter dari UI.
    fun setPeriod(period: ReportPeriod) { _selectedPeriod.value = period }
    fun setYear(year: Int) { _selectedYear.value = year }
    fun setMonth(month: Int) { _selectedMonth.value = month }

    /**
     * Fungsi inti yang memproses data mentah antrian menjadi data laporan yang siap saji.
     *
     * @param queues Daftar lengkap semua [QueueItem].
     * @param period Periode filter yang dipilih.
     * @param year Tahun filter yang dipilih.
     * @param month Bulan filter yang dipilih.
     * @return Objek [ReportUiState] yang telah diisi dengan data laporan.
     */
    private fun processReportData(
        queues: List<QueueItem>,
        period: ReportPeriod,
        year: Int,
        month: Int
    ): ReportUiState {
        val calendar = Calendar.getInstance()
        val allUsers = authRepository.getAllUsers()

        // --- Langkah 1: Tentukan Rentang Tanggal Berdasarkan Filter ---
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
                // Untuk perbandingan tahunan, ambil semua data.
                Pair(Date(0), Date())
            }
        }

        // --- Langkah 2: Filter Antrian & Kalkulasi KPI (Key Performance Indicators) ---
        val filteredQueues = queues.filter { it.createdAt.after(startDate) && it.createdAt.before(endDate) }
        val servedQueues = filteredQueues.filter { it.status == QueueStatus.SELESAI }
        val totalServed = servedQueues.size
        // (KPI lain seperti avg time, cancellation rate, dll. bisa ditambahkan di sini)

        // --- Langkah 3: Agregasi Data untuk Grafik ---
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

        // --- Langkah 4: Dapatkan Daftar Pasien Unik ---
        val uniquePatientIds = filteredQueues.map { it.userId }.distinct()
        val uniquePatients = allUsers.filter { it.uid in uniquePatientIds }

        // --- Langkah 5: Buat Objek State Akhir ---
        return ReportUiState(
            isLoading = false,
            totalPatientsServed = totalServed,
            chartData = chartData,
            uniquePatients = uniquePatients,
            selectedPeriod = period,
            selectedYear = year,
            selectedMonth = month,
            // Ekstrak daftar tahun yang tersedia dari data antrian untuk ditampilkan di dropdown.
            availableYears = queues.map { Calendar.getInstance().apply { time = it.createdAt }.get(Calendar.YEAR) }.distinct().sorted()
        )
    }
}

/**
 * Factory untuk membuat instance [ReportViewModel].
 */
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