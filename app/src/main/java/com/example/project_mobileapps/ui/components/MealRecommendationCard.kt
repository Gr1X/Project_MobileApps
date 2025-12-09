// File: ui/components/MealRecommendationCard.kt
package com.example.project_mobileapps.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// --- DEFINISI WARNA TEMA (HARDCODED AGAR MUDAH DICOPY) ---
val ThemePrimary = Color(0xFF6D80E3) // PrimaryPeriwinkle
val ThemePurpleDark = Color(0xFF4F5EAA) // Versi lebih gelap
val ThemePurpleLight = Color(0xFFA6B1F0) // Versi lebih terang
val ThemeLavender = Color(0xFF9FA8DA) // Lavender lembut
val ThemeSoftViolet = Color(0xFFB39DDB) // Ungu lembut
val ThemeSlate = Color(0xFF7986CB) // Biru keabuan

@Composable
fun MealRecommendationCard(
    mealTime: String,
    foodName: String?,
    macros: Any?,
    color: Color, // Warna badge (Sarapan/Siang/Malam) tetap ikut parameter agar beda dikit
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Parsing data dengan palet warna "1 Aura"
    val (calorieData, nutritionList) = remember(macros) {
        parseNutritionDataMonochrome(macros)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .heightIn(max = 500.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HEADER COMPACT ---
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestaurantMenu,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = mealTime.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = color
                    )
                }
            }

            // Nama Makanan
            Text(
                text = foodName ?: "Menu Rekomendasi",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                color = Color(0xFF1C1C1E),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .wrapContentHeight(Alignment.CenterVertically)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- CHART (WARNA "SATU AURA") ---
            val chartItems = nutritionList.filter {
                it.key in listOf("carbs", "protein", "fat", "sugar", "fiber")
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                MacroDonutChart(
                    macros = chartItems,
                    modifier = Modifier.size(120.dp),
                    strokeWidth = 12.dp
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = calorieData.value,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = ThemePrimary // Angka Kalori ikut warna tema
                    )
                    Text(
                        text = "Kcal",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- GRID NUTRISI ---
            if (nutritionList.isNotEmpty()) {
                NutritionGridCompact(nutritionList)
            } else {
                Text("-", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- TOMBOL LIHAT RESEP ---
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemePrimary, // Tombol pakai warna Utama (Periwinkle)
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Lihat Resep",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// --- GRID LAYOUT COMPACT ---
@Composable
fun NutritionGridCompact(items: List<NutritionItem>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val chunkedItems = items.chunked(3)
        chunkedItems.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        NutritionCardItemCompact(item)
                    }
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun NutritionCardItemCompact(item: NutritionItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 2.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dot Warna (Sesuai Aura)
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(item.color)
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.value,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                ),
                color = Color.Black
            )

            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color.Gray,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- CHART COMPONENT ---
@Composable
fun MacroDonutChart(
    macros: List<NutritionItem>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp
) {
    Canvas(modifier = modifier) {
        val totalValue = macros.sumOf { it.rawValue.toDouble() }.toFloat()
        var currentStartAngle = -90f
        val size = Size(size.width, size.height)

        // Background Lingkaran (Abu sangat muda)
        drawArc(
            color = Color(0xFFF0F0F5),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
            size = size
        )

        macros.forEach { item ->
            val sweepAngle = if (totalValue > 0) {
                (item.rawValue / totalValue) * 360f
            } else 0f

            drawArc(
                color = item.color,
                startAngle = currentStartAngle,
                sweepAngle = sweepAngle - 3f, // Gap kecil
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
                size = size
            )
            currentStartAngle += sweepAngle
        }
    }
}

data class NutritionItem(val key: String, val label: String, val value: String, val unit: String, val rawValue: Float, val color: Color)

// --- PARSING DATA (DENGAN WARNA MONOCHROME PURPLE) ---
fun parseNutritionDataMonochrome(macros: Any?): Pair<NutritionItem, List<NutritionItem>> {
    var calorieItem = NutritionItem("energy", "Kalori", "0", "kcal", 0f, Color.Black)
    val list = mutableListOf<NutritionItem>()
    if (macros == null) return Pair(calorieItem, list)

    val rawString = macros.toString()
    val regex = Regex("([a-zA-Z0-9_]+)\\s*[:=]\\s*([0-9]+\\.?[0-9]*)")
    val matches = regex.findAll(rawString)

    for (match in matches) {
        val key = match.groupValues[1].lowercase().trim()
        val valueStr = match.groupValues[2].trim()
        val numVal = valueStr.toFloatOrNull() ?: 0f
        val formattedVal = if (numVal % 1.0 == 0.0) numVal.toInt().toString() else String.format(Locale.US, "%.1f", numVal)

        // --- PEMBAGIAN WARNA SATU AURA (GRADASI UNGU) ---
        when {
            // Kalori
            key.contains("energy") || key.contains("kcal") || key.contains("cal") -> {
                if (!key.contains("calc")) calorieItem = NutritionItem("energy", "Kalori", formattedVal, "kcal", numVal, Color.Black)
            }
            // Protein (Warna Utama/Tergelap)
            key.contains("prot") ->
                list.add(NutritionItem("protein", "Protein", formattedVal, "g", numVal, ThemePrimary))

            // Karbo (Warna Lavender)
            key.contains("carb") || key.contains("karbo") ->
                list.add(NutritionItem("carbs", "Karbo", formattedVal, "g", numVal, ThemeLavender))

            // Lemak (Warna Ungu Gelap)
            key.contains("fat") || key.contains("lemak") ->
                list.add(NutritionItem("fat", "Lemak", formattedVal, "g", numVal, ThemePurpleDark))

            // Fiber (Warna Biru Keabuan/Slate)
            key.contains("fib") || key.contains("serat") ->
                list.add(NutritionItem("fiber", "Serat", formattedVal, "g", numVal, ThemeSlate))

            // Gula (Warna Ungu Soft/Pinkish Purple)
            key.contains("sugar") || key.contains("gula") ->
                list.add(NutritionItem("sugar", "Gula", formattedVal, "g", numVal, ThemeSoftViolet))

            // Kolesterol (Warna Terang)
            key.contains("chol") || key.contains("kolest") ->
                list.add(NutritionItem("cholesterol", "Kolest.", formattedVal, "mg", numVal, ThemePurpleLight))

            // Kalsium (Warna Abu Ungu)
            key.contains("calc") || key.contains("kals") ->
                list.add(NutritionItem("calcium", "Kalsium", formattedVal, "mg", numVal, Color(0xFFC5CAE9)))
        }
    }

    val order = listOf("carbs", "protein", "fat", "fiber", "sugar", "cholesterol", "calcium")
    val sortedList = list.sortedBy { val idx = order.indexOf(it.key); if (idx == -1) 99 else idx }
    return Pair(calorieItem, sortedList)
}