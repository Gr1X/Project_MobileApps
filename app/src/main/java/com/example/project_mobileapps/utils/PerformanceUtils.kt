import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

// Fungsi ajaib untuk membungkus codingan CRUD
suspend fun <T> trackCrud(traceName: String, block: suspend () -> T): T {
    // 1. Mulai Stopwatch
    val trace: Trace = FirebasePerformance.getInstance().newTrace(traceName)
    trace.start()

    return try {
        // 2. Jalankan codingan aslimu (Simpan ke DB, dll)
        val result = block()

        // 3. Catat Sukses
        trace.putAttribute("status", "success")
        result
    } catch (e: Exception) {
        // 4. Catat Gagal (Kalau error)
        trace.putAttribute("status", "error")
        trace.putAttribute("error_msg", e.message ?: "Unknown")
        throw e
    } finally {
        // 5. Matikan Stopwatch
        trace.stop()
    }
}