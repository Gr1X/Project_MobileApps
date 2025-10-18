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
/**
 * Singleton object yang bertindak sebagai Repository untuk autentikasi (Login, Logout, Register).
 * Ini adalah implementasi 'dummy' yang mengelola status login pengguna secara in-memory
 * dan menggunakan [DummyUserDatabase] sebagai sumber data.
 */
object AuthRepository {
    // Menyimpan user yang sedang login saat ini (MutableStateFlow internal)
    private val _currentUser = MutableStateFlow<User?>(null)
    /**
     * Aliran data reaktif (StateFlow) yang diekspos ke UI.
     * UI akan mengamati ini untuk mengetahui siapa user yang sedang login.
     * Bernilai `null` jika tidak ada yang login.
     */
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    /**
     * Mensimulasikan proses login.
     * @param email Email pengguna.
     * @param password Password pengguna.
     * @return [Result.success] berisi [User] jika login berhasil, [Result.failure] jika tidak.
     */
    suspend fun login(email: String, password: String): Result<User> {
        // Mencari user di database dummy
        val user = DummyUserDatabase.users.find { it.email == email && it.password == password }

        return if (user != null) {
            // Jika user ditemukan, update _currentUser yang akan memicu update ke UI
            _currentUser.value = user
            Result.success(user)
        } else {
            Result.failure(Exception("Email atau password salah."))
        }
    }
    /**
     * Mensimulasikan proses logout.
     * Mengatur [currentUser] kembali menjadi `null`.
     */
    suspend fun logout() {
        _currentUser.value = null
    }
    /**
     * Mensimulasikan proses registrasi pasien baru.
     * @param name Nama lengkap.
     * @param email Email baru.
     * @param password Password baru (nullable, mungkin untuk login via Google).
     * @param gender Jenis kelamin.
     * @param dateOfBirth Tanggal lahir.
     * @return [Result.success] berisi [User] baru jika berhasil, [Result.failure] jika email sudah terdaftar.
     */
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
    /**
     * Mengambil semua pengguna dari database dummy.
     * @return Daftar semua [User].
     */
    fun getAllUsers(): List<User> {
        return DummyUserDatabase.users
    }
    /**
     * Mencari pengguna (pasien) berdasarkan nama.
     * @param query Teks pencarian nama.
     * @return Daftar [User] yang cocok dan memiliki role PASIEN.
     */
    suspend fun searchUsersByName(query: String): List<User> {
        delay(300)
        if (query.isBlank()) {
            return emptyList()
        }
        return DummyUserDatabase.users.filter {
            it.name.contains(query, ignoreCase = true) && it.role == Role.PASIEN
        }
    }
    /**
     * Fungsi *DEBUGGING* / *DEVELOPMENT* untuk berganti role (misal: dari Pasien ke Admin)
     * tanpa perlu logout/login ulang.
     * @param newRole Role target (ADMIN, DOKTER, PASIEN).
     */
    fun switchUserRole(newRole: Role) {
        // Cari user pertama yang cocok dengan role yang dipilih
        val userToSwitch = DummyUserDatabase.users.find { it.role == newRole }
        if (userToSwitch != null) {
            _currentUser.value = userToSwitch
        }
    }
    /**
     * Memperbarui data pengguna (misal: setelah edit profile).
     * @param updatedUser Objek [User] dengan data yang sudah diperbarui.
     * @return [Result.success] jika berhasil, [Result.failure] jika user tidak ditemukan.
     */
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