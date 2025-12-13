package com.example.project_mobileapps.data.model.ml

import com.squareup.moshi.Json

// =================================================================
// 1. REQUEST BODY (INPUT)
// =================================================================
/**
 * Model data yang dikirim ke API untuk prediksi.
 * Field name di dalam @Json harus SAMA PERSIS dengan dokumentasi Swagger.
 */
data class MealPlanRequest(
    @Json(name = "bmi") val bmi: Double,
    @Json(name = "age") val age: Int,
    // [PERBAIKAN] Ubah Int -> Double agar support desimal (mmol/L)
    @Json(name = "fgb") val glucose: Double,
    @Json(name = "avg_systolyc") val systolic: Int,
    @Json(name = "avg_dystolyc") val diastolic: Int,
    // [PERBAIKAN] Ubah Int -> Double (pmol/L)
    @Json(name = "insulin") val insulin: Double
)

// =================================================================
// 2. RESPONSE BODY (OUTPUT)
// =================================================================
/**
 * Model data utama yang diterima dari API.
 * Strukturnya bersarang (Nested) sesuai JSON response.
 */
data class MealPlanResponse(
    @Json(name = "prediction") val prediction: String,       // Contoh: "Diabetes"
    @Json(name = "confidence") val confidence: Double,       // Contoh: 0.98
    @Json(name = "meal_plan") val mealPlan: MealPlan,        // Objek menu makanan
    @Json(name = "nutrition") val nutrition: NutritionInfo?  // Objek detail gizi
)

/**
 * Detail nama makanan per waktu makan.
 */
data class MealPlan(
    @Json(name = "breakfast") val breakfast: String,
    @Json(name = "lunch") val lunch: String,
    @Json(name = "dinner") val dinner: String
)

/**
 * Pembungkus informasi nutrisi per waktu makan.
 */
data class NutritionInfo(
    @Json(name = "breakfast") val breakfast: Macro,
    @Json(name = "lunch") val lunch: Macro,
    @Json(name = "dinner") val dinner: Macro
)

/**
 * Detail angka gizi (Makronutrisi & Mikronutrisi).
 * Kita ambil semua data yang tersedia di API agar fleksibel untuk UI.
 */
data class Macro(
    @Json(name = "energy_kcal") val calories: Double,
    @Json(name = "protein_g") val protein: Double,
    @Json(name = "carbs") val carbs: Double,
    @Json(name = "fat_g") val fat: Double,
    @Json(name = "fibre_g") val fiber: Double,
    @Json(name = "freesugar_g") val sugar: Double,
    @Json(name = "cholestrol_mg") val cholesterol: Double,
    @Json(name = "calcium_mg") val calcium: Double
)