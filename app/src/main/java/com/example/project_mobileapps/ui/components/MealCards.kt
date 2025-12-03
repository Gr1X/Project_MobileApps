package com.example.project_mobileapps.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_mobileapps.data.model.ml.Macro

/**
 * Kartu reusable untuk menampilkan satu rekomendasi makan (Pagi/Siang/Malam)
 * lengkap dengan Grid Nutrisi di bawahnya.
 */
@Composable
fun MealRecommendationCard(
    mealTime: String, // Contoh: "Sarapan"
    foodName: String, // Contoh: "Oatmeal dengan Buah"
    macros: Macro?,   // Data Gizi
    color: Color      // Warna tema kartu
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 1. Header: Chip Waktu Makan
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = mealTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Nama Makanan Utama
            Text(
                text = foodName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 3. Grid Nutrisi (Hanya jika data ada)
            if (macros != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MacroItem(Icons.Rounded.LocalFireDepartment, "${macros.calories.toInt()}", "kcal", "Energi", Color(0xFFFF7043))
                    MacroItem(Icons.Rounded.FitnessCenter, "${macros.protein.toInt()}", "g", "Protein", Color(0xFF5C6BC0))
                    MacroItem(Icons.Rounded.Restaurant, "${macros.carbs.toInt()}", "g", "Karbo", Color(0xFFFFA726))
                    MacroItem(Icons.Rounded.WaterDrop, "${macros.fat.toInt()}", "g", "Lemak", Color(0xFF78909C))
                }
            }
        }
    }
}

@Composable
private fun MacroItem(
    icon: ImageVector,
    value: String,
    unit: String,
    label: String,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "$value$unit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
    }
}