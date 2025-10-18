package com.example.project_mobileapps.data.model

import java.util.Date

/**
* Enum yang merepresentasikan semua kemungkinan status untuk sebuah item antrian.
* Status ini melacak perjalanan pasien dari pendaftaran hingga selesai.
*/
enum class QueueStatus {
    /** Pasien sudah mendaftar dan sedang menunggu giliran. Ini adalah status awal. */
    MENUNGGU,
    /** Pasien telah dipanggil oleh dokter/admin dan diharapkan segera menuju ruang periksa. */
    DIPANGGIL,
    /** Pasien sudah berada di dalam ruang periksa dan sedang dalam sesi konsultasi. */
    DILAYANI,
    /** Sesi konsultasi pasien telah selesai. Item ini akan menjadi data riwayat. */
    SELESAI,
    /** Pasien membatalkan antriannya. */
    DIBATALKAN
}


/**
 * Merepresentasikan satu "tiket" antrian untuk seorang pasien pada hari tertentu.
 *
 * @property queueNumber Nomor urut unik untuk antrian pada hari itu.
 * @property userId ID dari pengguna (pasien) yang memiliki antrian ini.
 * @property userName Nama pengguna (pasien), disimpan untuk kemudahan display.
 * @property doctorId ID dokter yang dituju.
 * @property keluhan Keluhan awal yang diinput oleh pasien saat mendaftar.
 * @property status Status antrian saat ini, menggunakan enum [QueueStatus].
 * @property createdAt Timestamp kapan antrian ini dibuat (pasien mendaftar).
 * @property calledAt Timestamp kapan pasien dipanggil (status berubah menjadi DIPANGGIL).
 * @property startedAt Timestamp kapan konsultasi dimulai (status berubah menjadi DILAYANI).
 * @property finishedAt Timestamp kapan konsultasi selesai (status berubah menjadi SELESAI).
 * @property hasBeenLate Flag untuk menandai apakah pasien ini pernah terlambat merespon panggilan.
 */
data class QueueItem(
    val queueNumber: Int,
    val userId: String,
    val userName: String,
    val doctorId: String,
    val keluhan: String,
    var status: QueueStatus = QueueStatus.MENUNGGU,
    val createdAt: Date = Date(),
    var calledAt: Date? = null,
    var startedAt: Date? = null,
    var finishedAt: Date? = null,
    var hasBeenLate: Boolean = false
)

/**
 * Menyimpan state atau status keseluruhan dari praktik dokter pada hari berjalan.
 *
 * @property doctorId ID dokter yang memiliki status praktik ini.
 * @property doctorName Nama dokter, disimpan untuk kemudahan display.
 * @property currentServingNumber Nomor antrian pasien yang saat ini sedang dilayani (status DILAYANI). Bernilai 0 jika tidak ada.
 * @property lastQueueNumber Nomor antrian terakhir yang dikeluarkan pada hari itu. Digunakan untuk generate nomor berikutnya.
 * @property dailyPatientLimit Batas maksimal jumlah pasien yang bisa mendaftar dalam satu hari.
 * @property estimatedServiceTimeInMinutes Estimasi waktu layanan rata-rata per pasien dalam menit. Digunakan untuk kalkulasi waktu tunggu.
 * @property isPracticeOpen Menandakan apakah pendaftaran antrian sedang dibuka atau ditutup.
 * @property totalServed Jumlah total pasien yang telah selesai dilayani pada hari itu.
 * @property openingHour Jam buka praktik (format 24 jam).
 * @property closingHour Jam tutup praktik (format 24 jam).
 * @property patientCallTimeLimitMinutes Batas waktu toleransi (dalam menit) bagi pasien untuk hadir setelah dipanggil.
 */
data class PracticeStatus(
    val doctorId: String,
    val doctorName: String,
    var currentServingNumber: Int = 0,
    var lastQueueNumber: Int = 0,
    val dailyPatientLimit: Int = 50,
    val estimatedServiceTimeInMinutes: Int = 30,
    val isPracticeOpen: Boolean = false,
    var totalServed: Int = 0,
    val openingHour: Int = 9,
    val closingHour: Int = 17,
    val patientCallTimeLimitMinutes: Int = 15
)