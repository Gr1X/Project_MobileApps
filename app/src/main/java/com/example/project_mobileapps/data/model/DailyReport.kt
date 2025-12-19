package com.example.project_mobileapps.data.model

/**
 * Data Transfer Object (DTO) untuk visualisasi Laporan Statistik.
 *
 * Model ini adalah hasil AGREGASI (pengelompokan) dari data mentah [QueueItem].
 * Digunakan khusus untuk komponen UI Grafik (Chart), dimana kita hanya
 * membutuhkan Label Sumbu X (Hari) dan Nilai Sumbu Y (Total).
 *
 * Catatan Arsitektur:
 * Model ini tidak memerlukan ID unik (UUID) karena merepresentasikan
 * "Grup Data", bukan "Single Data". Identitasnya adalah [day] itu sendiri.
 *
 * @property day Label untuk sumbu horizontal (misal: "Sen", "Sel", "Minggu 1").
 * @property totalPatients Jumlah total pasien yang berstatus 'SELESAI' pada periode tersebut.
 */
data class DailyReport(
    val label: String, // Label sumbu X (Sen, Sel, Jan, Feb)
    val count: Int     // Nilai sumbu Y (Jumlah pasien)
)