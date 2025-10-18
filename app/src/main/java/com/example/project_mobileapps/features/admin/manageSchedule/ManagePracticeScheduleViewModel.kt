package com.example.project_mobileapps.features.admin.manageSchedule

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.local.DailyScheduleData
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Merepresentasikan state UI untuk layar [ManagePracticeScheduleScreen].
 *
 * @property schedules Daftar jadwal harian yang sedang diedit oleh pengguna.
 * @property isLoading True jika data jadwal awal sedang dimuat.
 * @property isSaving True saat proses penyimpanan perubahan sedang berlangsung.
 * @property estimatedServiceTime Estimasi waktu layanan per pasien (dalam menit) saat ini.
 * @property isPracticeOpen Status buka/tutup praktik saat ini (tidak digunakan langsung di UI ini, tapi tersedia).
 * @property callTimeLimit Batas waktu panggil pasien (dalam menit) saat ini.
 */

data class PracticeScheduleUiState(
    val schedules: List<DailyScheduleData> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val estimatedServiceTime: Int = 15,
    val isPracticeOpen: Boolean = false,
    val callTimeLimit: Int = 15
)

/**
 * ViewModel untuk [ManagePracticeScheduleScreen].
 * Bertanggung jawab untuk memuat, memperbarui, dan menyimpan jadwal praktik dan pengaturan terkait.
 *
 * @property queueRepository Repository yang menyediakan dan menyimpan data jadwal.
 */
class ManagePracticeScheduleViewModel(private val queueRepository: QueueRepository) : ViewModel() {
    // State internal untuk melacak status proses penyimpanan.
    private val _isSaving = MutableStateFlow(false)
    // State internal untuk menampung daftar jadwal yang sedang diedit oleh pengguna sebelum disimpan.
    private val _editedSchedules = MutableStateFlow<List<DailyScheduleData>>(emptyList())

    /**
     * StateFlow publik yang menggabungkan beberapa sumber data menjadi satu [PracticeScheduleUiState].
     * UI akan mengamati flow ini untuk mendapatkan data terbaru secara reaktif.
     */
    val uiState: StateFlow<PracticeScheduleUiState> = combine(
        queueRepository.practiceStatusFlow,
        _editedSchedules,
        _isSaving
    ) { statuses, schedules, isSaving ->
        val practiceStatus = statuses["doc_123"]
        PracticeScheduleUiState(
            schedules = schedules,
            isLoading = false, // Loading selesai setelah _editedSchedules diisi.
            isSaving = isSaving,
            estimatedServiceTime = practiceStatus?.estimatedServiceTimeInMinutes ?: 15,
            callTimeLimit = practiceStatus?.patientCallTimeLimitMinutes ?: 15 // <-- AMBIL DATA DARI FLOW
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PracticeScheduleUiState(isLoading = true)
    )

    init {
        // Memuat jadwal awal saat ViewModel pertama kali dibuat.
        loadSchedules()
    }

    /**
     * Menangani perubahan pada input batas waktu panggil.
     * Langsung memanggil repository untuk memperbarui nilai.
     *
     * @param change Perubahan yang akan diterapkan (+1 atau -1).
     */
    fun onCallTimeLimitChange(change: Int) {
        viewModelScope.launch {
            val newTime = (uiState.value.callTimeLimit + change).coerceIn(1, 60) // Batasi 1-60 menit
            queueRepository.updatePatientCallTimeLimit("doc_123", newTime)
        }
    }

    /**
     * Mengambil jadwal dokter dari repository dan mengisinya ke state internal `_editedSchedules`.
     */
    private fun loadSchedules() {
        viewModelScope.launch {
            _editedSchedules.value = queueRepository.getDoctorSchedule("doc_123")
        }
    }

    /**
     * Memperbarui state jadwal lokal ketika pengguna mengubah status buka/tutup (Switch) untuk suatu hari.
     *
     * @param day Hari yang diubah (misal: "Senin").
     * @param isOpen Status baru (true jika buka, false jika tutup).
     */
    fun onStatusChange(day: String, isOpen: Boolean) {
        _editedSchedules.update { currentSchedules ->
            currentSchedules.map {
                if (it.dayOfWeek == day) it.copy(isOpen = isOpen) else it
            }
        }
    }

    /**
     * Memperbarui state jadwal lokal ketika pengguna mengubah jam mulai atau selesai.
     *
     * @param day Hari yang diubah.
     * @param hour Jam baru.
     * @param minute Menit baru.
     * @param isStartTime True jika yang diubah adalah waktu mulai, false jika waktu selesai.
     */
    fun onTimeChange(day: String, hour: Int, minute: Int, isStartTime: Boolean) {
        val formattedTime = String.format("%02d:%02d", hour, minute)
        _editedSchedules.update { currentSchedules ->
            currentSchedules.map {
                if (it.dayOfWeek == day) {
                    if (isStartTime) it.copy(startTime = formattedTime) else it.copy(endTime = formattedTime)
                } else {
                    it
                }
            }
        }
    }

    /**
     * Menangani perubahan pada input estimasi waktu layanan.
     * Langsung memanggil repository untuk memperbarui nilai.
     *
     * @param change Perubahan yang akan diterapkan (+1 atau -1).
     */
    fun onServiceTimeChange(change: Int) {
        viewModelScope.launch {
            val newTime = (uiState.value.estimatedServiceTime + change).coerceIn(5, 60)
            queueRepository.updateEstimatedServiceTime("doc_123", newTime)
        }
    }

    /**
     * Menyimpan semua perubahan jadwal yang ada di `_editedSchedules` ke repository.
     * Mengatur state `_isSaving` untuk memberikan feedback visual di UI.
     */
    fun saveSchedule(context: Context) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = queueRepository.updateDoctorSchedule("doc_123", _editedSchedules.value)
            if (result.isSuccess) {
                Toast.makeText(context, "Jadwal berhasil disimpan!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal menyimpan jadwal.", Toast.LENGTH_SHORT).show()
            }
            _isSaving.value = false
        }
    }
}

/**
 * Factory untuk membuat instance [ManagePracticeScheduleViewModel].
 */
class ManagePracticeScheduleViewModelFactory(private val queueRepository: QueueRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManagePracticeScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ManagePracticeScheduleViewModel(queueRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}