package com.example.project_mobileapps.data.remote

import com.example.project_mobileapps.data.model.ml.MealPlanRequest
import com.example.project_mobileapps.data.model.ml.MealPlanResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

// PERBAIKAN DI SINI
private val retrofitML = Retrofit.Builder()
    // 1. Gunakan URL root server (Hapus bagian /docs#...)
    // 2. Wajib diakhiri dengan '/'
    .baseUrl("https://diabetesmealplanpredictionapi.onrender.com/")
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()

interface MlApiService {
    // Endpoint 'predict' akan disambung ke Base URL
    @POST("predict")
    suspend fun predictMealPlan(@Body request: MealPlanRequest): MealPlanResponse
}

object MlApiClient {
    val service: MlApiService by lazy {
        retrofitML.create(MlApiService::class.java)
    }
}