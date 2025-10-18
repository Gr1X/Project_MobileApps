package com.example.project_mobileapps.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Merepresentasikan data seorang dokter.
 * Model ini dirancang untuk kompatibilitas dengan Firebase Firestore.
 *
 * @property id ID unik untuk dokter, digunakan sebagai kunci di database.
 * @property name Nama lengkap dokter.
 * @property specialization Bidang spesialisasi dokter (contoh: "Dokter Umum").
 * @property photoUrl URL yang menunjuk ke foto profil dokter.
 * @property schedule Teks singkat yang menampilkan jadwal praktik umum dokter (contoh: "Senin - Jumat | 09:00 - 17:00").
 */
@IgnoreExtraProperties // Anotasi ini memberitahu Firestore untuk mengabaikan properti tambahan saat deserialisasi.
data class Doctor(
    val id: String = "",
    val name: String = "",
    val specialization: String = "",
    val photoUrl: String = "",
    val schedule: String = ""
)