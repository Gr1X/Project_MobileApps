package com.example.project_mobileapps.data.local

// Data class untuk merepresentasikan data per hari
data class DailyReport(val day: String, val totalPatients: Int)

object DummyReportDatabase {
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