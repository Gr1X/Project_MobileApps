package com.example.project_mobileapps.data.repo

import com.example.project_mobileapps.data.model.Medicine
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MedicineRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val medicineCollection = firestore.collection("medicines")

    // Get Realtime Updates
    fun getMedicinesFlow(): Flow<List<Medicine>> {
        return medicineCollection.snapshots().map { snapshot ->
            snapshot.toObjects(Medicine::class.java)
        }
    }

    suspend fun addMedicine(medicine: Medicine): Result<Unit> {
        return try {
            val docRef = medicineCollection.document()
            val newMedicine = medicine.copy(id = docRef.id)
            docRef.set(newMedicine).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMedicine(medicine: Medicine): Result<Unit> {
        return try {
            medicineCollection.document(medicine.id).set(medicine).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMedicine(medicineId: String): Result<Unit> {
        return try {
            medicineCollection.document(medicineId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}