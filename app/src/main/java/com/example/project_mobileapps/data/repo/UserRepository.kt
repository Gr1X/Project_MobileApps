package com.example.project_mobileapps.data.repo

import com.example.project_mobileapps.data.model.User
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
/**
 * Repository yang mengelola data pengguna (User) di Firebase Firestore.
 * Ini adalah implementasi *nyata* yang terhubung ke cloud.
 * (Berbeda dengan AuthRepository yang masih dummy).
 */
class UserRepository {
    private val usersCollection = Firebase.firestore.collection("users")
    /**
     * Membuat dokumen pengguna baru di Firestore.
     * @param user Objek [User] yang akan disimpan. Dokumen akan menggunakan [user.uid] sebagai ID.
     * @return `true` jika operasi 'set' berhasil, `false` jika terjadi error.
     */
    suspend fun createUser(user: User): Boolean {
        return try {
            usersCollection.document(user.uid).set(user).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    /**
     * Mengambil data pengguna dari Firestore berdasarkan UID.
     * @param uid ID unik pengguna (biasanya dari Firebase Auth).
     * @return Objek [User] jika ditemukan, `null` jika tidak ditemukan atau terjadi error.
     */
    suspend fun getUser(uid: String): User? {
        return try {
            val document = usersCollection.document(uid).get().await()
            document.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }
}