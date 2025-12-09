package com.example.project_mobileapps.utils // Sesuaikan dengan package kamu

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CloudinaryHelper {

    // ✅ INI SUDAH DIUPDATE DENGAN CLOUD NAME KAMU
    private const val CLOUD_NAME = "dfgv3amnr"

    // Pastikan di Dashboard Cloudinary namanya "klinik_preset" (tanpa tanda kutip lain)
    private const val UPLOAD_PRESET = "klinik_preset"

    fun init(context: Context) {
        val config = HashMap<String, Any>()
        config["cloud_name"] = CLOUD_NAME
        config["secure"] = true

        try {
            MediaManager.init(context, config)
            Log.d("Cloudinary", "Init Berhasil: $CLOUD_NAME")
        } catch (e: Exception) {
            Log.w("Cloudinary", "MediaManager already initialized")
        }
    }

    suspend fun uploadImage(uri: Uri): String = suspendCancellableCoroutine { continuation ->
        MediaManager.get().upload(uri)
            .unsigned(UPLOAD_PRESET) // Mode Unsigned
            .option("folder", "klinik_images") // Masuk folder klinik_images
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    Log.d("Cloudinary", "Mulai Upload ke $CLOUD_NAME...")
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    // Progress
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as? String ?: ""
                    Log.d("Cloudinary", "Upload Sukses: $url")
                    continuation.resume(url)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    Log.e("Cloudinary", "Error Upload: ${error.description}")
                    continuation.resumeWithException(Exception("Gagal Upload: ${error.description}"))
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    // Reschedule
                }
            })
            .dispatch()
    }
}