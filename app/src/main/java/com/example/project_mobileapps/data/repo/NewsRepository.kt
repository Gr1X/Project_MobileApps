
package com.example.project_mobileapps.data.repo

import android.util.Log
import com.example.project_mobileapps.data.model.news.Article
import com.example.project_mobileapps.data.remote.NewsApi
/**
 * Repository yang bertanggung jawab untuk mengambil data berita kesehatan
 * dari sumber eksternal (NewsAPI) melalui [NewsApi] service (Retrofit).
 */
class NewsRepository {
    /**
     * Mengambil daftar artikel berita kesehatan teratas.
     * Fungsi ini adalah `suspend` karena melakukan panggilan jaringan (network call).
     *
     * @return Daftar [Article] jika berhasil. Mengembalikan daftar kosong ([emptyList])
     * jika terjadi kegagalan koneksi atau error API.
     */
    suspend fun getHealthNews(): List<Article> {
        return try {
            NewsApi.retrofitService.getTopHeadlines().articles
        } catch (e: Exception) {
            // 2. TAMBAHKAN LOG ERROR INI
            Log.e("NewsApiError", "Gagal mengambil berita dari API", e)
            emptyList()
        }
    }
}