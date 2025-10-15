package com.example.project_mobileapps.features.admin

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.local.DailyReport
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import com.example.project_mobileapps.data.repo.QueueRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
    val queueList: List<QueueItem> = emptyList(),
    val practiceStatus: PracticeStatus? = null,
    val isLoading: Boolean = true,
    val totalPatientsToday: Int = 0,
    val patientsWaiting: Int = 0,
    val patientsFinished: Int = 0,
    val weeklyReport: List<DailyReport> = emptyList()
)

class AdminDashboardViewModel(private val queueRepository: QueueRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        fetchWeeklyReport()

        viewModelScope.launch {
            combine(
                queueRepository.dailyQueuesFlow,
                queueRepository.practiceStatusFlow
            ) { queues, statuses ->
                val doctorId = "doc_123"
                val total = queues.count { it.status != com.example.project_mobileapps.data.model.QueueStatus.DIBATALKAN }
                val waiting = queues.count { it.status == com.example.project_mobileapps.data.model.QueueStatus.MENUNGGU || it.status == com.example.project_mobileapps.data.model.QueueStatus.DIPANGGIL }
                val finished = queues.count { it.status == com.example.project_mobileapps.data.model.QueueStatus.SELESAI }

                _uiState.update { currentState ->
                    currentState.copy(
                        queueList = queues.filter { it.doctorId == doctorId },
                        practiceStatus = statuses[doctorId],
                        isLoading = false,
                        totalPatientsToday = total,
                        patientsWaiting = waiting,
                        patientsFinished = finished
                    )
                }
            }.collect()
        }
    }

    private fun fetchWeeklyReport() {
        viewModelScope.launch {
            val report = queueRepository.getWeeklyReport()
            _uiState.update { it.copy(weeklyReport = report) }
        }
    }

    fun callNextPatient(context: Context) {
        viewModelScope.launch {
            val doctorId = "doc_123"
            val result = queueRepository.callNextPatient(doctorId)

            if (result.isSuccess) {
                Toast.makeText(context, "Pasien berikutnya berhasil dipanggil!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, result.exceptionOrNull()?.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
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