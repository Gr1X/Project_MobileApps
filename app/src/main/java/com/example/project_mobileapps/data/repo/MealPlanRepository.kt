package com.example.project_mobileapps.data.repo

import android.util.Log
import com.example.project_mobileapps.data.model.ml.MealPlanRequest
import com.example.project_mobileapps.data.model.ml.MealPlanResponse
import com.example.project_mobileapps.data.remote.MlApiClient

class MealPlanRepository {

    /**
     * Mengirim data kesehatan ke ML API untuk mendapatkan prediksi.
     */
    suspend fun getPrediction(
        bmi: Double,
        age: Int,
        glucose: Double,
        systolic: Int,
        diastolic: Int,
        insulin: Double
    ): Result<MealPlanResponse> {
        return try {

            val request = MealPlanRequest(
                bmi = bmi,
                age = age,
                glucose = glucose,
                systolic = systolic,
                diastolic = diastolic,
                insulin = insulin
            )

            Log.d("MealPlanRepo", "Mengirim Request: $request")

            val response = MlApiClient.service.predictMealPlan(request)

            Log.d("MealPlanRepo", "Sukses! Respon: ${response.prediction}")
            Result.success(response)

        } catch (e: Exception) {
            Log.e("MealPlanRepo", "Gagal koneksi ke ML API", e)
            Result.failure(e)
        }
    }
}