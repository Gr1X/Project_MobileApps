    // File BARU: features/admin/manageSchedule/ManagePracticeScheduleScreen.kt
package com.example.project_mobileapps.features.admin.manageSchedule

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.data.local.DailyScheduleData
import com.example.project_mobileapps.ui.components.ConfirmationBottomSheet

    /**
     * Composable untuk layar manajemen jadwal praktik.
     * Memungkinkan Admin/Dokter untuk mengatur jadwal mingguan, batas waktu panggil pasien,
     * dan estimasi waktu layanan per pasien.
     *
     * @param viewModel ViewModel [ManagePracticeScheduleViewModel] yang menyediakan state dan logika untuk layar ini.
     */
    @Composable fun ManagePracticeScheduleScreen(viewModel: ManagePracticeScheduleViewModel) {
        val uiState by viewModel.uiState.collectAsState()
        val context = LocalContext.current

        // State untuk mengontrol visibilitas bottom sheet konfirmasi penyimpanan.
        var showSaveConfirmation by remember { mutableStateOf(false) }

        if (showSaveConfirmation) {
            ConfirmationBottomSheet(
                onDismiss = { showSaveConfirmation = false },
                onConfirm = {
                    showSaveConfirmation = false
                    viewModel.saveSchedule(context)
                },
                title = "Simpan Perubahan Jadwal?",
                text = "Semua perubahan pada jadwal mingguan dan pengaturan waktu akan disimpan. Apakah Anda yakin?"
            )
        }

        // Menggunakan LazyColumn agar seluruh konten, termasuk header dan tombol, dapat di-scroll.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Item 1: Judul Halaman
            item {
                Text(
                    text = "Atur Jadwal & Waktu",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Item 2: Kartu Pengatur Waktu (Batas Panggil & Estimasi Layanan)
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        CallTimeLimitSelector(
                            currentTime = uiState.callTimeLimit,
                            onTimeChange = viewModel::onCallTimeLimitChange
                        )
                        Divider()
                        ServiceTimeSelector(
                            currentTime = uiState.estimatedServiceTime,
                            onTimeChange = viewModel::onServiceTimeChange
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Item 3: Judul Jadwal Mingguan
            item {
                Text(
                    "Jadwal Mingguan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Item 4 dst.: Daftar Jadwal Hari
            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
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

            // Item Terakhir: Tombol Simpan
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showSaveConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Simpan Jadwal")
                    }
                }
            }
        }
    }

    /**
     * Menampilkan satu baris untuk jadwal satu hari (misal: "Senin"), lengkap dengan
     * jam praktik yang dapat diubah dan Switch untuk mengaktifkan/menonaktifkan.
     *
     * @param scheduleData Data jadwal untuk hari yang bersangkutan.
     * @param onStatusChange Callback yang dipanggil saat Switch diubah (buka/tutup).
     * @param onTimeChange Callback yang dipanggil saat waktu mulai atau selesai diubah melalui TimePickerDialog.
     */
@Composable
fun ScheduleDayRow(
    scheduleData: DailyScheduleData,
    onStatusChange: (Boolean) -> Unit,
    onTimeChange: (hour: Int, minute: Int, isStartTime: Boolean) -> Unit
) {
    val context = LocalContext.current
    val showTimePicker = { isStartTime: Boolean ->
        val timeParts = (if (isStartTime) scheduleData.startTime else scheduleData.endTime).split(":")
        val initialHour = timeParts[0].toInt()
        val initialMinute = timeParts[1].toInt()

        TimePickerDialog(
            context,
            { _, hour, minute -> onTimeChange(hour, minute, isStartTime) },
            initialHour,
            initialMinute,
            true
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

    /**
     * Komponen UI yang dapat digunakan kembali untuk mengatur nilai waktu (menit)
     * menggunakan tombol tambah (+) dan kurang (-).
     * Digunakan di sini untuk mengatur batas waktu panggil pasien.
     *
     * @param currentTime Nilai waktu (menit) saat ini yang ditampilkan.
     * @param onTimeChange Callback yang dipanggil saat tombol + atau - ditekan, membawa perubahan (+1 atau -1).
     */
    @Composable
    private fun CallTimeLimitSelector(
        currentTime: Int,
        onTimeChange: (Int) -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Column(modifier = Modifier.weight(1f)) {
                Text("Batas Waktu Panggil", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("Waktu tunggu pasien setelah dipanggil", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { onTimeChange(-1) }, enabled = currentTime > 1) {
                    Icon(Icons.Default.Remove, "Kurangi")
                }
                Text(
                    "$currentTime min",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { onTimeChange(1) }, enabled = currentTime < 60) {
                    Icon(Icons.Default.Add, "Tambah")
                }
            }
        }
    }

    /**
     * Komponen UI yang dapat digunakan kembali, identik dengan [CallTimeLimitSelector],
     * namun digunakan untuk mengatur estimasi waktu layanan per pasien.
     *
     * @param currentTime Nilai waktu (menit) saat ini yang ditampilkan.
     * @param onTimeChange Callback yang dipanggil saat tombol + atau - ditekan.
     */
    @Composable
    private fun ServiceTimeSelector(
        currentTime: Int,
        onTimeChange: (Int) -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // TAMBAHKAN MODIFIER.WEIGHT(1f) DI SINI
            Column(modifier = Modifier.weight(1f)) {
                Text("Estimasi Waktu Layanan", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("Durasi rata-rata per pasien",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { onTimeChange(-1) }, enabled = currentTime > 5) {
                    Icon(Icons.Default.Remove, "Kurangi")
                }
                Text("$currentTime min", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { onTimeChange(1) }, enabled = currentTime < 60) {
                    Icon(Icons.Default.Add, "Tambah")
                }
            }
        }
    }