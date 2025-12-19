package com.example.project_mobileapps.ui.themes

import androidx.compose.ui.graphics.Color

// PALET WARNA KUSTOM "Modern Clinic"
// Warna-warna ini digunakan untuk mendefinisikan LightColorScheme di Theme.kt

// --- Warna Utama ---
/** Warna primer utama aplikasi (Biru keunguan). Digunakan untuk tombol utama, header, highlight. */
val PrimaryPeriwinkle = Color(0xFF6D80E3) // Biru keunguan yang dominan untuk kartu utama dan highlight
val OnPrimary = Color.White               // Teks putih di atas warna utama

// Warna Background & Surface
val AppBackground = Color(0xFFF4F5F9)     // Abu-abu sangat terang dengan sedikit nuansa biru untuk background utama
val Surface = Color.White                 // Putih bersih untuk latar belakang kartu

// Warna Aksen & Lainnya
val AccentStar = Color(0xFFFFC107)        // Kuning untuk ikon bintang rating
val LightPurpleGray = Color(0xFFE8EAF6)

// --- EXISTING COLORS (Bawaan Project) ---
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// --- NEW ADMIN DASHBOARD PALETTE ---

// Backgrounds
val AdminBackground = Color(0xFFF8F9FA) // Abu-abu sangat muda bersih

// Brand & Actions
  // Biru Utama (PrimaryBlue)

// Semantic / Status Colors
val StatusSuccess = Color(0xFF10B981)   // Hijau (Selesai/Naik)
val StatusWarning = Color(0xFFF59E0B)   // Orange (Menunggu)
val StatusError = Color(0xFFF56565)     // Merah (Batal/Danger)

// Typography (Teks)

// Charts & Visualizations
val ChartGradientStart = Color(0xFF3B82F6)
val ChartGradientEnd = Color(0xFF93C5FD)
val DonutEmpty = Color(0xFFE2E8F0)
val GridLineColor = Color(0xFFCBD5E1)   // Warna garis bantu chart

// Gender Specific
val GenderMale = Color(0xFF3B82F6)      // Biru Cowok
val GenderFemale = Color(0xFFEC4899)    // Pink Cewek

// 1. BRAND COLORS (Identitas Utama - Biru Keunguan/Periwinkle)
// Menggunakan tone yang "Trustworthy" tapi tetap modern.
val BrandPrimary = Color(0xFF6C63FF)      // Periwinkle Utama (Vibrant tapi Soft)
val BrandSecondary = Color(0xFF5A52D5)    // Versi sedikit lebih gelap untuk status bar/button press
val BrandLight = Color(0xFFEFEFFF)        // Periwinkle sangat muda (untuk background kartu aktif)

// 2. BACKGROUND & NEUTRALS (Basis Aplikasi - Putih Bersih)
// Kita hindari Putih #FFFFFF total untuk background layar agar mata tidak cepat lelah.
val BackgroundApp = Color(0xFFF9FAFB)     // Abu-abu sangat muda (hampir putih), standar aplikasi modern
val SurfaceWhite = Color(0xFFFFFFFF)      // Putih murni khusus untuk KARTU (Card) agar pop-up
val SurfaceSubtle = Color(0xFFF3F4F6)     // Abu muda untuk divider atau isian text field

// 3. TEXT COLORS (Keterbacaan)
// Jangan gunakan Hitam #000000, terlalu kontras dan kasar.
val TextPrimary = Color(0xFF1F2937)       // "Gunmetal" (Abu Gelap) - Enak dibaca
val TextSecondary = Color(0xFF6B7280)     // Abu Sedang - Untuk subtitle/caption
val TextTertiary = Color(0xFF9CA3AF)      // Abu Terang - Untuk placeholder

// 4. SEMANTIC COLORS (Status - Disesuaikan agar tidak nabrak)
// ERROR: Jangan pakai Merah #FF0000. Pakai "Soft Rose".
val StateError = Color(0xFFEF4444)        // Merah Rose (Jelas tapi tidak sakit mata)
val StateErrorBg = Color(0xFFFEF2F2)      // Latar belakang merah sangat muda

// SUCCESS: Jangan pakai Hijau Neon. Pakai "Emerald/Mint".
val StateSuccess = Color(0xFF10B981)      // Emerald Green (Tenang & Positif)
val StateSuccessBg = Color(0xFFECFDF5)    // Latar belakang hijau sangat muda

// WARNING: Pakai Amber/Kunyit, bukan Oranye traffic cone.
val StateWarning = Color(0xFFF59E0B)      // Amber
val StateWarningBg = Color(0xFFFFFBEB)    // Latar belakang kuning muda

// INFO: Biru Langit
val StateInfo = Color(0xFF3B82F6)         // Sky Blue
val StateInfoBg = Color(0xFFEFF6FF)       // Latar belakang biru muda

// 5. CHART COLORS (Khusus Grafik Admin/Pasien)
val ChartBlue = BrandPrimary
val ChartTeal = Color(0xFF2DD4BF)
val ChartPurple = Color(0xFFC084FC)
val ChartOrange = Color(0xFFFB923C)