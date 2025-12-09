// File: features/patient/mealplan/SmartMealPlanScreen.kt
package com.example.project_mobileapps.features.patient.mealplan

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

@Composable
fun SmartMealPlanScreen(
    onNavigateBack: () -> Unit
) {
    // State Variables
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

    // Fungsi Helper Parsing
    fun parseInput(value: String): Double {
        return value.replace(",", ".").toDoubleOrNull() ?: 0.0
    }

    // Auto BMI Calculation
    val calculatedBmi = remember(inputWeight, inputHeight) {
        val w = parseInput(inputWeight)
        val h = parseInput(inputHeight)
        if (w > 0 && h > 0) {
            w / ((h / 100) * (h / 100))
        } else 0.0
    }

    Scaffold(
        containerColor = Color(0xFFF5F7FA), // Warna background soft gray
        topBar = {
            if (predictionResult == null) {
                HeroHeaderSection(onBackClick = onNavigateBack)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
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

                                // Validasi Dasar
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
                                onReset = { predictionResult = null },
                                onBack = onNavigateBack
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- HEADER SECTION ---
@Composable
fun HeroHeaderSection(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Icon(
            imageVector = Icons.Outlined.LocalHospital,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.1f),
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 30.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Smart Nutrition AI",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// --- INPUT SECTION (UPDATED) ---
@Composable
fun InputSection(
    age: String, onAgeChange: (String) -> Unit,
    weight: String, onWeightChange: (String) -> Unit,
    height: String, onHeightChange: (String) -> Unit,
    bmiValue: Double,
    glucose: String, onGlucoseChange: (String) -> Unit,
    systolic: String, onSystolicChange: (String) -> Unit,
    diastolic: String, onDiastolicChange: (String) -> Unit,
    insulin: String, onInsulinChange: (String) -> Unit,
    onAnalyzeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- 1. DATA FISIK ---
        SectionHeader("Data Fisik", Icons.Outlined.MonitorWeight)

        // [PERUBAHAN] Menghapus Card, hanya menggunakan Column agar transparan
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Usia
            MedicalTextField(
                value = age,
                onValueChange = onAgeChange,
                label = "Usia",
                suffix = "Tahun",
                isIntegerOnly = true
            )

            // [PERUBAHAN] Berat dan Tinggi sekarang ke bawah (Column) bukan Row
            MedicalTextField(
                value = weight,
                onValueChange = onWeightChange,
                label = "Berat Badan",
                suffix = "kg"
            )

            MedicalTextField(
                value = height,
                onValueChange = onHeightChange,
                label = "Tinggi Badan",
                suffix = "cm"
            )

            // Tampilan BMI
            if (bmiValue > 0) {
                // Memberikan sedikit background pada BMI agar tetap terbaca jelas
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Indeks Massa Tubuh (BMI)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(String.format(Locale.US, "%.1f", bmiValue), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        val (status, color) = when {
                            bmiValue < 18.5 -> "Underweight" to Color(0xFFFFB74D)
                            bmiValue < 24.9 -> "Normal" to Color(0xFF66BB6A)
                            bmiValue < 29.9 -> "Overweight" to Color(0xFFFF7043)
                            else -> "Obesitas" to Color(0xFFEF5350)
                        }
                        Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = status,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- 2. DATA LABORATORIUM ---
        SectionHeader("Data Laboratorium", Icons.Filled.Info)

        // [PERUBAHAN] Menghapus Card, langsung Column
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            MedicalTextField(
                value = glucose,
                onValueChange = onGlucoseChange,
                label = "Gula Darah (Puasa)",
                suffix = "mg/dL"
            )

            // [PERUBAHAN] Tensi tetap Row agar rapi, tapi spacing diatur
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MedicalTextField(
                    value = systolic,
                    onValueChange = onSystolicChange,
                    label = "Systolic", // Tensi Atas
                    suffix = "mmHg",
                    modifier = Modifier.weight(1f),
                    isIntegerOnly = true
                )
                MedicalTextField(
                    value = diastolic,
                    onValueChange = onDiastolicChange,
                    label = "Diastolic", // Tensi Bawah
                    suffix = "mmHg",
                    modifier = Modifier.weight(1f),
                    isIntegerOnly = true
                )
            }

            MedicalTextField(
                value = insulin,
                onValueChange = onInsulinChange,
                label = "Insulin",
                suffix = "µU/ml"
            )
        }

        Spacer(Modifier.height(8.dp))

        // Tombol Submit
        Button(
            onClick = onAnalyzeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = ButtonDefaults.buttonElevation(4.dp)
        ) {
            Text("Analisis Sekarang", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(32.dp))
    }
}

// --- CUSTOM TEXT FIELD ---
@Composable
fun MedicalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String = "",
    modifier: Modifier = Modifier,
    isIntegerOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (isIntegerOnly) {
                if (input.all { it.isDigit() }) onValueChange(input)
            } else {
                if (input.all { it.isDigit() || it == '.' || it == ',' }) onValueChange(input)
            }
        },
        label = { Text(label) },
        suffix = {
            if (suffix.isNotEmpty()) {
                Text(suffix, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isIntegerOnly) KeyboardType.Number else KeyboardType.Decimal
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color(0xFFE0E0E0),
            // Background field tetap putih agar kontras dengan layar yang agak abu
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

// --- RESULT SECTION ---
@Composable
fun ResultSection(
    result: MealPlanResponse,
    userBmi: Double,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

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
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp) // Padding bawah agar scroll tidak mentok
    ) {
        // Header Back Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, bottom = 16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Hasil Analisis",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                )
            )

            // --- INFO STATUS KESEHATAN (Tetap sama) ---
            val rawPrediction = result.prediction
            val (statusTitle, statusDesc, themeColor, icon) = when {
                rawPrediction.contains("Pre", ignoreCase = true) -> Quadruple(
                    "Pre-Diabetes", "Kadar gula darah Anda di atas normal. Perlu waspada.", Color(0xFFFFA726), Icons.Default.Warning
                )
                rawPrediction.contains("Non", ignoreCase = true) || rawPrediction.contains("No", ignoreCase = true) || rawPrediction.contains("Normal", ignoreCase = true) || rawPrediction.contains("Negative", ignoreCase = true) -> Quadruple(
                    "Normal (Sehat)", "Risiko rendah. Pertahankan gaya hidup sehat!", Color(0xFF66BB6A), Icons.Default.CheckCircle
                )
                else -> Quadruple(
                    "Risiko Tinggi", "Indikasi kuat diabetes. Segera konsultasikan ke dokter.", Color(0xFFEF5350), Icons.Default.HealthAndSafety
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, themeColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(themeColor.copy(alpha = 0.1f))
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = themeColor, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = "Status Kesehatan", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Text(text = statusTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E))
                        }
                    }
                    Divider(color = themeColor.copy(alpha = 0.2f))
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = statusDesc, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF424242), lineHeight = 20.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Rekomendasi Menu",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- SLIDER REKOMENDASI (DENGAN SNAPPING & UKURAN FIX) ---
        LazyRow(
            state = listState,
            flingBehavior = snapBehavior, // EFEK MAGNET (SNAPPING)
            contentPadding = PaddingValues(horizontal = 24.dp), // Padding kiri-kanan agar kartu di tengah
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MealRecommendationCard(
                    mealTime = "Sarapan",
                    foodName = result.mealPlan.breakfast,
                    macros = result.nutrition?.breakfast,
                    color = Color(0xFFFFA726),
                    // Menggunakan lebar fix 85% layar agar kartu terlihat konsisten
                    modifier = Modifier.fillParentMaxWidth(0.85f),
                    onClick = { findRecipe(result.mealPlan.breakfast) }
                )
            }
            item {
                MealRecommendationCard(
                    mealTime = "Makan Siang",
                    foodName = result.mealPlan.lunch,
                    macros = result.nutrition?.lunch,
                    color = Color(0xFF42A5F5),
                    modifier = Modifier.fillParentMaxWidth(0.85f),
                    onClick = { findRecipe(result.mealPlan.lunch) }
                )
            }
            item {
                MealRecommendationCard(
                    mealTime = "Makan Malam",
                    foodName = result.mealPlan.dinner,
                    macros = result.nutrition?.dinner,
                    color = Color(0xFF7E57C2),
                    modifier = Modifier.fillParentMaxWidth(0.85f),
                    onClick = { findRecipe(result.mealPlan.dinner) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tombol Hitung Ulang
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text("Hitung Ulang", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// --- HELPER LAINNYA ---
@Composable
fun LoadingSection() {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 6.dp)
        Spacer(Modifier.height(24.dp))
        Text("AI sedang menganalisis...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)