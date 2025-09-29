package com.example.project_mobileapps.features.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    uiState: ScheduleUiState,
    onKeluhanChanged: (String) -> Unit,
    onTimeSelected: (String) -> Unit,
    onConfirmBooking: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Pilih Jadwal") }) },
        bottomBar = {
            Button(
                onClick = onConfirmBooking,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                enabled = uiState.selectedTime != null && uiState.bookingStatus != "Loading"
            ) {
                if (uiState.bookingStatus == "Loading") {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Konfirmasi Booking untuk jam ${uiState.selectedTime ?: ""}")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (uiState.isLoadingSchedule) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                OutlinedTextField(
                    value = uiState.keluhan,
                    onValueChange = onKeluhanChanged,
                    label = { Text("Masukkan Keluhan Singkat") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text("Pilih Waktu Tersedia:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 90.dp),
                    modifier = Modifier.height(250.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.availableTimes) { time ->
                        TimeSlotChip(
                            time = time,
                            isSelected = time == uiState.selectedTime,
                            onTimeSelected = onTimeSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeSlotChip(time: String, isSelected: Boolean, onTimeSelected: (String) -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clickable { onTimeSelected(time) }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = time, color = contentColor)
    }
}