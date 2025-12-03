// File: features/patient/mealplan/SmartMealPlanScreen.kt
package com.example.project_mobileapps.features.patient.mealplan

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.data.model.ml.MealPlanResponse
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.ui.components.MealRecommendationCard
import com.example.project_mobileapps.ui.components.ToastManager
import com.example.project_mobileapps.ui.components.ToastType
import com.example.project_mobileapps.ui.themes.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartMealPlanScreen(
    onNavigateBack: () -> Unit
) {
    // --- STATE VARIABLES ---
    var inputAge by remember { mutableStateOf("") }
    var inputBmi by remember { mutableStateOf("") }
    var inputGlucose by remember { mutableStateOf("") }
    var inputSystolic by remember { mutableStateOf("") }
    var inputDiastolic by remember { mutableStateOf("") }
    var inputInsulin by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var predictionResult by remember { mutableStateOf<MealPlanResponse?>(null) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Meal Plan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            AnimatedContent(
                targetState = when {
                    isLoading -> "LOADING"
                    predictionResult != null -> "RESULT"
                    else -> "INPUT"
                },
                label = "MealPlanState"
            ) { state ->
                when (state) {
                    "INPUT" -> InputSection(
                        age = inputAge, onAgeChange = { inputAge = it },
                        bmi = inputBmi, onBmiChange = { inputBmi = it },
                        glucose = inputGlucose, onGlucoseChange = { inputGlucose = it },
                        systolic = inputSystolic, onSystolicChange = { inputSystolic = it },
                        diastolic = inputDiastolic, onDiastolicChange = { inputDiastolic = it },
                        insulin = inputInsulin, onInsulinChange = { inputInsulin = it },
                        onAnalyzeClick = {
                            scope.launch {
                                isLoading = true
                                delay(800) // Simulasi loading agar smooth
                                val result = AppContainer.mealPlanRepository.getPrediction(
                                    bmi = inputBmi.toDoubleOrNull() ?: 0.0,
                                    age = inputAge.toIntOrNull() ?: 0,
                                    glucose = inputGlucose.toIntOrNull() ?: 0,
                                    systolic = inputSystolic.toIntOrNull() ?: 0,
                                    diastolic = inputDiastolic.toIntOrNull() ?: 0,
                                    insulin = inputInsulin.toIntOrNull() ?: 0
                                )
                                isLoading = false
                                result.onSuccess {
                                    predictionResult = it
                                }.onFailure {
                                    ToastManager.showToast("Gagal: ${it.message}", ToastType.ERROR)
                                }
                            }
                        }
                    )

                    "LOADING" -> LoadingSection()

                    "RESULT" -> ResultSection(
                        result = predictionResult!!,
                        onReset = { predictionResult = null }
                    )
                }
            }
        }
    }
}

// --- SUB-COMPONENTS UNTUK KERAPIHAN ---

@Composable
fun InputSection(
    age: String, onAgeChange: (String) -> Unit,
    bmi: String, onBmiChange: (String) -> Unit,
    glucose: String, onGlucoseChange: (String) -> Unit,
    systolic: String, onSystolicChange: (String) -> Unit,
    diastolic: String, onDiastolicChange: (String) -> Unit,
    insulin: String, onInsulinChange: (String) -> Unit,
    onAnalyzeClick: () -> Unit
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Isi data kesehatan Anda untuk mendapatkan prediksi risiko diabetes dan rekomendasi menu makan yang dipersonalisasi oleh AI.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Data Fisik", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SimpleOutlinedTextField(value = age, onValueChange = onAgeChange, label = "Usia (Tahun)", modifier = Modifier.weight(1f))
                    SimpleOutlinedTextField(value = bmi, onValueChange = onBmiChange, label = "BMI", modifier = Modifier.weight(1f), isDecimal = true)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Data Lab", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                SimpleOutlinedTextField(value = glucose, onValueChange = onGlucoseChange, label = "Gula Darah Puasa (FGB)")

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SimpleOutlinedTextField(value = systolic, onValueChange = onSystolicChange, label = "Tekanan Atas (Sys)", modifier = Modifier.weight(1f))
                    SimpleOutlinedTextField(value = diastolic, onValueChange = onDiastolicChange, label = "Tekanan Bawah (Dia)", modifier = Modifier.weight(1f))
                }

                SimpleOutlinedTextField(value = insulin, onValueChange = onInsulinChange, label = "Level Insulin")
            }
        }

        Button(
            onClick = onAnalyzeClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = age.isNotEmpty() && glucose.isNotEmpty() // Validasi sederhana
        ) {
            Text("Mulai Analisis AI")
        }
    }
}

@Composable
fun LoadingSection() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(60.dp), strokeWidth = 6.dp)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Sedang Menganalisis...", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("AI sedang menghitung menu terbaik...", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
fun ResultSection(result: MealPlanResponse, onReset: () -> Unit) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Status Kesehatan
        val isRisk = result.prediction.contains("Diabetes", ignoreCase = true)
        val statusColor = if (isRisk) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)

        Card(
            colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isRisk) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                    contentDescription = null, tint = statusColor, modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Hasil Prediksi AI", style = MaterialTheme.typography.labelMedium, color = statusColor)
                    Text(result.prediction, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }
        }

        Text("Rekomendasi Menu Harian", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // 2. Kartu Makanan
        MealRecommendationCard(
            mealTime = "Sarapan",
            foodName = result.mealPlan.breakfast,
            macros = result.nutrition?.breakfast,
            color = Color(0xFFFFA726)
        )
        MealRecommendationCard(
            mealTime = "Makan Siang",
            foodName = result.mealPlan.lunch,
            macros = result.nutrition?.lunch,
            color = MaterialTheme.colorScheme.primary
        )
        MealRecommendationCard(
            mealTime = "Makan Malam",
            foodName = result.mealPlan.dinner,
            macros = result.nutrition?.dinner,
            color = Color(0xFF7E57C2)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Hitung Ulang")
        }
    }
}

@Composable
fun SimpleOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isDecimal: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = {
            // Validasi input hanya angka (dan titik jika decimal)
            if (it.all { c -> c.isDigit() || (isDecimal && c == '.') }) onValueChange(it)
        },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}