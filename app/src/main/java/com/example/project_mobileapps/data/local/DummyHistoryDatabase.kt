package com.example.project_mobileapps.data.local

import com.example.project_mobileapps.data.model.HistoryItem

// Di data/local/DummyHistoryDatabase.kt
object DummyHistoryDatabase {
    val history = listOf(
        HistoryItem("hist001", "Dr. Budi Santoso", "5 Oktober 2025", "Demam dan batuk"),
        HistoryItem("hist002", "Dr. Budi Santoso", "12 September 2025", "Pemeriksaan rutin"),
        HistoryItem("hist003", "Dr. Budi Santoso", "3 Agustus 2025", "Sakit kepala")
    )
}