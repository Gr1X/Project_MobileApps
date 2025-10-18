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

/**
 * Merepresentasikan state UI untuk layar Dashboard Admin.
 * Berisi semua data yang telah diproses dan siap untuk ditampilkan di Composable.
 *
 * @property practiceStatus Status praktik dokter saat ini (buka/tutup, nomor dilayani, dll).
 * @property isLoading True jika data sedang dimuat, false jika sudah siap.
 * @property totalPatientsToday Jumlah total pasien yang mendaftar pada hari ini.
 * @property patientsWaiting Jumlah pasien yang sedang menunggu atau telah dipanggil.
 * @property patientsFinished Jumlah pasien yang telah selesai dilayani hari ini.
 * @property top5ActiveQueue Daftar 5 pasien teratas yang masih aktif dalam antrian (menunggu, dipanggil, dilayani).
 * @property doctorScheduleToday Jadwal praktik dokter untuk hari ini.
 */
data class AdminDashboardUiState(
    val practiceStatus: PracticeStatus? = null,
    val isLoading: Boolean = true,
    val totalPatientsToday: Int = 0,
    val patientsWaiting: Int = 0,
    val patientsFinished: Int = 0,
    val top5ActiveQueue: List<QueueItem> = emptyList(),
    val doctorScheduleToday: DailyScheduleData? = null
)

/**
 * ViewModel yang bertanggung jawab atas logika bisnis dan pengelolaan state untuk [AdminDashboardScreen].
 *
 * @property queueRepository Repository yang menyediakan data antrian dan status praktik.
 */
class AdminDashboardViewModel(private val queueRepository: QueueRepository) : ViewModel() {

    /**
     * StateFlow yang memancarkan [AdminDashboardUiState] terbaru ke UI.
     * Menggunakan `combine` untuk secara reaktif mengolah data dari `dailyQueuesFlow` dan `practiceStatusFlow`.
     * Setiap kali ada perubahan pada salah satu flow sumber, blok `combine` akan dieksekusi ulang
     * untuk menghasilkan state UI yang baru dan akurat.
     */
    val uiState: StateFlow<AdminDashboardUiState> = combine(
        queueRepository.dailyQueuesFlow,
        queueRepository.practiceStatusFlow
    ) { queues, statuses ->

        val today = Calendar.getInstance()
        val isSameDay = { d1: Calendar, d2: Calendar -> d1.get(Calendar.YEAR) == d2.get(Calendar.YEAR) && d1.get(Calendar.DAY_OF_YEAR) == d2.get(Calendar.DAY_OF_YEAR) }
        // Filter untuk hanya mengambil antrian yang dibuat pada hari ini.
        val queuesToday = queues.filter { val qd = Calendar.getInstance().apply { time = it.createdAt }; isSameDay(today, qd) }

        // Mengambil status dan jadwal praktik untuk dokter yang relevan.
        val practiceStatus = statuses["doc_123"]
        val weeklySchedule = queueRepository.getDoctorSchedule("doc_123")
        val dayOfWeekInt = today.get(Calendar.DAY_OF_WEEK)
        val dayMapping = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
        val currentDayString = dayMapping[dayOfWeekInt - 1]
        val todaySchedule = weeklySchedule.find { it.dayOfWeek.equals(currentDayString, ignoreCase = true) }

        // Mengambil 5 antrian aktif teratas untuk ditampilkan di dashboard.
        val activeQueues = queuesToday
            .filter { it.status in listOf(QueueStatus.DILAYANI, QueueStatus.DIPANGGIL, QueueStatus.MENUNGGU) }
            .sortedBy { it.queueNumber }
            .take(5)

        // Membuat objek state UI yang baru dengan data yang sudah diproses.
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
        // Mengubah cold Flow menjadi hot StateFlow yang efisien untuk dibagikan ke UI.
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdminDashboardUiState() // State awal saat UI pertama kali berlangganan.
    )
}

/**
 * Factory untuk membuat instance [AdminDashboardViewModel].
 * Diperlukan karena ViewModel memiliki dependensi ([queueRepository]) yang perlu disediakan saat pembuatan.
 */
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