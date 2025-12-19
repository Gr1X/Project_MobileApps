package com.example.project_mobileapps.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * MEDICAL_RECORDS (Data Klinis Permanen)
 * Disimpan selamanya untuk riwayat pasien.
 */
data class MedicalRecord(
    @DocumentId
    val id: String = "",

    // Traceability (Jejak data)
    val queueId: String = "",      // ID Antrian asalnya
    val patientId: String = "",    // ID Pasien
    val doctorId: String = "",     // ID Dokter
    val doctorName: String = "",   // Nama Dokter saat periksa

    @ServerTimestamp
    val createdAt: Date = Date(),  // Tanggal & Jam Periksa

    // --- Data Subjektif & Assessment ---
    val complaint: String = "",    // Keluhan Pasien
    val diagnosis: String = "",    // Diagnosa Dokter (ICD-10 style atau bebas)
    val medicalAction: String = "",// Tindakan yang dilakukan di klinik (jika ada)
    val doctorNotes: String = "",  // Catatan khusus (misal: "Kontrol 3 hari lagi")

    // --- Data Objektif (Tanda Vital) ---
    val vitalSigns: VitalSigns = VitalSigns(),

    // --- Plan (Resep Obat) ---
    // List item obat yang harus ditebus pasien
    val prescriptions: List<PrescriptionItem> = emptyList()
)

data class VitalSigns(
    val weightKg: Double = 0.0,
    val heightCm: Double = 0.0,
    val bloodPressure: String = "", // Contoh: "120/80"
    val temperatureC: Double = 0.0,
    val physicalExamNotes: String = "" // Contoh: "Tenggorokan merah"
)

/**
 * Item Resep untuk ditebus di Apotik
 */
data class PrescriptionItem(
    val medicineId: String = "",   // ID dari master Medicine (opsional, buat tracking)
    val medicineName: String = "", // Nama obat (snapshot saat diresepkan)
    val dosage: String = "",       // Dosis: "500mg" atau "1 sendok takar"
    val instructions: String = "", // Signa: "3x1 sesudah makan"
    val quantity: String = ""      // Jumlah tebus: "10 Butir" atau "1 Botol"
)