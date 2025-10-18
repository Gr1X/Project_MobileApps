package com.example.project_mobileapps.data.local

import com.example.project_mobileapps.data.model.Gender
import com.example.project_mobileapps.data.model.Role
import com.example.project_mobileapps.data.model.User
/**
 * Singleton object yang berfungsi sebagai database sementara (in-memory) untuk data pengguna.
 * Digunakan selama pengembangan dan pengujian untuk menyimulasikan pengambilan data pengguna
 * tanpa memerlukan koneksi ke database eksternal (seperti Firebase Firestore atau Room).
 */
object DummyUserDatabase {
    /**
     * Daftar (list) yang dapat diubah (mutable) yang menyimpan semua data pengguna dummy.
     * Dibuat mutable agar kita dapat menyimulasikan penambahan atau pengubahan pengguna
     * saat aplikasi berjalan (misalnya untuk menguji fitur registrasi).
     */
    val users = mutableListOf(
        // --- Kumpulan Akun Pasien ---
        User(
            uid = "pasien01", name = "Budi Setiawan", email = "pasien@gmail.com", password = "password",
            role = Role.PASIEN, phoneNumber = "081234567890", gender = Gender.PRIA, dateOfBirth = "1990-05-15"
        ),
        User(
            uid = "pasien02", name = "Citra Lestari", email = "citra@gmail.com", password = "password",
            role = Role.PASIEN, phoneNumber = "081211223344", gender = Gender.WANITA, dateOfBirth = "1995-11-20"
        ),
        User(
            uid = "pasien03", name = "Agus Setiawan", email = "agus@gmail.com", password = "password",
            role = Role.PASIEN, phoneNumber = "081344556677", gender = Gender.PRIA, dateOfBirth = "1988-01-30"
        ),
        User(
            uid = "pasien04", name = "Dewi Anggraini", email = "dewi@gmail.com", password = "password",
            role = Role.PASIEN, phoneNumber = "081577889900", gender = Gender.WANITA, dateOfBirth = "2000-07-07"
        ),
        User(
            uid = "pasien05", name = "Eko Prasetyo", email = "eko@gmail.com", password = "password",
            role = Role.PASIEN, phoneNumber = "081812341234", gender = Gender.PRIA, dateOfBirth = "1992-03-12"
        ),
        User(
            uid = "pasien06", name = "Fitriani Mawar", email = "fitri@gmail.com", password = "password",
            role = Role.PASIEN, phoneNumber = "081956785678", gender = Gender.WANITA, dateOfBirth = "1998-09-25"
        ),
        // Akun Dokter
        User(uid = "dokter01", name = "Dr. Budi Santoso", email = "dokter@gmail.com", password = "dokter",
            role = Role.DOKTER),
        //Akun Admin
        User(uid = "admin01", name = "Admin Klinik", email = "admin@gmail.com", password = "admin",
            role = Role.ADMIN)
    )
}