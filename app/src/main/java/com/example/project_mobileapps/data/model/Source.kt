package com.example.project_mobileapps.data.model

/**
 * Merepresentasikan sumber dari sebuah artikel berita.
 * Model ini biasanya digunakan sebagai bagian dari model [Article] yang lebih besar,
 * terutama saat parsing data dari API berita.
 *
 * @property id ID unik atau slug untuk sumber berita (misal: "techcrunch"). Bisa bernilai null.
 * @property name Nama lengkap sumber berita yang ditampilkan kepada pengguna (misal: "TechCrunch"). Bisa bernilai null.
 */
data class Source(
    val id: String?,
    val name: String?
)