// File: features/patient/mealplan/SmartMealPlanScreen.kt
package com.example.project_mobileapps.features.patient.mealplan

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_mobileapps.data.model.ml.MealPlanResponse
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.ui.components.MealRecommendationCard
import com.example.project_mobileapps.ui.components.ToastManager
import com.example.project_mobileapps.ui.components.ToastType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartMealPlanScreen(
    onNavigateBack: () -> Unit
) {
    var inputAge by remember { mutableStateOf("") }
    var inputWeight by remember { mutableStateOf("") }
    var inputHeight by remember { mutableStateOf("") }
    var inputGlucose by remember { mutableStateOf("") }
    var inputSystolic by remember { mutableStateOf("") }
    var inputDiastolic by remember { mutableStateOf("") }
    var inputInsulin by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var predictionResult by remember { mutableStateOf<MealPlanResponse?>(null) }

    val scope = rememberCoroutineScope()

    fun parseInput(value: String): Double {
        return value.replace(",", ".").toDoubleOrNull() ?: 0.0
    }

    val calculatedBmi = remember(inputWeight, inputHeight) {
        val w = parseInput(inputWeight)
        val h = parseInput(inputHeight)
        if (w > 0 && h > 0) {
            w / ((h / 100) * (h / 100))
        } else 0.0
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            HeroHeaderSection(onBackClick = onNavigateBack)

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
                        weight = inputWeight, onWeightChange = { inputWeight = it },
                        height = inputHeight, onHeightChange = { inputHeight = it },
                        bmiValue = calculatedBmi,
                        glucose = inputGlucose, onGlucoseChange = { inputGlucose = it },
                        systolic = inputSystolic, onSystolicChange = { inputSystolic = it },
                        diastolic = inputDiastolic, onDiastolicChange = { inputDiastolic = it },
                        insulin = inputInsulin, onInsulinChange = { inputInsulin = it },
                        onAnalyzeClick = {
                            scope.launch {
                                val ageVal = inputAge.toIntOrNull() ?: 0
                                val weightVal = parseInput(inputWeight)
                                val heightVal = parseInput(inputHeight)
                                val glucoseVal = parseInput(inputGlucose)

                                if (ageVal == 0 || weightVal == 0.0 || heightVal == 0.0 || glucoseVal == 0.0) {
                                    ToastManager.showToast("Mohon lengkapi data fisik & gula darah", ToastType.ERROR)
                                    return@launch
                                }
                                if (ageVal !in 5..120) {
                                    ToastManager.showToast("Usia tidak valid", ToastType.ERROR)
                                    return@launch
                                }

                                isLoading = true
                                delay(1500)
                                val result = AppContainer.mealPlanRepository.getPrediction(
                                    bmi = calculatedBmi,
                                    age = ageVal,
                                    glucose = glucoseVal.toInt(),
                                    systolic = parseInput(inputSystolic).toInt(),
                                    diastolic = parseInput(inputDiastolic).toInt(),
                                    insulin = parseInput(inputInsulin).toInt()
                                )
                                isLoading = false
                                result.onSuccess { predictionResult = it }
                                    .onFailure { ToastManager.showToast("Gagal: ${it.message}", ToastType.ERROR) }
                            }
                        }
                    )

                    "LOADING" -> LoadingSection()

                    "RESULT" -> {
                        predictionResult?.let { result ->
                            ResultSection(
                                result = result,
                                userBmi = calculatedBmi,
                                onReset = { predictionResult = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- RESULT SECTION (DENGAN FITUR CARI RESEP) ---
@Composable
fun ResultSection(result: MealPlanResponse, userBmi: Double, onReset: () -> Unit) {
    val context = LocalContext.current

    fun findRecipe(foodName: String?) {
        if (foodName.isNullOrEmpty()) return
        try {
            val query = "Resep cara masak $foodName enak sehat"
            val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val uri = Uri.parse("https://www.google.com/search?q=$encodedQuery")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            ToastManager.showToast("Tidak dapat membuka browser", ToastType.ERROR)
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp) // Jarak antar elemen lebih lega
    ) {
        // --- 1. LOGIKA 3 KONDISI (Normal, Pre, Diabetes) ---
        val rawPrediction = result.prediction

        // Tentukan Status berdasarkan string dari API
        val (statusTitle, statusDesc, themeColor, icon) = when {
            // Kondisi 1: Pre-Diabetes (Waspada)
            rawPrediction.contains("Pre", ignoreCase = true) -> {
                Quadruple(
                    "Pre-Diabetes",
                    "Kadar gula darah Anda di atas normal. Perlu waspada dan jaga pola makan.",
                    Color(0xFFFFA726), // Orange
                    Icons.Default.Warning
                )
            }
            // Kondisi 2: Normal / Non-Diabetes
            rawPrediction.contains("Non", ignoreCase = true) ||
                    rawPrediction.contains("No", ignoreCase = true) ||
                    rawPrediction.contains("Normal", ignoreCase = true) ||
                    rawPrediction.contains("Negative", ignoreCase = true) -> {
                Quadruple(
                    "Normal (Sehat)",
                    "Hasil analisis menunjukkan risiko rendah. Pertahankan gaya hidup sehat!",
                    Color(0xFF66BB6A), // Green
                    Icons.Default.CheckCircle
                )
            }
            // Kondisi 3: Diabetes (Bahaya) - Default jika mengandung kata "Diabetes" tanpa "Non"
            else -> {
                Quadruple(
                    "Risiko Tinggi (Diabetes)",
                    "Hasil menunjukkan indikasi kuat. Segera konsultasikan dengan dokter.",
                    Color(0xFFEF5350), // Red
                    Icons.Default.HealthAndSafety
                )
            }
        }

        // --- 2. KARTU HASIL (MODERN MINIMALIST DESIGN) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, themeColor.copy(alpha = 0.3f)), // Border tipis elegan
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Header Kartu (Warna Background Tipis)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeColor.copy(alpha = 0.1f)) // Soft tint background
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Status Kesehatan",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E) // Hitam pekat
                        )
                    }
                }

                Divider(color = themeColor.copy(alpha = 0.2f))

                // Body Kartu (Detail & BMI)
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = statusDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF424242),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // BMI Section yang Minimalis
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("BMI Anda", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                text = String.format(Locale.US, "%.1f", userBmi),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                        }
                        // Status BMI Badge
                        val (bmiLabel, bmiColor) = getBmiLabelColor(userBmi)
                        Surface(
                            color = bmiColor.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = bmiLabel,
                                color = bmiColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 3. REKOMENDASI MAKANAN ---
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Rekomendasi Menu Harian",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E)
            )
            Text(
                "Pilih menu di bawah untuk melihat resep lengkapnya.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            MealRecommendationCard(
                mealTime = "Sarapan",
                foodName = result.mealPlan.breakfast,
                macros = result.nutrition?.breakfast,
                color = Color(0xFFFFA726), // Warm Orange
                onClick = { findRecipe(result.mealPlan.breakfast) }
            )
            MealRecommendationCard(
                mealTime = "Makan Siang",
                foodName = result.mealPlan.lunch,
                macros = result.nutrition?.lunch,
                color = Color(0xFF42A5F5), // Cool Blue
                onClick = { findRecipe(result.mealPlan.lunch) }
            )
            MealRecommendationCard(
                mealTime = "Makan Malam",
                foodName = result.mealPlan.dinner,
                macros = result.nutrition?.dinner,
                color = Color(0xFF7E57C2), // Calm Purple
                onClick = { findRecipe(result.mealPlan.dinner) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tombol Hitung Ulang (Outline Style agar tidak terlalu dominan)
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Text("Hitung Ulang Analisis", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(32.dp))
    }
}

// Helper Class Sederhana untuk menampung 4 nilai
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// Helper untuk BMI Label
fun getBmiLabelColor(bmi: Double): Pair<String, Color> {
    return when {
        bmi < 18.5 -> "Underweight" to Color(0xFFFFB74D)
        bmi < 24.9 -> "Normal" to Color(0xFF66BB6A)
        bmi < 29.9 -> "Overweight" to Color(0xFFFF7043)
        else -> "Obesity" to Color(0xFFEF5350)
    }
}
// --- HELPER LAINNYA (Sama seperti sebelumnya) ---

@Composable
fun HeroHeaderSection(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(180.dp)
            .background(brush = Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))))
    ) {
        Icon(Icons.Outlined.Restaurant, null, tint = Color.White.copy(alpha = 0.15f), modifier = Modifier.size(160.dp).align(Alignment.BottomEnd).offset(x = 40.dp, y = 20.dp))
        IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.TopStart).padding(top = 40.dp, start = 16.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Color.White) }
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color.White, shape = CircleShape, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.HealthAndSafety, "Logo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(6.dp)) }
                Spacer(Modifier.width(12.dp))
                Text("Smart Nutrition AI", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text("Rencanakan Makan Sehat", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun InputSection(age: String, onAgeChange: (String) -> Unit, weight: String, onWeightChange: (String) -> Unit, height: String, onHeightChange: (String) -> Unit, bmiValue: Double, glucose: String, onGlucoseChange: (String) -> Unit, systolic: String, onSystolicChange: (String) -> Unit, diastolic: String, onDiastolicChange: (String) -> Unit, insulin: String, onInsulinChange: (String) -> Unit, onAnalyzeClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SectionHeader("Data Fisik", Icons.Outlined.MonitorWeight)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MedicalTextField(value = age, onValueChange = onAgeChange, label = "Usia", suffix = "Tahun")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MedicalTextField(value = weight, onValueChange = onWeightChange, label = "Berat", suffix = "kg", modifier = Modifier.weight(1f))
                    MedicalTextField(value = height, onValueChange = onHeightChange, label = "Tinggi", suffix = "cm", modifier = Modifier.weight(1f))
                }
                if (bmiValue > 0) {
                    Divider(color = Color(0xFFF0F0F0))
                    val (status, color) = when { bmiValue < 18.5 -> "Kurus" to Color(0xFFFFB74D); bmiValue < 24.9 -> "Normal" to Color(0xFF66BB6A); bmiValue < 29.9 -> "Gemuk" to Color(0xFFFF7043); else -> "Obesitas" to Color(0xFFEF5350) }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text("BMI Anda", style = MaterialTheme.typography.labelSmall, color = Color.Gray); Text(String.format(Locale.US, "%.1f", bmiValue), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                        Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) { Text(status, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = color, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        SectionHeader("Data Laboratorium", Icons.Filled.Info)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MedicalTextField(value = glucose, onValueChange = onGlucoseChange, label = "Gula Darah (Puasa)", suffix = "mg/dL")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MedicalTextField(value = systolic, onValueChange = onSystolicChange, label = "Tensi Atas", suffix = "mmHg", modifier = Modifier.weight(1f))
                    MedicalTextField(value = diastolic, onValueChange = onDiastolicChange, label = "Tensi Bawah", suffix = "mmHg", modifier = Modifier.weight(1f))
                }
                MedicalTextField(value = insulin, onValueChange = onInsulinChange, label = "Insulin", suffix = "µU/ml")
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAnalyzeClick, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), elevation = ButtonDefaults.buttonElevation(4.dp)) { Text("Analisis Sekarang", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun LoadingSection() { Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { CircularProgressIndicator(modifier = Modifier.size(64.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 6.dp); Spacer(Modifier.height(24.dp)); Text("AI sedang menganalisis...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) } }

@Composable
fun SectionHeader(title: String, icon: ImageVector) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black) } }

@Composable
fun MedicalTextField(value: String, onValueChange: (String) -> Unit, label: String, suffix: String = "", modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' || c == ',' }) onValueChange(it) }, label = { Text(label) }, suffix = { if (suffix.isNotEmpty()) Text(suffix, style = MaterialTheme.typography.bodySmall, color = Color.Gray) }, modifier = modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color(0xFFE0E0E0), focusedContainerColor = Color.White, unfocusedContainerColor = Color(0xFFF9F9F9)))
}