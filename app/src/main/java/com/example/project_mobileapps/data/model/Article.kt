 package com.example.project_mobileapps.data.model

 /**
  * Merepresentasikan satu item artikel berita.
  * Model ini digunakan untuk data yang tidak terkait langsung dengan API berita (NewsAPI),
  * memberikan struktur data yang lebih umum jika diperlukan.
  *
  * @property author Nama penulis atau sumber artikel.
  * @property title Judul dari artikel berita.
  * @property description Deskripsi singkat atau kutipan dari artikel.
  * @property url Link URL asli menuju artikel lengkap.
  * @property urlToImage Link URL ke gambar utama atau thumbnail artikel.
  * @property publishedAt Tanggal dan waktu artikel dipublikasikan dalam format String.
  */

data class Article(
    val author: String?,
    val title: String?,
    val description: String?,
    val url: String?,
    val urlToImage: String?,
    val publishedAt: String?
)
