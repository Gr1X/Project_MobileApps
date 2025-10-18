package com.example.project_mobileapps.data.remote

import com.example.project_mobileapps.BuildConfig
import com.example.project_mobileapps.data.model.news.NewsResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Konfigurasi dan pembuatan instance Retrofit untuk berkomunikasi dengan NewsAPI.
 * Instance ini bersifat privat untuk file ini, memastikan semua konfigurasi terpusat di sini.
 */
private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
    .baseUrl("https://newsapi.org/")
    .build()

/**
 * Interface yang mendefinisikan endpoint-endpoint dari NewsAPI yang akan digunakan oleh aplikasi.
 * Retrofit akan secara otomatis mengimplementasikan interface ini untuk melakukan panggilan HTTP.
 */
interface NewsApiService {
    /**
     * Mengambil berita utama (top headlines) dari NewsAPI.
     * Ini adalah suspend function, artinya harus dipanggil dari dalam coroutine.
     *
     * @param country Kode negara 2 huruf (misal: "us", "id") untuk sumber berita.
     * @param category Kategori berita yang ingin diambil (misal: "health", "technology").
     * @param apiKey Kunci API untuk otentikasi dengan NewsAPI, diambil dari BuildConfig.
     * @return [NewsResponse] Objek yang berisi daftar artikel berita.
     */
    @GET("v2/top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String = "us",
        @Query("category") category: String = "health",
        @Query("apiKey") apiKey: String = BuildConfig.NEWS_API_KEY
    ): NewsResponse
}

/**
 * Object singleton untuk menyediakan akses mudah ke instance [NewsApiService].
 * Menggunakan `lazy` delegate memastikan bahwa instance Retrofit hanya dibuat sekali
 * saat pertama kali diakses, yang merupakan praktik yang efisien.
 */
object NewsApi {
    val retrofitService: NewsApiService by lazy { retrofit.create(NewsApiService::class.java) }
}