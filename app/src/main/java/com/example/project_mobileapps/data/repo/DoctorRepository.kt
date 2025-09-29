package com.example.project_mobileapps.data.repo // atau .repository sesuai nama package Anda

import com.example.project_mobileapps.data.model.Doctor
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class DoctorRepository {

    private val db = Firebase.firestore

    suspend fun getAllDoctors(): List<Doctor> {
        return try {
            val snapshot = db.collection("doctors").get().await()
            // Mengubah setiap dokumen menjadi objek Doctor secara otomatis
            snapshot.documents.mapNotNull { doc ->
                doc.toObject<Doctor>()?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDoctorById(doctorId: String): Doctor? {
        return try {
            val snapshot = db.collection("doctors").document(doctorId).get().await()
            snapshot.toObject<Doctor>()?.copy(id = snapshot.id)
        } catch (e: Exception) {
            null
        }
    }
}