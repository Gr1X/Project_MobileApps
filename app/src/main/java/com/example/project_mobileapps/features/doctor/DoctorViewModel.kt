// File: features/doctor/DoctorViewModel.kt
package com.example.project_mobileapps.features.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.DailyScheduleData
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.features.admin.manageSchedule.PatientQueueDetails
import kotlinx.coroutines.flow.*
import java.util.Calendar
/**
 * Model data (UI State) untuk [DoctorDashboardScreen].
 * Menggabungkan semua informasi yang diperlukan oleh UI.
 *
 * @property greeting Salam sapaan (misal: "Selamat Pagi").
 * @property doctorName Nama dokter yang sedang login.
 * @property topQueueList Daftar 3 pasien teratas dalam antrian.
 * @property practiceStatus Objek [PracticeStatus] yang berisi info praktik (buka/tutup, antrian, dll).
 * @property isLoading Status loading awal.
 * @property waitingInQueue Jumlah total pasien yang sedang menunggu (termasuk dipanggil/dilayani).
 * @property nextQueueNumber Nomor antrian berikutnya yang akan dipanggil (status MENUNGGU).
 * @property selectedPatient Pasien yang dipilih untuk ditampilkan di bottom sheet detail.
 * @property todaySchedule Jadwal [DailyScheduleData] untuk hari ini.
 */
data class DoctorUiState(
    val greeting: String = "Selamat Datang",
    val doctorName: String = "Dokter",
    val topQueueList: List<PatientQueueDetails> = emptyList(),
    val practiceStatus: PracticeStatus? = null,
    val isLoading: Boolean = true,
    val waitingInQueue: Int = 0,
    val nextQueueNumber: String = "-",
    val selectedPatient: PatientQueueDetails? = null,
    val todaySchedule: DailyScheduleData? = null // <-- TAMBAHKAN BARIS INI
)
/**
 * ViewModel untuk [DoctorDashboardScreen].
 * Bertanggung jawab untuk mengumpulkan data dari berbagai repository,
 * menggabungkannya menjadi satu [DoctorUiState], dan mengelola logika
 * untuk pemilihan pasien.
 *
 * @param queueRepository Repository untuk data antrian, status praktik, dan jadwal.
 * @param authRepository Repository untuk data pengguna (dokter dan pasien).
 */
class DoctorViewModel(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _selectedPatient = MutableStateFlow<PatientQueueDetails?>(null)
    /**
     * StateFlow publik yang diekspos ke UI.
     * Menggunakan `combine` untuk menggabungkan 4 aliran data (Flow) menjadi satu [DoctorUiState].
     * Setiap kali salah satu dari 4 flow ini berubah, `combine` akan dieksekusi ulang
     * dan UI state akan diperbarui secara otomatis.
     */
    val uiState: StateFlow<DoctorUiState> = combine(
        queueRepository.dailyQueuesFlow,
        queueRepository.practiceStatusFlow,
        authRepository.currentUser,
        _selectedPatient
    ) { queues, statuses, doctorUser, selectedPatient ->

        val doctorId = AppContainer.CLINIC_ID
        val allUsers = authRepository.getAllUsers()

        val weeklySchedule = queueRepository.getDoctorSchedule(doctorId)
        val practiceStatus = statuses[doctorId]
        val calendar = Calendar.getInstance()
        val dayOfWeekInt = calendar.get(Calendar.DAY_OF_WEEK) // Minggu=1, Senin=2, ..
        val dayMapping = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
        val currentDayString = dayMapping[dayOfWeekInt - 1]
        val todaySchedule = weeklySchedule.find { it.dayOfWeek.equals(currentDayString, ignoreCase = true) }
        val activeQueues = queues
            .filter { it.doctorId == doctorId && (it.status == QueueStatus.MENUNGGU || it.status == QueueStatus.DIPANGGIL || it.status == QueueStatus.DILAYANI) }
            .sortedBy { it.queueNumber }
        val topThreeQueues = activeQueues.take(3).map { queueItem ->
            PatientQueueDetails(queueItem = queueItem, user = allUsers.find { it.uid == queueItem.userId })
        }
        val nextPatient = activeQueues.find { it.status == QueueStatus.MENUNGGU }
        val nextQueueNumberString = nextPatient?.queueNumber?.toString() ?: "-"
        val totalWaitingInQueue = activeQueues.size

        DoctorUiState(
            greeting = getGreetingBasedOnTime(),
            doctorName = doctorUser?.name ?: "Dokter",
            topQueueList = topThreeQueues,
            practiceStatus = practiceStatus,
            isLoading = false,
            waitingInQueue = totalWaitingInQueue,
            nextQueueNumber = nextQueueNumberString,
            selectedPatient = selectedPatient,
            todaySchedule = todaySchedule // <-- MASUKKAN DATA JADWAL KE STATE
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DoctorUiState()
    )

    private fun getGreetingBasedOnTime(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Selamat Pagi"
            in 12..13 -> "Selamat Siang"
            in 14..17 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }
    /**
     * Dipanggil dari UI saat dokter mengklik kartu pasien.
     * Menyimpan data pasien ke state [selectedPatient] untuk memicu bottom sheet.
     * @param patient Data pasien yang diklik.
     */
    fun selectPatient(patient: PatientQueueDetails) {
        _selectedPatient.value = patient
    }
    /**
     * Dipanggil dari UI saat bottom sheet ditutup (dismiss).
     * Mengosongkan state [selectedPatient].
     */
    fun clearSelectedPatient() {
        _selectedPatient.value = null
    }
}
/**
 * Factory untuk [DoctorViewModel].
 * Diperlukan untuk meng-inject [QueueRepository] dan [AuthRepository].
 */
class DoctorViewModelFactory(
    private val queueRepository: QueueRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DoctorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DoctorViewModel(queueRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}