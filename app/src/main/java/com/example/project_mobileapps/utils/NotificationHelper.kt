// File: utils/NotificationHelper.kt
package com.example.project_mobileapps.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.project_mobileapps.MainActivity
import com.example.project_mobileapps.R

object NotificationHelper {
    private const val CHANNEL_ID = "antrian_channel_id"
    private const val CHANNEL_NAME = "Update Antrian"
    private const val CHANNEL_DESC = "Notifikasi status antrian dan panggilan"

    // Fungsi untuk membuat Channel (Wajib dipanggil sekali saat App Start)
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH // Agar muncul Pop-up (Heads-up)
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Fungsi untuk menampilkan notifikasi
    fun showNotification(context: Context, title: String, message: String) {
        // Intent agar saat notif diklik, aplikasi terbuka
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ganti dengan icon app Anda jika ada
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Penting untuk Heads-up
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Hilang saat diklik
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        try {
            // Gunakan id unik (misal current time) agar notifikasi bisa menumpuk
            // atau ID tetap (misal 1) agar saling menimpa. Kita pakai ID unik.
            val notificationId = System.currentTimeMillis().toInt()
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace() // Izin belum diberikan
        }
    }
}