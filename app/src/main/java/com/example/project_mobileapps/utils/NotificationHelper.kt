package com.example.project_mobileapps.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log // Tambah Import Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.project_mobileapps.MainActivity
import com.example.project_mobileapps.R

object NotificationHelper {
    private const val CHANNEL_ID = "antrian_channel_id"
    private const val CHANNEL_NAME = "Update Antrian"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifikasi status antrian"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            Log.d("DEBUG_NOTIF", "Channel Notifikasi Dibuat")
        }
    }

    fun showNotification(context: Context, title: String, message: String) {
        Log.d("DEBUG_NOTIF", "Mencoba memunculkan notifikasi: $title - $message") // LOG PENTING

        // Cek Izin Dulu (Khusus Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("DEBUG_NOTIF", "❌ GAGAL: Izin POST_NOTIFICATIONS belum diberikan user!")
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // GUNAKAN ICON BAWAAN ANDROID (Supaya pasti muncul)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
            Log.d("DEBUG_NOTIF", "✅ SUKSES: Notifikasi dikirim ke sistem")
        } catch (e: SecurityException) {
            Log.e("DEBUG_NOTIF", "❌ ERROR Security: ${e.message}")
        } catch (e: Exception) {
            Log.e("DEBUG_NOTIF", "❌ ERROR Lain: ${e.message}")
        }
    }
}