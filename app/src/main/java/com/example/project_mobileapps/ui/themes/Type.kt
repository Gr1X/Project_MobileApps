package com.example.project_mobileapps.ui.themes

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.project_mobileapps.R

// 1. Definisikan keluarga font Poppins dengan semua varian ketebalan yang Anda punya
/**
 * Mendefinisikan [FontFamily] kustom menggunakan file font Poppins dari `res/font`.
 * Ini mengelompokkan semua varian ketebalan font Poppins ke dalam satu keluarga.
 */
val PoppinsFamily = FontFamily(
    Font(R.font.poppins_thin, FontWeight.Thin),
    Font(R.font.poppins_extralight, FontWeight.ExtraLight),
    Font(R.font.poppins_light, FontWeight.Light),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_extrabold, FontWeight.ExtraBold),
    Font(R.font.poppins_black, FontWeight.Black)
)

// 2. Buat "Skala Tipografi" yang lengkap menggunakan font Poppins
/**
 * Mendefinisikan skala tipografi kustom untuk aplikasi menggunakan [PoppinsFamily].
 * Objek [Typography] ini akan di-pass ke [MaterialTheme] di `Theme.kt`.
 * Setiap [TextStyle] mendefinisikan `fontFamily`, `fontWeight`, `fontSize`, `lineHeight`, dll.
 * Nama-nama properti (misal: `headlineLarge`, `bodyMedium`) mengikuti skala tipe Material Design 3.
 */
val Typography = Typography(
    // Judul Paling Besar (misal: "Jadwal Praktik" di header)
    headlineLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    // Judul Sedang (misal: Sapaan "Hello, Budi Santoso")
    headlineMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    // Judul Kartu Utama (misal: "Dr. Budi Santoso")
    titleLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    // Judul Sedang (misal: "Pantauan Antrian")
    titleMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    // Judul Kecil (misal: di dalam list item)
    titleSmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp
    ),
    // Teks Isi/Body Utama
    bodyLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    // Teks Isi/Body Sedang
    bodyMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    // Teks Label (misal: untuk tombol atau input field)
    labelLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // Teks Label Kecil (misal: sub-judul "Antrian di Depan")
    labelMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)