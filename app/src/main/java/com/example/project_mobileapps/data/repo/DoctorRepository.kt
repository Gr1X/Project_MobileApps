package com.example.project_mobileapps.data.repo

import com.example.project_mobileapps.data.model.Doctor

class DoctorRepository {
    // Definisikan satu-satunya dokter di sini
    private val theOnlyDoctor = Doctor(
        id = "doc_123", // ID unik untuk dokter ini
        name = "Dr. Budi Santoso",
        specialization = "Dokter Umum Klinik Pribadi",
        photoUrl = "",
        schedule = "Senin - Jumat | 09:00 - 17:00"
    )

    // Fungsi untuk mendapatkan data dokter tersebut
    fun getTheOnlyDoctor(): Doctor {
        return theOnlyDoctor
    }

    // Fungsi ini tetap ada untuk kompatibilitas
    suspend fun getDoctorById(doctorId: String): Doctor? {
        return if (doctorId == theOnlyDoctor.id) theOnlyDoctor else null
    }
}