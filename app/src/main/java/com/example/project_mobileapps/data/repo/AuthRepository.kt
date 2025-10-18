package com.example.project_mobileapps.data.repo

import com.example.project_mobileapps.data.local.DummyUserDatabase
import com.example.project_mobileapps.data.model.Gender
import com.example.project_mobileapps.data.model.Role
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.di.AppContainer
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

    suspend fun logout() {
        _currentUser.value = null
    }

    suspend fun register(
        name: String,
        email: String,
        password: String? = null,
        gender: Gender = Gender.PRIA,
        dateOfBirth: String = "N/A",
        phoneNumber: String = ""
    ): Result<User> {
        delay(1000)
        if (DummyUserDatabase.users.any { it.email == email }) {
            return Result.failure(Exception("Email sudah terdaftar."))
        }
        val newUid = "pasien_${System.currentTimeMillis()}"
        val newUser = User(
            uid = newUid,
            name = name,
            email = email,
            password = password,
            role = Role.PASIEN,
            gender = gender,
            dateOfBirth = dateOfBirth,
            phoneNumber = phoneNumber
        )
        DummyUserDatabase.users.add(newUser)
        return Result.success(newUser)
    }

    fun getAllUsers(): List<User> {
        return DummyUserDatabase.users
    }

    suspend fun searchUsersByName(query: String): List<User> {
        delay(300)
        if (query.isBlank()) {
            return emptyList()
        }
        return DummyUserDatabase.users.filter {
            it.name.contains(query, ignoreCase = true) && it.role == Role.PASIEN
        }
    }

    fun switchUserRole(newRole: Role) {
        // Cari user pertama yang cocok dengan role yang dipilih
        val userToSwitch = DummyUserDatabase.users.find { it.role == newRole }
        if (userToSwitch != null) {
            _currentUser.value = userToSwitch
        }
    }

    suspend fun updateUser(updatedUser: User): Result<Unit> {
        val userIndex = DummyUserDatabase.users.indexOfFirst { it.uid == updatedUser.uid }
        if (userIndex != -1) {
            DummyUserDatabase.users[userIndex] = updatedUser
            _currentUser.value = updatedUser
            return Result.success(Unit)
        }
        return Result.failure(Exception("User tidak ditemukan"))
    }
}