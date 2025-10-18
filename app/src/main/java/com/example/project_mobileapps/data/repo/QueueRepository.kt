package com.example.project_mobileapps.data.repo

import com.example.project_mobileapps.data.local.DailyReport
import com.example.project_mobileapps.data.local.DailyScheduleData
import com.example.project_mobileapps.data.model.HistoryItem
import com.example.project_mobileapps.data.model.PracticeStatus
import com.example.project_mobileapps.data.model.QueueItem
import kotlinx.coroutines.flow.StateFlow
/**
 * Interface (kontrak) yang mendefinisikan semua operasi terkait data antrian dan manajemen praktik.
 * Ini memisahkan 'apa' (definisi fungsi) dari 'bagaimana' (implementasi, misal: DummyQueueRepository).
 * Memudahkan untuk menukar implementasi dummy dengan implementasi nyata (misal: Firebase) di masa depan.
 */
interface QueueRepository {
    /**
     * Aliran data reaktif (StateFlow) yang menyimpan status praktik terkini.
     * Key adalah [String] (doctorId), Value adalah [PracticeStatus].
     * UI akan mengamati (observe) aliran ini untuk update real-time.
     */
    val practiceStatusFlow: StateFlow<Map<String, PracticeStatus>>
    /**
     * Aliran data reaktif (StateFlow) yang menyimpan daftar antrian harian.
     * UI (Pasien, Dokter, Admin) akan mengamati aliran ini.
     */
    val dailyQueuesFlow: StateFlow<List<QueueItem>>
    /**
     * Mencoba mendaftarkan pasien ke dalam antrian.
     * @param doctorId ID dokter yang dituju.
     * @param userId ID unik pasien.
     * @param userName Nama pasien.
     * @param keluhan Keluhan awal pasien.
     * @return [Result.success] berisi [QueueItem] jika berhasil, [Result.failure] jika gagal (misal: antrian penuh, sudah terdaftar).
     */
    suspend fun takeQueueNumber(doctorId: String, userId: String, userName: String, keluhan: String): Result<QueueItem>
    /**
     * Membatalkan nomor antrian yang dimiliki oleh pasien.
     * @param userId ID pasien yang membatalkan.
     * @param doctorId ID dokter terkait.
     * @return [Result.success] jika berhasil.
     */
    suspend fun cancelQueue(userId: String, doctorId: String): Result<Unit>
    /**
     * Dipanggil oleh dokter untuk memanggil pasien berikutnya dari antrian 'MENUNGGU'.
     * @param doctorId ID dokter yang memanggil.
     * @return [Result.success] jika berhasil, [Result.failure] jika tidak ada antrian lagi.
     */
    suspend fun callNextPatient(doctorId: String): Result<Unit>
    /**
     * Mengatur status praktik (Buka/Tutup) secara manual oleh dokter/admin.
     * @param doctorId ID dokter yang statusnya diubah.
     * @param isOpen Status baru (`true` untuk buka, `false` untuk tutup).
     * @return [Result.success] jika berhasil.
     */
    suspend fun setPracticeOpen(doctorId: String, isOpen: Boolean): Result<Unit>
    /**
     * Dipanggil oleh dokter untuk mengonfirmasi kedatangan pasien yang sudah dipanggil.
     * Mengubah status pasien dari 'DIPANGGIL' menjadi 'DILAYANI'.
     * @param queueId Nomor antrian pasien.
     * @param doctorId ID dokter terkait.
     * @return [Result.success] jika berhasil.
     */
    suspend fun confirmPatientArrival(queueId: Int, doctorId: String): Result<Unit>
    /**
     * Dipanggil oleh dokter setelah konsultasi dengan pasien selesai.
     * Mengubah status pasien menjadi 'SELESAI' dan memperbarui statistik.
     * @param queueId Nomor antrian pasien yang selesai.
     * @param doctorId ID dokter terkait.
     * @return [Result.success] jika berhasil.
     */
    suspend fun finishConsultation(queueId: Int, doctorId: String): Result<Unit>  // Konsultasi Selesai
    /**
     * Mengambil daftar riwayat kunjungan untuk pasien tertentu.
     * @param userId ID unik pasien.
     * @return Daftar [HistoryItem] milik pasien tersebut.
     */
    suspend fun getVisitHistory(userId: String): List<HistoryItem>
    /**
     * Memeriksa pasien yang statusnya 'DIPANGGIL' tetapi tidak kunjung datang (terlambat)
     * berdasarkan [PracticeStatus.patientCallTimeLimitMinutes].
     * @param doctorId ID dokter yang antriannya diperiksa.
     */
    suspend fun checkForLatePatients(doctorId: String)
    /**
     * Menambahkan antrian secara manual (oleh Admin) untuk pasien yang datang langsung (walk-in).
     * @param patientName Nama pasien walk-in.
     * @param complaint Keluhan awal.
     * @return [Result.success] berisi [QueueItem] jika berhasil.
     */
    suspend fun addManualQueue(patientName: String, complaint: String): Result<QueueItem>
    /**
     * Mengambil data laporan mingguan untuk dashboard Admin.
     * @return Daftar [DailyReport] untuk 7 hari.
     */
    suspend fun getWeeklyReport(): List<DailyReport>
    /**
     * Mereset seluruh data antrian harian (biasanya dilakukan tengah malam atau oleh Admin).
     */
    suspend fun resetQueue()
    /**
     * Mengambil data jadwal mingguan untuk seorang dokter.
     * @param doctorId ID dokter.
     * @return Daftar [DailyScheduleData] (7 hari).
     */
    suspend fun getDoctorSchedule(doctorId: String): List<DailyScheduleData>
    /**
     * Memperbarui jadwal mingguan seorang dokter.
     * @param doctorId ID dokter.
     * @param newSchedule Daftar [DailyScheduleData] baru.
     * @return [Result.success] jika berhasil disimpan.
     */
    suspend fun updateDoctorSchedule(doctorId: String, newSchedule: List<DailyScheduleData>): Result<Unit>
    /**
     * Memperbarui estimasi waktu layanan rata-rata per pasien (dalam menit).
     * @param doctorId ID dokter.
     * @param minutes Waktu baru dalam menit.
     * @return [Result.success] jika berhasil disimpan.
     */
    suspend fun updateEstimatedServiceTime(doctorId: String, minutes: Int): Result<Unit>
    /**
     * Memperbarui batas waktu tunggu pasien (dalam menit) setelah dipanggil.
     * @param doctorId ID dokter.
     * @param minutes Waktu baru dalam menit.
     * @return [Result.success] jika berhasil disimpan.
     */
    suspend fun updatePatientCallTimeLimit(doctorId: String, minutes: Int): Result<Unit>
}