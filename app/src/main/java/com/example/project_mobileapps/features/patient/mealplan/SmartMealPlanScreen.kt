// File: features/patient/mealplan/SmartMealPlanScreen.kt
package com.example.project_mobileapps.features.patient.mealplan

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.repo.AuthRepository
import com.example.project_mobileapps.di.AppContainer
import com.example.project_mobileapps.ui.components.ToastManager
import com.example.project_mobileapps.ui.components.ToastType
import com.example.project_mobileapps.ui.themes.PrimaryPeriwinkle
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropUp
import com.example.project_mobileapps.ui.components.MealRecommendationCard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.ui.text.style.TextAlign
import com.example.project_mobileapps.data.model.ml.MealPlanResponse
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartMealPlanScreen(
    onNavigateBack: () -> Unit
) {
    // Gunakan Factory untuk Inject Repository
    val viewModel: SmartMealPlanViewModel = viewModel(
        factory = SmartMealPlanViewModelFactory(AppContainer.mealPlanRepository, AuthRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    // Handler Error Toast
    LaunchedEffect(uiState.error) {
        uiState.error?.let { ToastManager.showToast(it, ToastType.ERROR) }
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            // [PERBAIKAN] Judul Dinamis & TopBar Selalu Muncul
            val titleText = if (uiState.prediction != null) "Hasil Analisis" else "AI Nutritionist"

            // Menggunakan CenterAlignedTopAppBar atau SmallTopAppBar agar konsisten
            SmallTopAppBar(
                title = {
                    Text(titleText, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Color(0xFF1E293B))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8F9FA), // Samakan dengan background agar seamless
                    scrolledContainerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            AnimatedContent(
                targetState = when {
                    uiState.isLoading -> "LOADING"
                    uiState.prediction != null -> "RESULT"
                    else -> "INPUT"
                },
                label = "ScreenTransition"
            ) { screenState ->
                when (screenState) {
                    "INPUT" -> MealPlanInputContent(uiState, viewModel)
                    "LOADING" -> LoadingContent()
                    "RESULT" -> ResultSection(
                        result = uiState.prediction!!,
                        userBmi = uiState.bmi,
                        onReset = { viewModel.resetResult() },
                        onBack = onNavigateBack
                    )
                }
            }
        }
    }
}

// --- RESULT SECTION (UI HASIL PREDIKSI) ---
// File: features/patient/mealplan/SmartMealPlanScreen.kt

// ... imports

@Composable
fun ResultSection(
    result: MealPlanResponse,
    userBmi: Double,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState() // Gunakan rememberLazyListState standar
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // 1. Tentukan Data Status
    val rawPrediction = result.prediction
    val (statusTitle, statusDesc, themeColor, icon, riskLevel) = when {
        rawPrediction.contains("Pre", ignoreCase = true) -> Quintuple(
            "Pre-Diabetes", "Gula darah di ambang batas. Waspada.",
            Color(0xFFFFA726), Icons.Default.Warning, 1
        )
        rawPrediction.contains("Non", ignoreCase = true) ||
                rawPrediction.contains("No", ignoreCase = true) ||
                rawPrediction.contains("Normal", ignoreCase = true) ||
                rawPrediction.contains("Negative", ignoreCase = true) -> Quintuple(
            "Sehat (Normal)", "Risiko rendah. Pertahankan!",
            Color(0xFF66BB6A), Icons.Default.CheckCircle, 0
        )
        else -> Quintuple(
            "Risiko Tinggi", "Indikasi diabetes. Konsultasi dokter.",
            Color(0xFFEF5350), Icons.Default.HealthAndSafety, 2
        )
    }

    // 2. Helper BMI Category
    val bmiCategory = when {
        userBmi < 18.5 -> "Underweight"
        userBmi < 24.9 -> "Normal Weight"
        userBmi < 29.9 -> "Overweight"
        else -> "Obesity"
    }

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
            .padding(bottom = 32.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- 2. MAIN RESULT CARD (REDESIGNED) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // A. HEADER ROW (Icon Kiri, Teks Kanan - Lebih Kompak)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Icon Container
                        Surface(
                            shape = CircleShape,
                            color = themeColor.copy(alpha = 0.1f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, null, tint = themeColor, modifier = Modifier.size(32.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Text Content
                        Column {
                            Text("Status Kesehatan", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Text(
                                text = statusTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = statusDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF475569),
                        lineHeight = 20.sp
                    )

                    Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF1F5F9))

                    // B. VISUAL GAUGE (Informatif)
                    Text("Tingkat Risiko", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    RiskGauge(currentLevel = riskLevel)

                    Spacer(modifier = Modifier.height(20.dp))

                    // C. STATS BOX (BMI)
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Indeks BMI", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(
                                    text = String.format("%.1f", userBmi),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF334155)
                                )
                            }
                            // Kategori Chip
                            Surface(
                                color = Color(0xFF334155),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = bmiCategory,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- 3. REKOMENDASI MENU ---
            Text(
                "Rencana Makan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Slider (Sama)
        LazyRow(
            state = listState,
            flingBehavior = snapBehavior,
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MealRecommendationCard(
                    mealTime = "Sarapan",
                    foodName = result.mealPlan.breakfast,
                    macros = result.nutrition?.breakfast,
                    color = Color(0xFFFFA726),
                    modifier = Modifier.width(300.dp),
                    onClick = { findRecipe(result.mealPlan.breakfast) }
                )
            }
            item {
                MealRecommendationCard(
                    mealTime = "Makan Siang",
                    foodName = result.mealPlan.lunch,
                    macros = result.nutrition?.lunch,
                    color = Color(0xFF42A5F5),
                    modifier = Modifier.width(300.dp),
                    onClick = { findRecipe(result.mealPlan.lunch) }
                )
            }
            item {
                MealRecommendationCard(
                    mealTime = "Makan Malam",
                    foodName = result.mealPlan.dinner,
                    macros = result.nutrition?.dinner,
                    color = Color(0xFF7E57C2),
                    modifier = Modifier.width(300.dp),
                    onClick = { findRecipe(result.mealPlan.dinner) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Footer Button
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Analisis Ulang", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// --- KOMPONEN GAUGE BARU (LEBIH RAPI) ---
@Composable
fun RiskGauge(currentLevel: Int) {
    val labels = listOf("Normal", "Pre-Diab", "Tinggi")
    // Warna untuk: Hijau (Aman), Orange (Waspada), Merah (Bahaya)
    val colors = listOf(Color(0xFF66BB6A), Color(0xFFFFA726), Color(0xFFEF5350))

    Column {
        // 1. The Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFFF1F5F9)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            colors.forEachIndexed { index, color ->
                val isActive = index == currentLevel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (isActive) color else color.copy(alpha = 0.2f))
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. The Labels (Aligned below segments)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween // Spread text
        ) {
            labels.forEachIndexed { index, label ->
                val isActive = index == currentLevel
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = if(isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if(isActive) colors[index] else Color.Gray,
                    textAlign = if(index==0) TextAlign.Start else if(index==2) TextAlign.End else TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun RiskLevelIndicator(currentLevel: Int) {
    // Level: 0=Sehat (Hijau), 1=Pre (Orange), 2=Tinggi (Merah)
    val labels = listOf("Sehat", "Waspada", "Bahaya")
    val colors = listOf(Color(0xFF66BB6A), Color(0xFFFFA726), Color(0xFFEF5350))

    Column(modifier = Modifier.fillMaxWidth()) {
        // Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFF1F5F9)), // Background abu track
            horizontalArrangement = Arrangement.spacedBy(2.dp) // Jarak antar segmen
        ) {
            colors.forEachIndexed { index, color ->
                // Jika level ini aktif, warnanya menyala. Jika tidak, warnanya pudar.
                val isActive = index == currentLevel
                val segmentColor = if (isActive) color else color.copy(alpha = 0.2f)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(segmentColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Label Text & Arrow
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                val isActive = index == currentLevel
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isActive) {
                        // Panah Penunjuk (Arrow Up)
                        Icon(
                            imageVector = Icons.Default.ArrowDropUp,
                            contentDescription = null,
                            tint = colors[index],
                            modifier = Modifier.size(24.dp).offset(y = (-4).dp)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors[index]
                        )
                    }
                }
            }
        }
    }
}

// Helper Class (Updated to 5 items)
data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
@Composable
fun MiniStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF334155))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
    }
}

@Composable
fun MealPlanInputContent(state: MealPlanUiState, viewModel: SmartMealPlanViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- 1. HERO BANNER ---
        HeroBannerCard()

        // --- 2. DATA FISIK ---
        Text("Data Fisik", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Baris 1: Usia (Auto-filled)
                HealthInputRow(
                    value = state.age,
                    onValueChange = viewModel::onAgeChange,
                    label = "Usia",
                    unit = "Thn",
                    icon = Icons.Default.Info,
                    error = state.ageError,
                    placeholder = "Contoh: 25"
                )
                Divider(color = Color.LightGray.copy(alpha = 0.2f))

                // Baris 2: Berat & Tinggi
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HealthInputRow(
                        value = state.weight,
                        onValueChange = viewModel::onWeightChange,
                        label = "Berat",
                        unit = "kg",
                        icon = Icons.Default.MonitorWeight,
                        modifier = Modifier.weight(1f),
                        error = state.weightError
                    )
                    HealthInputRow(
                        value = state.height,
                        onValueChange = viewModel::onHeightChange,
                        label = "Tinggi",
                        unit = "cm",
                        icon = Icons.Outlined.Analytics,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Baris 3: BMI Display (Real-time)
                if (state.bmi > 0) {
                    BmiIndicatorCompact(state.bmi)
                }
            }
        }

        // --- 3. DATA LABORATORIUM ---
        Text("Data Laboratorium", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // [PERBAIKAN 1]: Satuan Gula Darah jadi mmol/L
                HealthInputRow(
                    value = state.glucose,
                    onValueChange = viewModel::onGlucoseChange,
                    label = "Gula Darah (FGB)",
                    unit = "mmol/L",
                    icon = Icons.Default.WaterDrop,
                    error = state.glucoseError,
                    placeholder = "Normal: 4.0 - 5.9"
                )

                // [PERBAIKAN 2]: Insulin TIDAK Opsional & Satuan pmol/L
                HealthInputRow(
                    value = state.insulin,
                    onValueChange = viewModel::onInsulinChange,
                    label = "Insulin", // <-- KATA (Opsional) DIHAPUS
                    unit = "pmol/L",         // <-- SUDAH DIPERBAIKI
                    placeholder = "Normal: 18 - 173"
                )

                // Tensi
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HealthInputRow(
                        value = state.systolic, onValueChange = viewModel::onSystolicChange,
                        label = "Systolic", unit = "mmHg", modifier = Modifier.weight(1f),
                        placeholder = "120"
                    )
                    HealthInputRow(
                        value = state.diastolic, onValueChange = viewModel::onDiastolicChange,
                        label = "Diastolic", unit = "mmHg", modifier = Modifier.weight(1f),
                        placeholder = "80"
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- TOMBOL ANALISIS ---
        Button(
            onClick = viewModel::analyzeHealth,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPeriwinkle)
        ) {
            Text("Analisis Kesehatan Saya", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(32.dp))
    }
}

// --- KOMPONEN INPUT BARU (PROFESSIONAL) ---
@Composable
fun HealthInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier,
    error: String? = null,
    placeholder: String = ""
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) onValueChange(it) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text(placeholder, color = Color.LightGray, fontSize = 12.sp) },
            suffix = {
                Text(unit, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = PrimaryPeriwinkle)
            },
            isError = error != null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPeriwinkle,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = Color(0xFFF9FAFB),
                unfocusedContainerColor = Color(0xFFF9FAFB)
            )
        )
        if (error != null) {
            Text(error, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

// --- HERO BANNER (ILUSTRASI MENGGUNAKAN ICON) ---
@Composable
fun HeroBannerCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(PrimaryPeriwinkle, Color(0xFF8C9EFF))
                )
            )
    ) {
        // Dekorasi Lingkaran
        Box(modifier = Modifier.size(100.dp).align(Alignment.TopEnd).offset(x = 20.dp, y = (-20).dp).background(Color.White.copy(alpha = 0.1f), CircleShape))
        Box(modifier = Modifier.size(60.dp).align(Alignment.BottomStart).offset(x = (-10).dp, y = 10.dp).background(Color.White.copy(alpha = 0.1f), CircleShape))

        Row(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Cek Risiko Diabetes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("Dapatkan rencana makan personal berbasis AI.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            }
            // Icon Gede
            Icon(
                imageVector = Icons.Outlined.Analytics,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

@Composable
fun BmiIndicatorCompact(bmi: Double) {
    val (color, text) = when {
        bmi < 18.5 -> Color(0xFFFFB74D) to "Underweight"
        bmi < 24.9 -> Color(0xFF66BB6A) to "Normal"
        bmi < 29.9 -> Color(0xFFFF7043) to "Overweight"
        else -> Color(0xFFEF5350) to "Obesity"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("BMI Anda: ${String.format("%.1f", bmi)}", fontWeight = FontWeight.Bold, color = color)
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = PrimaryPeriwinkle,
            strokeWidth = 6.dp,
            modifier = Modifier.size(60.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text("Sedang Menganalisis...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("AI sedang menghitung nutrisi terbaik untuk Anda.", color = Color.Gray, fontSize = 14.sp)
    }
}