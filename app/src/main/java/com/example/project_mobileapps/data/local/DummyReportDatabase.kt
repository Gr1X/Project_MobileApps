package com.example.project_mobileapps.data.local

// Data class untuk merepresentasikan data per hari
/**
 * Model data sederhana untuk merepresentasikan data laporan harian.
 * Digunakan untuk mengisi komponen UI seperti diagram batang (bar chart)
 * di dashboard admin.
 *
 * @property day Singkatan hari (misal: "Sen", "Sel").
 * @property totalPatients Jumlah total pasien yang berkunjung pada hari tersebut.
 */
data class DailyReport(val day: String, val totalPatients: Int)
/**
 * Singleton object yang menyediakan data laporan mingguan (weekly report) palsu.
 * Digunakan untuk pengembangan dan pengujian fitur dashboard admin.
 */
object DummyReportDatabase {
    /**
     * Daftar (list) statis yang berisi data [DailyReport] untuk satu minggu.
     * Digunakan sebagai sumber data untuk chart laporan mingguan.
     */
    val weeklyReport = listOf(
        DailyReport("Sen", 25),
        DailyReport("Sel", 30),
        DailyReport("Rab", 28),
        DailyReport("Kam", 35),
        DailyReport("Jum", 40),
        DailyReport("Sab", 15),
        DailyReport("Min", 10)
    )
}