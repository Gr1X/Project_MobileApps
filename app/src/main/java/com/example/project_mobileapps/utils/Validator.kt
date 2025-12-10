// File: utils/Validator.kt
package com.example.project_mobileapps.utils

import android.util.Patterns

enum class PasswordStrength(val label: String, val color: Long, val progress: Float) {
    WEAK("Lemah", 0xFFEF5350, 0.33f),   // Merah
    MEDIUM("Sedang", 0xFFFFA726, 0.66f), // Oranye
    STRONG("Kuat", 0xFF66BB6A, 1.0f),    // Hijau
    NONE("", 0xFFE0E0E0, 0.0f)           // Abu-abu (kosong)
}

object Validator {

    fun validateName(name: String): ValidationResult {
        return if (name.isBlank()) {
            ValidationResult.Error("Nama tidak boleh kosong")
        } else if (name.length < 3) {
            ValidationResult.Error("Nama minimal 3 karakter")
        } else {
            ValidationResult.Success
        }
    }

    fun validateEmail(email: String): ValidationResult {
        return if (email.isBlank()) {
            ValidationResult.Error("Email tidak boleh kosong")
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            ValidationResult.Error("Format email tidak valid (contoh: user@mail.com)")
        } else {
            ValidationResult.Success
        }
    }

    fun validatePassword(password: String): ValidationResult {
        return if (password.isBlank()) {
            ValidationResult.Error("Password tidak boleh kosong")
        } else if (password.length < 8) {
            ValidationResult.Error("Password minimal 8 karakter")
        } else if (!password.any { it.isDigit() } || !password.any { it.isLetter() }) {
            // Standar keamanan profesional: Harus kombinasi huruf & angka
            ValidationResult.Error("Password harus kombinasi huruf dan angka")
        } else {
            ValidationResult.Success
        }
    }

    fun validateConfirmPassword(pass: String, confirmPass: String): ValidationResult {
        return if (confirmPass.isBlank()) {
            ValidationResult.Error("Konfirmasi password wajib diisi")
        } else if (pass != confirmPass) {
            ValidationResult.Error("Password tidak cocok")
        } else {
            ValidationResult.Success
        }
    }

    // Logic Pengecekan Kekuatan Password
    fun checkPasswordStrength(password: String): PasswordStrength {
        if (password.isBlank()) return PasswordStrength.NONE

        var score = 0
        // Kriteria 1: Panjang > 8
        if (password.length >= 8) score++
        // Kriteria 2: Ada Angka
        if (password.any { it.isDigit() }) score++
        // Kriteria 3: Ada Huruf Besar
        if (password.any { it.isUpperCase() }) score++
        // Kriteria 4: Ada Simbol Unik (@, #, $, dll)
        if (password.any { !it.isLetterOrDigit() }) score++

        return when (score) {
            0, 1, 2 -> PasswordStrength.WEAK
            3 -> PasswordStrength.MEDIUM
            4 -> PasswordStrength.STRONG
            else -> PasswordStrength.WEAK
        }
    }

    fun validatePhone(phone: String): ValidationResult {
        return if (phone.isBlank()) {
            ValidationResult.Error("Nomor HP tidak boleh kosong")
        } else if (!phone.all { it.isDigit() || it == '+' }) {
            ValidationResult.Error("Hanya angka yang diperbolehkan")
        } else if (phone.length < 10) {
            ValidationResult.Error("Nomor HP terlalu pendek")
        } else {
            ValidationResult.Success
        }
    }
}

// Sealed class untuk hasil validasi yang mudah dibaca UI
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}