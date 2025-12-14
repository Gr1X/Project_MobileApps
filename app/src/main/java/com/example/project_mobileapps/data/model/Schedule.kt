package com.example.project_mobileapps.data.model

import com.google.firebase.firestore.PropertyName

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
    val dayOfWeek: String = "",
    @get:PropertyName("open")
    @set:PropertyName("open")
    var isOpen: Boolean = false,
    var startTime: String = "",
    var endTime: String = "",

    // --- TAMBAHAN BARU: PENGATURAN ISTIRAHAT ---
    @get:PropertyName("isBreakEnabled")
    @set:PropertyName("isBreakEnabled")
    var isBreakEnabled: Boolean = false,

    @get:PropertyName("breakStartTime")
    @set:PropertyName("breakStartTime")
    var breakStartTime: String = "12:00",

    @get:PropertyName("breakEndTime")
    @set:PropertyName("breakEndTime")
    var breakEndTime: String = "13:00"
)

