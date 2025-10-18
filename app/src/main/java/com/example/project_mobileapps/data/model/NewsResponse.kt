package com.example.project_mobileapps.data.model.news

import com.squareup.moshi.Json

/**
 * Merepresentasikan struktur level atas (root) dari respons JSON yang diterima dari NewsAPI.
 *
 * @property articles Daftar objek [Article] yang merupakan isi utama dari respons berita.
 */
data class NewsResponse(@Json(name = "articles") val articles: List<Article>)

/**
 * Merepresentasikan satu artikel berita individual dari NewsAPI.
 * Anotasi `@Json` digunakan untuk memetakan nama field di JSON (snake_case atau camelCase)
 * ke nama properti di kelas Kotlin (camelCase).
 *
 * @property title Judul lengkap dari artikel berita.
 * @property source Objek [Source] yang berisi informasi tentang sumber berita (misal: "CNN", "BBC News").
 * @property urlToImage URL yang menunjuk ke gambar utama atau thumbnail artikel.
 * @property url URL asli yang mengarah ke artikel berita lengkap di situs web sumbernya.
 */
data class Article(
    @Json(name = "title")
    val title: String?,
    @Json(name = "source")
    val source: Source?,
    @Json(name = "urlToImage")
    val urlToImage: String?,
    @Json(name = "url")
    val url: String?
)

/**
 * Merepresentasikan sumber dari artikel berita.
 *
 * @property name Nama dari sumber berita (contoh: "TechCrunch").
 */
data class Source(@Json(name = "name") val name: String?)