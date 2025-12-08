// File: ui/components/MealRecommendationCard.kt
package com.example.project_mobileapps.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MealRecommendationCard(
    mealTime: String,
    foodName: String?,
    macros: Any?, // String "{calories=..., protein=...}"
    color: Color,
    onClick: () -> Unit // Aksi untuk tombol "Lihat Resep"
) {
    // Parsing data nutrisi menjadi List object yang siap pakai untuk Grafik
    val nutritionList = remember(macros) {
        parseNutritionDataForBars(macros)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(20.dp), // iOS Style Rounded
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
        // HAPUS .clickable() dari sini agar kartu tidak bisa ditekan sembarangan
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // 1. HEADER (Waktu & Nama Makanan)
            Row(verticalAlignment = Alignment.Top) {
                // Ikon Makanan Bulat
                Surface(
                    color = color.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Restaurant,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    // Label Waktu (Capsule)
                    Surface(
                        color = Color(0xFFF2F2F7), // iOS Gray
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = mealTime.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Nama Makanan Besar
                    Text(
                        text = foodName ?: "Belum ada rekomendasi",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF1C1C1E),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 2. STATISTIK NUTRISI (BARS)
            if (nutritionList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFFF2F2F7))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Informasi Nutrisi",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Loop setiap nutrisi dan buat Bar Chart
                nutritionList.forEach { item ->
                    NutritionProgressBar(
                        label = item.label,
                        valueText = item.value,
                        percentage = item.percentage, // 0.0f - 1.0f
                        color = item.color
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // 3. TOMBOL AKSI (LIHAT RESEP)
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lihat Cara Masak (Resep)", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// --- SUB-COMPONENT: PROGRESS BAR ---
@Composable
fun NutritionProgressBar(
    label: String,
    valueText: String,
    percentage: Float,
    color: Color
) {
    // Animasi Progress Bar
    val animatedProgress by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 1000) // Durasi animasi 1 detik
    )

    Column {
        // Label & Value (Kiri Kanan)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bar Visual
        LinearProgressIndicator(
            progress = { animatedProgress }, // Gunakan nilai animasi
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp) // Lebih tipis (Minimalis)
                .clip(RoundedCornerShape(50)), // Rounded ends
            color = color,
            trackColor = Color(0xFFF2F2F7), // Abu background bar
            strokeCap = StrokeCap.Round,
        )
    }
}

// --- DATA CLASS UNTUK PARSING ---
data class NutritionData(
    val label: String,
    val value: String,
    val percentage: Float,
    val color: Color
)

// --- LOGIC PARSING PINTAR ---
fun parseNutritionDataForBars(macros: Any?): List<NutritionData> {
    if (macros == null) return emptyList()

    val rawString = macros.toString() // Contoh: "{calories=500, protein=30g, carbs=50g}"
    val cleanString = rawString.replace("{", "").replace("}", "")
    val parts = cleanString.split(",")

    val result = mutableListOf<NutritionData>()
    // Set untuk melacak label yang sudah ditambahkan agar tidak duplikat
    val seenLabels = mutableSetOf<String>()

    for (part in parts) {
        val pair = part.split("=")
        if (pair.size == 2) {
            val key = pair[0].trim().lowercase()
            val valueStr = pair[1].trim()

            // Ekstrak angka dari string (misal "30g" -> 30.0)
            val numberValue = valueStr.filter { it.isDigit() || it == '.' }.toFloatOrNull() ?: 0f

            // Tentukan Label, Warna, dan Persentase Visual (Estimasi Max)
            val (label, color, maxVal) = when {
                key.contains("cal") -> Triple("Kalori", Color(0xFFFF9800), 800f) // Max 800 kcal per meal
                key.contains("prot") -> Triple("Protein", Color(0xFF2196F3), 50f) // Max 50g
                key.contains("carb") -> Triple("Karbohidrat", Color(0xFF4CAF50), 100f) // Max 100g
                key.contains("fat") || key.contains("lemak") -> Triple("Lemak", Color(0xFFF44336), 40f) // Max 40g
                else -> Triple(key.capitalize(), Color.Gray, 100f)
            }

            // [FIX DUPLIKAT] Cek apakah label ini sudah ada
            if (!seenLabels.contains(label)) {
                // Hitung persentase bar (0.0 - 1.0)
                val progress = (numberValue / maxVal).coerceIn(0.05f, 1.0f) // Min 5% biar bar kelihatan
                result.add(NutritionData(label, valueStr, progress, color))
                seenLabels.add(label)
            }
        }
    }
    // Urutkan biar rapi: Kalori -> Protein -> Karbo -> Lemak
    return result.sortedBy {
        when(it.label) {
            "Kalori" -> 1
            "Protein" -> 2
            "Karbohidrat" -> 3
            else -> 4
        }
    }
}

// Extension kapitalisasi
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}