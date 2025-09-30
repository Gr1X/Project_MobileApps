package com.example.project_mobileapps.data.remote

import com.example.project_mobileapps.BuildConfig
import com.example.project_mobileapps.data.model.news.NewsResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
    .baseUrl("https://newsapi.org/")
    .build()

interface NewsApiService {
    @GET("v2/top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String = "us",
        @Query("category") category: String = "health",
        @Query("apiKey") apiKey: String = BuildConfig.NEWS_API_KEY
    ): NewsResponse
}

object NewsApi {
    val retrofitService: NewsApiService by lazy { retrofit.create(NewsApiService::class.java) }
}