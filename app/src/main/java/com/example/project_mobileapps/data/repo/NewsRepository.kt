
package com.example.project_mobileapps.data.repo

import android.util.Log
import com.example.project_mobileapps.data.model.news.Article
import com.example.project_mobileapps.data.remote.NewsApi

class NewsRepository {
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