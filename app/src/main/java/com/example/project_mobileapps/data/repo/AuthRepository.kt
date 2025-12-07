package com.example.project_mobileapps.data.repo

import android.app.Activity
import android.util.Log
import com.example.project_mobileapps.data.model.Gender
import com.example.project_mobileapps.data.model.Role
import com.example.project_mobileapps.data.model.User
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Singleton object yang bertindak sebagai Repository untuk autentikasi (Login, Logout, Register).
 */
object AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    // Menyimpan user yang sedang login saat ini (MutableStateFlow internal)
    private val _currentUser = MutableStateFlow<User?>(null)

    /**
     * Aliran data reaktif (StateFlow) yang diekspos ke UI.
     */
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val repoScope = CoroutineScope(Dispatchers.IO)

    // --- VARIABEL UNTUK MENYIMPAN DATA OTP SEMENTARA ---
    // Harus var (mutable) dan nullable agar bisa diisi saat kode terkirim
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    // Init block untuk memantau status login Firebase secara otomatis
    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                repoScope.launch {
                    fetchUserData(firebaseUser.uid)
                }
            } else {
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

    // --- FUNGSI BARU: GOOGLE LOGIN ---
    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Gagal autentikasi Google")

            val docRef = usersCollection.document(firebaseUser.uid)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                // User Lama
                val existingUser = snapshot.toObject(User::class.java)!!
                _currentUser.value = existingUser
                Result.success(existingUser)
            } else {
                // User Baru
                val newUser = User(
                    uid = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "User Baru",
                    email = firebaseUser.email ?: "",
                    role = Role.PASIEN,
                    profilePictureUrl = firebaseUser.photoUrl.toString(),
                    phoneNumber = firebaseUser.phoneNumber ?: "N/A"
                )
                docRef.set(newUser).await()
                _currentUser.value = newUser
                Result.success(newUser)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 1. FUNGSI KIRIM OTP
    fun sendOtp(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: () -> Unit,
        onError: (String) -> Unit
    ) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d("AuthRepo", "onVerificationCompleted: $credential")
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e("AuthRepo", "Gagal Kirim SMS", e)
                onError(e.message ?: "Gagal mengirim SMS")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d("AuthRepo", "SMS Terkirim! ID: $verificationId")
                // Simpan verificationId dan token ke variabel di object ini
                storedVerificationId = verificationId
                resendToken = token

                // Panggil callback sukses
                onCodeSent()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // 2. FUNGSI VERIFIKASI KODE & LINK KE AKUN
    suspend fun verifyOtp(code: String): Result<Unit> {
        return try {
            val verificationId = storedVerificationId ?: throw Exception("Terjadi kesalahan sistem (Verification ID null). Silakan kirim ulang kode.")

            // Buat Credential dari kode
            val credential = PhoneAuthProvider.getCredential(verificationId, code)

            // Ambil user yang baru saja login (via Register Email/Pass)
            val currentUser = auth.currentUser ?: throw Exception("User tidak ditemukan. Silakan login ulang.")

            // Hubungkan No HP ke Akun Email tersebut
            currentUser.linkWithCredential(credential).await()

            // Opsional: Update status di Firestore
            // usersCollection.document(currentUser.uid).update("isPhoneVerified", true).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mensimulasikan proses login.
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("Gagal mendapatkan UID")

            fetchUserData(uid)

            // Kita return user dari _currentUser jika sudah ter-fetch, atau coba fetch manual
            if (_currentUser.value != null) {
                Result.success(_currentUser.value!!)
            } else {
                // Fallback fetch sync (bisa jadi race condition dgn init, tapi aman di sini)
                val snapshot = usersCollection.document(uid).get().await()
                val user = snapshot.toObject(User::class.java)
                if (user != null) {
                    _currentUser.value = user
                    Result.success(user)
                } else {
                    Result.failure(Exception("Data pengguna tidak ditemukan."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mensimulasikan proses logout.
     */
    suspend fun logout() {
        auth.signOut()
        _currentUser.value = null
    }

    /**
     * Mensimulasikan proses registrasi pasien baru.
     */
    suspend fun register(
        name: String, email: String, pass: String?,
        gender: Gender = Gender.PRIA, dob: String = "N/A", phone: String = ""
    ): Result<User> { // <--- KEMBALI KE USER
        return try {
            if (pass == null) throw Exception("Password required")

            // 1. Buat Akun
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val firebaseUser = authResult.user ?: throw Exception("Gagal buat user")

            // 2. Buat Objek User
            val newUser = User(
                uid = firebaseUser.uid, name = name, email = email, role = Role.PASIEN,
                gender = gender, dateOfBirth = dob, phoneNumber = phone
            )

            // 3. Simpan Data ke Firestore
            usersCollection.document(firebaseUser.uid).set(newUser).await()

            // 4. KIRIM LINK VERIFIKASI
            firebaseUser.sendEmailVerification().await()
            Log.d("AuthRepo", "Email verifikasi terkirim ke $email")

            // KEMBALIKAN OBJEK USER (Bukan Boolean)
            Result.success(newUser)

        } catch (e: Exception) {
            Log.e("AuthRepo", "Gagal register", e)
            Result.failure(e)
        }
    }

    /**
     * Mengambil semua pengguna.
     */
    suspend fun getAllUsers(): List<User> {
        return try {
            val snapshot = usersCollection.get().await()
            snapshot.toObjects(User::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveUserToFirestore(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.uid).set(user).await()
            // Setelah simpan ke DB, baru kita set state global currentUser
            _currentUser.value = user
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun reloadUser(): User? {
        return try {
            val user = auth.currentUser
            user?.reload()?.await() // Paksa refresh dari server Firebase

            if (user != null && user.isEmailVerified) {
                // Jika sudah verified, ambil data lengkap dari Firestore
                fetchUserData(user.uid)
                _currentUser.value
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Mencari pengguna (pasien) berdasarkan nama.
     */
    suspend fun searchUsersByName(query: String): List<User> {
        return try {
            usersCollection
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
                .whereEqualTo("role", Role.PASIEN.name)
                .get()
                .await()
                .toObjects(User::class.java)
        } catch (e: Exception) {
            // Fallback filter client side jika index belum siap
            getAllUsers().filter {
                it.name.contains(query, ignoreCase = true) && it.role == Role.PASIEN
            }
        }
    }

    /**
     * Memperbarui data pengguna.
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