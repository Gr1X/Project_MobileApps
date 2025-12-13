// File: features/patient/mealplan/SmartMealPlanViewModel.kt
package com.example.project_mobileapps.features.patient.mealplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.model.ml.MealPlanResponse
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.data.repo.MealPlanRepository
import com.example.project_mobileapps.ui.components.ToastManager
import com.example.project_mobileapps.ui.components.ToastType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MealPlanUiState(
    val age: String = "",
    val weight: String = "",
    val height: String = "",
    val glucose: String = "",
    val systolic: String = "",
    val diastolic: String = "",
    val insulin: String = "",

    // Auto-calculated
    val bmi: Double = 0.0,

    // UI Status
    val isLoading: Boolean = false,
    val prediction: MealPlanResponse? = null,
    val error: String? = null,

    // Input Errors
    val ageError: String? = null,
    val weightError: String? = null,
    val glucoseError: String? = null
)

class SmartMealPlanViewModel(
    private val repository: MealPlanRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealPlanUiState())
    val uiState: StateFlow<MealPlanUiState> = _uiState.asStateFlow()

    init {
        // --- AUTO FILL LOGIC ---
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    // Hitung Usia dari DOB
                    val calculatedAge = calculateAge(user.dateOfBirth)
                    if (calculatedAge > 0) {
                        _uiState.update { it.copy(age = calculatedAge.toString()) }
                    }
                }
            }
        }
    }

    fun onAgeChange(v: String) { _uiState.update { it.copy(age = v, ageError = null) } }

    fun onWeightChange(v: String) {
        _uiState.update {
            val newState = it.copy(weight = v, weightError = null)
            it.copy(weight = v, weightError = null, bmi = calculateBmi(v, it.height))
        }
    }

    fun onHeightChange(v: String) {
        _uiState.update {
            val newState = it.copy(height = v)
            it.copy(height = v, bmi = calculateBmi(it.weight, v))
        }
    }

    fun onGlucoseChange(v: String) { _uiState.update { it.copy(glucose = v, glucoseError = null) } }
    fun onSystolicChange(v: String) { _uiState.update { it.copy(systolic = v) } }
    fun onDiastolicChange(v: String) { _uiState.update { it.copy(diastolic = v) } }
    fun onInsulinChange(v: String) { _uiState.update { it.copy(insulin = v) } }

    fun analyzeHealth() {
        val state = _uiState.value

        // 1. Parsing Input
        val ageVal = state.age.toIntOrNull() ?: 0
        val weightVal = state.weight.replace(",", ".").toDoubleOrNull() ?: 0.0

        // Input: mmol/L & pmol/L (Desimal)
        val glucoseVal = state.glucose.replace(",", ".").toDoubleOrNull() ?: 0.0
        val insulinVal = state.insulin.replace(",", ".").toDoubleOrNull() ?: 0.0

        val systolicVal = state.systolic.toIntOrNull() ?: 0
        val diastolicVal = state.diastolic.toIntOrNull() ?: 0

        var hasError = false

        // --- VALIDASI LIMIT (Sesuai Standar Baru) ---

        // 1. Usia
        if (ageVal !in 5..100) {
            _uiState.update { it.copy(ageError = "Usia tidak valid (5-100)") }
            hasError = true
        }

        // 2. Berat
        if (weightVal !in 10.0..300.0) {
            _uiState.update { it.copy(weightError = "Berat tidak valid") }
            hasError = true
        }

        // 3. Gula Darah (mmol/L)
        // Range aman input: 2.0 - 40.0
        if (glucoseVal < 2.0 || glucoseVal > 40.0) {
            _uiState.update { it.copy(glucoseError = "Nilai tidak wajar (2-40 mmol/L)") }
            hasError = true
        }

        // 4. Tensi
        if (systolicVal !in 70..250) {
            ToastManager.showToast("Tensi Sistolik tidak valid (70-250)", ToastType.ERROR)
            hasError = true
        }
        if (diastolicVal !in 40..150) {
            ToastManager.showToast("Tensi Diastolik tidak valid (40-150)", ToastType.ERROR)
            hasError = true
        }
        if (systolicVal <= diastolicVal && systolicVal != 0) {
            ToastManager.showToast("Sistolik harus lebih besar dari Diastolik", ToastType.ERROR)
            hasError = true
        }

        // 5. Insulin (pmol/L)
        // Validasi: 0 - 4000
        if (state.insulin.isNotBlank() && (insulinVal < 0 || insulinVal > 4000)) {
            ToastManager.showToast("Nilai Insulin tidak wajar", ToastType.ERROR)
            hasError = true
        }

        if (hasError) return

        // --- KIRIM LANGSUNG TANPA KONVERSI ---

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            delay(1500)

            val result = repository.getPrediction(
                bmi = state.bmi,
                age = ageVal,
                glucose = glucoseVal, // Kirim mmol/L (Double)
                systolic = systolicVal,
                diastolic = diastolicVal,
                insulin = insulinVal  // Kirim pmol/L (Double)
            )

            result.fold(
                onSuccess = { response ->
                    _uiState.update { it.copy(isLoading = false, prediction = response) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Gagal analisis") }
                }
            )
        }
    }

    fun resetResult() {
        _uiState.update { it.copy(prediction = null) }
    }

    // Helper: Hitung Umur
    private fun calculateAge(dobString: String): Int {
        if (dobString == "N/A" || dobString.isBlank()) return 0
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val birthDate = sdf.parse(dobString) ?: return 0
            val today = Calendar.getInstance()
            val birthCal = Calendar.getInstance().apply { time = birthDate }
            var age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            age
        } catch (e: Exception) { 0 }
    }

    // Helper: Hitung BMI Real-time
    private fun calculateBmi(weightStr: String, heightStr: String): Double {
        val w = weightStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        val h = heightStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        return if (w > 0 && h > 0) {
            val hMeter = h / 100
            w / (hMeter * hMeter)
        } else 0.0
    }
}

class SmartMealPlanViewModelFactory(
    private val repository: MealPlanRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SmartMealPlanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SmartMealPlanViewModel(repository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}