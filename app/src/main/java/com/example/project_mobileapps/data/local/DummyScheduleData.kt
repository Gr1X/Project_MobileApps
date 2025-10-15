package com.example.project_mobileapps.data.local

data class DailyScheduleData(
    val dayOfWeek: String,
    var isOpen: Boolean,
    var startTime: String,
    var endTime: String
)

object DummyScheduleDatabase {
    val weeklySchedules = mutableMapOf(
        "doc_123" to mutableListOf(
            DailyScheduleData("Senin", true, "09:00", "17:00"),
            DailyScheduleData("Selasa", true, "09:00", "17:00"),
            DailyScheduleData("Rabu", true, "09:00", "17:00"),
            DailyScheduleData("Kamis", true, "09:00", "17:00"),
            DailyScheduleData("Jumat", true, "09:00", "15:00"),
            DailyScheduleData("Sabtu", false, "00:00", "00:00"),
            DailyScheduleData("Minggu", false, "00:00", "00:00")
        )
    )
}