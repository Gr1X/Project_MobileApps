package com.example.project_mobileapps.data.repo

import android.util.Log
import com.example.project_mobileapps.data.model.Gender
import com.example.project_mobileapps.data.model.Role
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.di.AppContainer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Singleton object yang bertindak sebagai Repository untuk autentikasi (Login, Logout, Register).
 * Ini adalah implementasi 'dummy' yang mengelola status login pengguna secara in-memory
 * dan menggunakan [DummyUserDatabase] sebagai sumber data.
 */
object AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")


    // Menyimpan user yang sedang login saat ini (MutableStateFlow internal)
    private val _currentUser = MutableStateFlow<User?>(null)
    /**
     * Aliran data reaktif (StateFlow) yang diekspos ke UI.
     * UI akan mengamati ini untuk mengetahui siapa user yang sedang login.
     * Bernilai `null` jika tidak ada yang login.
     */
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val repoScope = CoroutineScope(Dispatchers.IO)

    // Init block untuk memantau status login Firebase secara otomatis
    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                // Jika ada user Firebase login, ambil data lengkapnya dari Firestore
                repoScope.launch {
                    fetchUserData(firebaseUser.uid)
                }
            } else {
                // Jika logout
                _currentUser.value = null
            }
        }
    }

    // Fungsi internal untuk mengambil data user dari Firestore
    private suspend fun fetchUserData(uid: String) {
        try {
            val snapshot = usersCollection.document(uid).get().await()
            val user = snapshot.toObject(User::class.java)
            _currentUser.value = user
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching user data", e)
            _currentUser.value = null
        }
    }

    /**
     * Mensimulasikan proses login.
     * @param email Email pengguna.
     * @param password Password pengguna.
     * @return [Result.success] berisi [User] jika login berhasil, [Result.failure] jika tidak.
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            // 1. Login ke Firebase Auth
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("Gagal mendapatkan UID")

            // 2. Ambil data detail dari Firestore
            val snapshot = usersCollection.document(uid).get().await()
            val user = snapshot.toObject(User::class.java)

            if (user != null) {
                _currentUser.value = user
                Result.success(user)
            } else {
                Result.failure(Exception("Data pengguna tidak ditemukan di Firestore."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mensimulasikan proses logout.
     * Mengatur [currentUser] kembali menjadi `null`.
     */
    suspend fun logout() {
        auth.signOut()
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
        return try {
            if (password == null) throw Exception("Password diperlukan untuk registrasi email")

            // 1. Buat akun di Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("Gagal membuat user")

            // 2. Siapkan objek User untuk disimpan di Firestore
            val newUser = User(
                uid = uid,
                name = name,
                email = email,
                role = Role.PASIEN, // Default register adalah PASIEN
                gender = gender,
                dateOfBirth = dateOfBirth,
                phoneNumber = phoneNumber
            )

            // 3. Simpan ke Firestore
            usersCollection.document(uid).set(newUser).await()

            // 4. Update state lokal
            _currentUser.value = newUser
            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengambil semua pengguna dari database dummy.
     * @return Daftar semua [User].
     */
    suspend fun getAllUsers(): List<User> {
        return try {
            val snapshot = usersCollection.get().await()
            snapshot.toObjects(User::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Mencari pengguna (pasien) berdasarkan nama.
     * @param query Teks pencarian nama.
     * @return Daftar [User] yang cocok dan memiliki role PASIEN.
     */
    suspend fun searchUsersByName(query: String): List<User> {
        return try {
            // Catatan: Firestore search sederhana (case-sensitive biasanya).
            // Untuk production yang lebih baik, perlu solusi text-search seperti Algolia,
            // tapi untuk sekarang kita pakai query dasar.
            usersCollection
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
                .whereEqualTo("role", Role.PASIEN.name) // Pastikan enum disimpan sebagai String di Firestore jika pakai .name
                .get()
                .await()
                .toObjects(User::class.java)
        } catch (e: Exception) {
            // Fallback jika query di atas bermasalah karena index atau enum
            // Ambil semua pasien lalu filter di client (TIDAK DISARANKAN UNTUK PRODUCTION BESAR)
            getAllUsers().filter {
                it.name.contains(query, ignoreCase = true) && it.role == Role.PASIEN
            }
        }
    }

    /**
     * Fungsi *DEBUGGING* / *DEVELOPMENT* untuk berganti role (misal: dari Pasien ke Admin)
     * tanpa perlu logout/login ulang.
     * @param newRole Role target (ADMIN, DOKTER, PASIEN).
     */
    suspend fun switchUserRole(newRole: Role) {
        // Opsional: Implementasi untuk debug, misalnya memaksa ubah role di state lokal saja
        _currentUser.value = _currentUser.value?.copy(role = newRole)
    }

    /**
     * Memperbarui data pengguna (misal: setelah edit profile).
     * @param updatedUser Objek [User] dengan data yang sudah diperbarui.
     * @return [Result.success] jika berhasil, [Result.failure] jika user tidak ditemukan.
     */
    suspend fun updateUser(updatedUser: User): Result<Unit> {
        return try {
            usersCollection.document(updatedUser.uid).set(updatedUser).await()
            _currentUser.value = updatedUser
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}