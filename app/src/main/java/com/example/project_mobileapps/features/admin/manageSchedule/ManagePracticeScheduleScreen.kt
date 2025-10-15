// File BARU: features/admin/manageSchedule/ManagePracticeScheduleScreen.kt
package com.example.project_mobileapps.features.admin.manageSchedule

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.data.local.DailyScheduleData

@Composable
fun ManagePracticeScheduleScreen(viewModel: ManagePracticeScheduleViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Atur Jadwal Praktik",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.schedules) { schedule ->
                    ScheduleDayRow(
                        scheduleData = schedule,
                        onStatusChange = { isOpen ->
                            viewModel.onStatusChange(schedule.dayOfWeek, isOpen)
                        },
                        onTimeChange = { hour, minute, isStartTime ->
                            viewModel.onTimeChange(schedule.dayOfWeek, hour, minute, isStartTime)
                        }
                    )
                    Divider()
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.saveSchedule(context) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Simpan Perubahan")
                }
            }
        }
    }
}

@Composable
fun ScheduleDayRow(
    scheduleData: DailyScheduleData,
    onStatusChange: (Boolean) -> Unit,
    onTimeChange: (hour: Int, minute: Int, isStartTime: Boolean) -> Unit
) {
    val context = LocalContext.current

    // Fungsi untuk menampilkan Time Picker
    val showTimePicker = { isStartTime: Boolean ->
        val timeParts = (if (isStartTime) scheduleData.startTime else scheduleData.endTime).split(":")
        val initialHour = timeParts[0].toInt()
        val initialMinute = timeParts[1].toInt()

        TimePickerDialog(
            context,
            { _, hour, minute -> onTimeChange(hour, minute, isStartTime) },
            initialHour,
            initialMinute,
            true // 24-hour format
        ).show()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            scheduleData.dayOfWeek,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (scheduleData.isOpen) {
                Text(
                    text = scheduleData.startTime,
                    modifier = Modifier
                        .clickable { showTimePicker(true) }
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text("-")
                Text(
                    text = scheduleData.endTime,
                    modifier = Modifier
                        .clickable { showTimePicker(false) }
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text("Tutup", modifier = Modifier.padding(horizontal = 8.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Switch(
                checked = scheduleData.isOpen,
                onCheckedChange = onStatusChange
            )
        }
    }
}