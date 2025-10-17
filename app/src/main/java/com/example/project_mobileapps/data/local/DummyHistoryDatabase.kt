package com.example.project_mobileapps.data.local

import com.example.project_mobileapps.data.model.HistoryItem
import com.example.project_mobileapps.data.model.QueueStatus

object DummyHistoryDatabase {
    // --- PERUBAHAN DI SINI: Gunakan mutableListOf agar bisa diubah ---
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