package com.example.project_mobileapps.data.model

/**
 * Merepresentasikan satu item dalam riwayat kunjungan seorang pasien.
 * Data ini dibuat setelah sebuah sesi konsultasi ditandai 'SELESAI'.
 *
 * @property visitId ID unik untuk setiap kunjungan.
 * @property userId ID dari pengguna (pasien) yang melakukan kunjungan.
 * @property doctorName Nama dokter yang menangani pada saat kunjungan.
 * @property visitDate Tanggal kunjungan dalam format String yang sudah diformat.
 * @property initialComplaint Keluhan awal yang diinput oleh pasien saat mendaftar antrian.
 * @property status Status akhir dari antrian pada saat itu (biasanya SELESAI atau DIBATALKAN).
 */
data class HistoryItem(
    val visitId: String,
    val userId: String,
    val doctorName: String,
    val visitDate: String,
    val initialComplaint: String,
    val status: QueueStatus
)