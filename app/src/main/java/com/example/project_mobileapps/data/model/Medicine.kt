package com.example.project_mobileapps.data.model

data class Medicine(
    val id: String = "",
    val name: String = "",        // Contoh: "Amoxicillin 500mg"
    val category: String = "",    // Contoh: "Antibiotik"
    val form: String = "Tablet"   // Contoh: Tablet, Sirup, Salep
)