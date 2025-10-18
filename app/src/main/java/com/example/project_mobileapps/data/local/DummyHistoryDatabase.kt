package com.example.project_mobileapps.data.local

import com.example.project_mobileapps.data.model.HistoryItem
import com.example.project_mobileapps.data.model.QueueStatus
/**
 * Singleton object yang menyediakan data riwayat kunjungan (history) palsu.
 * Berguna untuk pengembangan UI dan pengujian fitur yang menampilkan daftar riwayat
 * kunjungan pasien, khususnya untuk 'pasien01'.
 */
object DummyHistoryDatabase {
    // --- PERUBAHAN DI SINI: Gunakan mutableListOf agar bisa diubah ---
    /**
     * Daftar (list) yang dapat diubah (mutable) yang menyimpan item riwayat kunjungan.
     * Dibuat mutable untuk memungkinkan simulasi penambahan riwayat baru saat
     * antrian pasien selesai.
     */
    val history = mutableListOf(
        HistoryItem(
            visitId = "hist001",
            userId = "pasien01",
            doctorName = "Dr. Budi Santoso",
            visitDate = "5 Oktober 2025",
            initialComplaint = "Demam dan batuk",
            status = QueueStatus.SELESAI
        ),
        HistoryItem(
            visitId = "hist002",
            userId = "pasien01",
            doctorName = "Dr. Budi Santoso",
            visitDate = "12 September 2025",
            initialComplaint = "Pemeriksaan rutin",
            status = QueueStatus.SELESAI
        ),
        HistoryItem(
            visitId = "hist003",
            userId = "pasien01",
            doctorName = "Dr. Budi Santoso",
            visitDate = "3 Agustus 2025",
            initialComplaint = "Sakit kepala",
            status = QueueStatus.SELESAI
        )
    )
}