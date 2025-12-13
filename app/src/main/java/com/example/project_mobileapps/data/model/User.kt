package com.example.project_mobileapps.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Enum untuk merepresentasikan jenis kelamin pengguna.
 */
enum class Gender {
    PRIA,
    WANITA
}

/**
 * Enum untuk merepresentasikan peran (role) pengguna di dalam sistem.
 * Peran ini menentukan hak akses dan alur UI yang akan ditampilkan kepada pengguna.
 */
enum class Role {
    /** Pengguna adalah seorang pasien. */
    PASIEN,

    /** Pengguna adalah seorang dokter. */
    DOKTER,

    /** Pengguna adalah seorang admin atau asisten dokter. */
    ADMIN
}

/**
 * Merepresentasikan data seorang pengguna (user) di dalam aplikasi.
 * Model ini digunakan untuk semua jenis peran (Pasien, Dokter, Admin).
 *
 * @property uid ID unik pengguna, biasanya berasal dari sistem otentikasi (contoh: Firebase Auth UID).
 * @property name Nama lengkap pengguna.
 * @property email Alamat email pengguna, digunakan untuk login.
 * @property password Password pengguna. Properti ini bisa bernilai null (opsional) terutama jika menggunakan metode login lain seperti Google Sign-In.
 * @property role Peran pengguna dalam sistem, menggunakan enum [Role].
 * @property phoneNumber Nomor telepon pengguna. Default "N/A".
 * @property gender Jenis kelamin pengguna, menggunakan enum [Gender]. Default PRIA.
 * @property dateOfBirth Tanggal lahir pengguna dalam format String (contoh: "1990-05-15"). Default "N/A".
 * @property profilePictureUrl URL yang menunjuk ke foto profil pengguna.
 */
data class User(
    val uid: String = "",
    val username: String = "",
    val name: String = "",
    val email: String = "",
    @get:Exclude val password: String? = null,
    val role: Role = Role.PASIEN,
    val phoneNumber: String = "N/A",
    val gender: Gender = Gender.PRIA,
    val dateOfBirth: String = "N/A",
    val profilePictureUrl: String = ""
) {
    @PropertyName("password")
    @get:Exclude
    private val passwordForFirestore: String? = password
}