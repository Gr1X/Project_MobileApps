package com.example.project_mobileapps.data.repo

import com.example.project_mobileapps.data.model.Doctor
/**
 * Repository yang mengelola data profil dokter.
 * Dalam implementasi ini, data dokter di-hardcode karena aplikasi
 * ini hanya berfokus pada satu klinik/dokter.
 */
class DoctorRepository {
    // Definisikan satu-satunya dokter di sini
    /**
     * Objek data [Doctor] yang di-hardcode.
     * Ini adalah satu-satunya dokter yang digunakan di seluruh aplikasi.
     */
    private val theOnlyDoctor = Doctor(
        id = "doc_123", // ID unik untuk dokter ini
        name = "Dr. Budi Santoso",
        specialization = "Dokter Umum Klinik Pribadi",
        photoUrl = "",
        schedule = "Senin - Jumat | 09:00 - 17:00"
    )

    // Fungsi untuk mendapatkan data dokter tersebut
    /**
     * Fungsi sinkron untuk mendapatkan data dokter utama.
     * @return Objek [Doctor] (Dr. Budi Santoso).
     */
    fun getTheOnlyDoctor(): Doctor {
        return theOnlyDoctor
    }

    // Fungsi ini tetap ada untuk kompatibilitas
    /**
     * Fungsi asinkron untuk mengambil data dokter berdasarkan ID.
     * Tetap dibuat `suspend` untuk menjaga kompatibilitas jika nanti diganti
     * dengan database sungguhan.
     * @param doctorId ID dokter yang dicari.
     * @return [Doctor] jika ID cocok, `null` jika tidak.
     */
    suspend fun getDoctorById(doctorId: String): Doctor? {
        return if (doctorId == theOnlyDoctor.id) theOnlyDoctor else null
    }
}