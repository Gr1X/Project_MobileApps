package com.example.project_mobileapps.data.model

import com.google.firebase.firestore.DocumentId

/**
 * MEDICINES (Master Data / Katalog Obat)
 * Hanya sebagai referensi untuk dropdown resep dokter.
 * Tidak ada stok/harga karena pasien menebus di apotik luar.
 */
data class Medicine(
    @DocumentId
    var id: String = "",
    val name: String = "",
    val category: String = "",
    val form: String = ""
)