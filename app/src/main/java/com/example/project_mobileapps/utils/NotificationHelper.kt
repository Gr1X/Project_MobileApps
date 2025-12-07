package com.example.project_mobileapps.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.project_mobileapps.MainActivity // Ganti dengan Activity utama Anda
import com.example.project_mobileapps.R // Pastikan import R benar

object NotificationHelper {
    private const val CHANNEL_ID = "queue_channel_id"
    private const val CHANNEL_NAME = "Update Antrian"

    // Wajib dipanggil di onCreate MainActivity
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH // Agar bunyi dan muncul popup
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifikasi status antrian pasien"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, title: String, content: String) {
        // Intent agar saat notif diklik, aplikasi terbuka
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ganti dengan icon app Anda
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            // ID unik (bisa random atau queueNumber) agar notif tidak saling menimpa jika mau history
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            // Handle permission error for Android 13+ here if needed
        }
    }
}