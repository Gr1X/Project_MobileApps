// File: features/notifications/GlobalNotificationObserver.kt
package com.example.project_mobileapps.features.notifications

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.project_mobileapps.core.navigation.AdminMenu
import com.example.project_mobileapps.core.navigation.DoctorMenu
import com.example.project_mobileapps.data.model.QueueStatus
import com.example.project_mobileapps.data.model.Role
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.NotificationRepository
import com.example.project_mobileapps.data.repo.QueueRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.navigation.BottomNavItem
import com.example.project_mobileapps.utils.NotificationHelper
import kotlinx.coroutines.delay
import java.util.Calendar

fun sendSmartNotification(
    context: Context,
    id: Int,
    title: String,
    msg: String,
    role: String,
    route: String
) {
    // 1. Tampilkan di Status Bar OS
    NotificationHelper.showNotification(context, id, title, msg, role, route)

    // 2. Simpan ke Riwayat Aplikasi (Menu Lonceng)
    // Gunakan userId jika ingin private, atau null jika broadcast. Di sini kita broadcast sesuai logic role.
    NotificationRepository.addNotification(message = "$title: $msg")
}

/**
 * Composable tanpa UI (Side-Effect) yang memantau data secara global
 * dan memicu notifikasi sistem berdasarkan Role pengguna.
 */
@Composable
fun GlobalNotificationObserver() {
    val context = LocalContext.current
    val user by AuthRepository.currentUser.collectAsState()
    val queues by AppContainer.queueRepository.dailyQueuesFlow.collectAsState()
    val practiceStatusMap by AppContainer.queueRepository.practiceStatusFlow.collectAsState()

    val doctorId = AppContainer.CLINIC_ID
    val practiceStatus = practiceStatusMap[doctorId]

    // --- STATE ANTI-SPAM ---
    // Menyimpan nomor antrian terakhir yang sudah dinotifikasi agar tidak bunyi 2x
    var lastNotifiedQueueNumber by remember { mutableIntStateOf(-1) }
    var lastStatus by remember { mutableStateOf<QueueStatus?>(null) }
    var lastPatientCount by remember { mutableIntStateOf(0) }
    var hasNotifiedClosing by remember { mutableStateOf(false) }
    var hasNotifiedShiftStart by remember { mutableStateOf(false) }

    // Efek akan jalan setiap ada perubahan pada data antrian atau status
    LaunchedEffect(queues, practiceStatus, user) {
        val currentUser = user ?: return@LaunchedEffect
        val currentServing = practiceStatus?.currentServingNumber ?: 0
        val serviceTime = practiceStatus?.estimatedServiceTimeInMinutes ?: 15

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeTotalMinutes = (currentHour * 60) + currentMinute

        // ================================================================
        // LOGIKA UNTUK PASIEN
        // ================================================================
        if (currentUser.role == Role.PASIEN) {
            val myQueue = queues.find { it.userId == currentUser.uid }
            val targetRoute = BottomNavItem.Queue.route // Arahkan ke Tab Antrian

            if (myQueue != null) {
                val peopleAhead = myQueue.queueNumber - currentServing

                // A. SELESAI
                if (myQueue.status == QueueStatus.SELESAI && lastStatus != QueueStatus.SELESAI) {
                    sendSmartNotification(context, 1, "✅ Konsultasi Selesai", "Rekam medis siap. Ketuk untuk melihat.", "PASIEN", "history")
                    lastStatus = QueueStatus.SELESAI
                }
                // B. DIBATALKAN
                else if (myQueue.status == QueueStatus.DIBATALKAN && lastStatus != QueueStatus.DIBATALKAN && lastStatus != null) {
                    sendSmartNotification(context, 1, "❌ Antrian Dibatalkan", "Mohon hubungi admin.", "PASIEN", targetRoute)
                    lastStatus = QueueStatus.DIBATALKAN
                }
                // C. DIPANGGIL
                else if (myQueue.status == QueueStatus.DIPANGGIL && lastStatus != QueueStatus.DIPANGGIL) {
                    sendSmartNotification(context, 1, "🔊 GILIRAN ANDA!", "Masuk ke ruang periksa sekarang.", "PASIEN", targetRoute)
                    lastStatus = QueueStatus.DIPANGGIL
                }
                // D. MENUNGGU (3,2,1)
                else if (myQueue.status == QueueStatus.MENUNGGU && currentServing != lastNotifiedQueueNumber) {
                    if (peopleAhead in 1..3) {
                        val title = if (peopleAhead == 1) "Anda Berikutnya!" else "$peopleAhead Pasien Lagi"
                        sendSmartNotification(context, 1, title, "Estimasi: ${peopleAhead * serviceTime} menit.", "PASIEN", targetRoute)
                        lastNotifiedQueueNumber = currentServing
                    }
                    if (lastStatus != QueueStatus.MENUNGGU) lastStatus = QueueStatus.MENUNGGU
                }
            }
        }

        // ================================================================
        // LOGIKA UNTUK DOKTER
        // ================================================================
        else if (currentUser.role == Role.DOKTER) {
            val waitingList = queues.filter { it.status == QueueStatus.MENUNGGU }
            val targetRoute = DoctorMenu.Queue.route // Arahkan ke Monitor Antrian

            // PASIEN BARU
            if (waitingList.size > lastPatientCount && lastPatientCount != 0) {
                val newPatient = waitingList.maxByOrNull { it.queueNumber }
                newPatient?.let {
                    sendSmartNotification(context, it.queueNumber, "Pasien Baru", "${it.userName} - ${it.keluhan}", "MEDIS", targetRoute)
                }
            }
            lastPatientCount = waitingList.size

            // JADWAL
            val openingTimeMinutes = (practiceStatus?.openingHour ?: 8) * 60
            if (!hasNotifiedShiftStart && (openingTimeMinutes - currentTimeTotalMinutes) in 1..15) {
                sendSmartNotification(context, 888, "⏰ Jadwal Praktik", "Buka dalam 15 menit.", "MEDIS", targetRoute)
                hasNotifiedShiftStart = true
            }
        }

        // ================================================================
        // LOGIKA UNTUK ADMIN
        // ================================================================
        else if (currentUser.role == Role.ADMIN) {
            val waitingList = queues.filter { it.status == QueueStatus.MENUNGGU }
            val targetRoute = AdminMenu.Monitoring.route // Arahkan ke Monitoring

            if (waitingList.size >= 10 && lastPatientCount < 10) {
                sendSmartNotification(context, 999, "⚠️ Antrian Padat", "${waitingList.size} pasien menunggu.", "MEDIS", targetRoute)
            }
            lastPatientCount = waitingList.size

            // TUTUP
            val closingTimeMinutes = (practiceStatus?.closingHour ?: 21) * 60
            if (!hasNotifiedClosing && (closingTimeMinutes - currentTimeTotalMinutes) in 1..30) {
                sendSmartNotification(context, 777, "🌙 Persiapan Tutup", "30 Menit lagi tutup.", "MEDIS", targetRoute)
                hasNotifiedClosing = true
            }
        }
    }
}