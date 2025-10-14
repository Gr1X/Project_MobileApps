package com.example.project_mobileapps.data.repo

import com.example.project_mobileapps.data.local.DummyUserDatabase
import com.example.project_mobileapps.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay

object AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    suspend fun login(email: String, password: String): Result<User> {
        val user = DummyUserDatabase.users.find { it.email == email && it.password == password }

        return if (user != null) {
            _currentUser.value = user
            Result.success(user)
        } else {
            Result.failure(Exception("Email atau password salah."))
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    suspend fun register(name: String, email: String, password: String): Result<Boolean> {
        kotlinx.coroutines.delay(1000) // Simulasi loading
        // Dalam sistem dummy, kita anggap registrasi selalu berhasil
        return Result.success(true)
    }
}