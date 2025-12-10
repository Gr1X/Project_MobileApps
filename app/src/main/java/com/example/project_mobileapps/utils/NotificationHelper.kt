// File: utils/NotificationHelper.kt
package com.example.project_mobileapps.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.project_mobileapps.MainActivity
import com.example.project_mobileapps.R

object NotificationHelper {
    private const val CHANNEL_PATIENT = "channel_patient_updates"
    private const val CHANNEL_MEDICAL = "channel_medical_staff"
    private const val THEME_COLOR = 0xFF6D80E3.toInt()

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val patientChannel = NotificationChannel(CHANNEL_PATIENT, "Update Antrian Pasien", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifikasi giliran dan status antrian"
                enableVibration(true)
                lightColor = THEME_COLOR
            }
            val medicalChannel = NotificationChannel(CHANNEL_MEDICAL, "Info Klinik (Dokter/Admin)", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Update pasien baru dan operasional"
                lightColor = THEME_COLOR
            }
            manager.createNotificationChannels(listOf(patientChannel, medicalChannel))
        }
    }

    /**
     * Menampilkan Notifikasi di Status Bar.
     * @param notificationId ID Unik (Gunakan QueueNumber agar notif lama ter-update, bukan numpuk)
     * @param roleTarget "PASIEN" atau "MEDIS"
     */
    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        roleTarget: String = "PASIEN",
        targetRoute: String? = null
    ) {
        // Cek Izin Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return // Jangan crash jika tidak ada izin
            }
        }

        val channelId = if (roleTarget == "PASIEN") CHANNEL_PATIENT else CHANNEL_MEDICAL

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // SISIPKAN DATA RUTE
            if (targetRoute != null) {
                putExtra("TARGET_ROUTE", targetRoute)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent, // Pakai ID notif agar request code unik
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setColor(THEME_COLOR)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Handle permission error
        }
    }

    // Fungsi untuk membatalkan notifikasi (misal saat antrian selesai)
    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}