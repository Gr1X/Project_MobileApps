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

private val retrofitML = Retrofit.Builder()
    .baseUrl("https://diabetesmealplanpredictionapi-production-96e7.up.railway.app/")
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()

interface MlApiService {
    // Endpoint sesuai dokumentasi Swagger: /predict (POST)
    @POST("predict")
    suspend fun predictMealPlan(@Body request: MealPlanRequest): MealPlanResponse
}

// 4. Singleton Accessor
object MlApiClient {
    val service: MlApiService by lazy {
        retrofitML.create(MlApiService::class.java)
    }
}