package com.example.project_mobileapps.data.repo

import android.app.Activity
import android.util.Log
import com.example.project_mobileapps.data.model.Gender
import com.example.project_mobileapps.data.model.Role
import com.example.project_mobileapps.data.model.User
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
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
import com.example.project_mobileapps.data.repo.FirestoreQueueRepository
import com.google.firebase.firestore.toObject

/**
 * Singleton object yang bertindak sebagai Repository untuk autentikasi (Login, Logout, Register).
 */
object AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val repoScope = CoroutineScope(Dispatchers.IO)

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null && firebaseUser.isEmailVerified) {
                // Hanya ambil data jika sudah verified
                repoScope.launch { fetchUserData(firebaseUser.uid) }
            } else {
                _currentUser.value = null
            }
        }
    }

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

    // --- REVISI: REGISTER TAHAP 1 (AUTH SAJA) ---
    suspend fun registerAuthOnly(
        name: String, email: String, pass: String,
        phone: String, gender: Gender, dob: String
    ): Result<String> { // Mengembalikan UID
        return try {
            // 1. Cek User Create (Akan error jika email sudah ada)
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val firebaseUser = authResult.user ?: throw Exception("Gagal membuat user")

            // 2. Update Profile Name (Biar di Auth ada namanya)
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            if (phone.isNotEmpty()) {
                // Kita jalankan di scope repository agar tidak menghalangi return
                repoScope.launch {
                    FirestoreQueueRepository.syncTemporaryRecordsToNewAccount(firebaseUser.uid, phone)
                }
            }

            // 3. Kirim Email Verifikasi
            firebaseUser.sendEmailVerification().await()

            // 4. Return UID (Jangan simpan ke Firestore dulu)
            Result.success(firebaseUser.uid)

        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Email sudah terdaftar. Silakan login."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerInitial(username: String, email: String, pass: String): Result<String> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val firebaseUser = authResult.user ?: throw Exception("Gagal membuat user")

            // Simpan Username ke DisplayName Auth (Sementara)
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            firebaseUser.sendEmailVerification().await()
            Result.success(firebaseUser.uid)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Email sudah terdaftar."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeUserProfile(
        uid: String,
        fullName: String,
        gender: Gender,
        dob: String,
        phone: String
    ): Result<User> {
        return try {
            val currentUserAuth = auth.currentUser
            val email = currentUserAuth?.email ?: ""
            // [PERBAIKAN] Ambil Username dari Auth DisplayName (yang diinput pas Register)
            val savedUsername = currentUserAuth?.displayName ?: "User"

            // Buat objek User lengkap dengan pemisahan Username & Nama Lengkap
            val newUser = User(
                uid = uid,
                username = savedUsername, // Simpan Username murni
                name = fullName,          // Simpan Nama Lengkap
                email = email,
                role = Role.PASIEN,
                gender = gender,
                dateOfBirth = dob,
                phoneNumber = phone,
                profilePictureUrl = ""
            )

            // Simpan ke Firestore
            usersCollection.document(uid).set(newUser).await()

            _currentUser.value = newUser
            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- REVISI: REGISTER TAHAP 2 (SIMPAN KE FIRESTORE) ---
    suspend fun saveUserToFirestore(
        uid: String, name: String, email: String,
        phone: String, gender: Gender, dob: String
    ): Result<User> {
        return try {
            val newUser = User(
                uid = uid,
                name = name,
                email = email,
                role = Role.PASIEN,
                gender = gender,
                dateOfBirth = dob,
                phoneNumber = phone,
                profilePictureUrl = "" // Default kosong
            )
            usersCollection.document(uid).set(newUser).await()

            // Set state agar UI login
            _currentUser.value = newUser
            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Gagal login")

            val whiteListedEmails = listOf("admin@klinik.com", "dokter@klinik.com", "pasien@klinik.com")
            val isWhitelisted = whiteListedEmails.contains(email)

            if (!firebaseUser.isEmailVerified && !isWhitelisted) {
                auth.signOut()
                throw Exception("Email belum diverifikasi. Silakan cek inbox Anda.")
            }

            fetchUserData(firebaseUser.uid)

            val snapshot = usersCollection.document(firebaseUser.uid).get().await()

            if (snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                _currentUser.value = user
                Result.success(user)
            } else {
                Result.success(null)
            }

            val user = snapshot.toObject(User::class.java)
                ?: throw Exception("Data user tidak ditemukan di database.")

            _currentUser.value = user
            Result.success(user)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fungsi Reload untuk Polling
    suspend fun reloadUser(): Boolean {
        return try {
            val user = auth.currentUser
            user?.reload()?.await()
            user?.isEmailVerified == true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        auth.signOut()
        _currentUser.value = null
    }

    suspend fun updateUser(updatedUser: User): Result<Unit> {
        return try {
            usersCollection.document(updatedUser.uid).set(updatedUser).await()
            _currentUser.value = updatedUser
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserById(userId: String): User? {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            // Konversi dokumen Firestore menjadi objek User
            snapshot.toObject<User>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
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

                val phone = existingUser.phoneNumber
                if (phone.isNotEmpty() && phone != "N/A") {
                    FirestoreQueueRepository.syncTemporaryRecordsToNewAccount(existingUser.uid, phone)
                }

                Result.success(existingUser)

            } else {
                // User Baru
                val phone = firebaseUser.phoneNumber ?: ""

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

                if (phone.isNotEmpty()) {
                    FirestoreQueueRepository.syncTemporaryRecordsToNewAccount(newUser.uid, phone)
                }

                Result.success(newUser)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): List<User> {
        return try {
            val snapshot = usersCollection.get().await()
            snapshot.toObjects(User::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

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

            // [AUTO-SYNC] JEMBATAN KONEKSI DATA
            // Panggil fungsi di QueueRepository untuk memindahkan data 'temp_08xx' ke 'UID' baru
            if (phone.isNotEmpty()) {
                FirestoreQueueRepository.syncTemporaryRecordsToNewAccount(newUser.uid, phone)
            }

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
}