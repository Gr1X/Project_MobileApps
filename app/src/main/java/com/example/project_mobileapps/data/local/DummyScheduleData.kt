package com.example.project_mobileapps.data.local
/**
 * Model data yang merepresentasikan jadwal operasional klinik untuk satu hari.
 * Digunakan oleh [DummyScheduleDatabase] dan kemungkinan oleh ViewModel
 * untuk mengelola pengaturan jadwal dokter.
 *
 * @property dayOfWeek Nama hari (misal: "Senin", "Selasa").
 * @property isOpen Status boolean, `true` jika klinik buka pada hari ini, `false` jika tutup.
 * @property startTime Waktu mulai operasional (format "HH:mm"), relevan jika [isOpen] true.
 * @property endTime Waktu selesai operasional (format "HH:mm"), relevan jika [isOpen] true.
 */
data class DailyScheduleData(
    val dayOfWeek: String,
    var isOpen: Boolean,
    var startTime: String,
    var endTime: String
)
/**
 * Singleton object yang bertindak sebagai database sementara untuk jadwal mingguan dokter.
 */
object DummyScheduleDatabase {
    /**
     * Map yang dapat diubah (mutable) untuk menyimpan jadwal mingguan.
     * - Key (kunci): String yang merupakan ID unik dokter (misal: "doc_123").
     * - Value (nilai): Daftar [DailyScheduleData] yang berisi jadwal 7 hari untuk dokter tersebut.
     *
     * Dibuat mutable untuk menyimulasikan pembaruan jadwal oleh admin.
     */
    val weeklySchedules = mutableMapOf(
        // Jadwal default untuk dokter dengan ID "doc_123"
        "doc_123" to mutableListOf(
            DailyScheduleData("Senin", true, "09:00", "17:00"),
            DailyScheduleData("Selasa", true, "09:00", "17:00"),
            DailyScheduleData("Rabu", true, "09:00", "17:00"),
            DailyScheduleData("Kamis", true, "09:00", "17:00"),
            DailyScheduleData("Jumat", true, "09:00", "15:00"),
            DailyScheduleData("Sabtu", false, "00:00", "00:00"), // Tutup
            DailyScheduleData("Minggu", false, "00:00", "00:00") // Tutup
        )
    )
}