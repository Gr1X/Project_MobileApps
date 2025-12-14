package com.example.project_mobileapps.utils

import android.util.Log
import com.example.project_mobileapps.data.model.Medicine
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object MedicineSeeder {

    // DAFTAR OBAT BARU (Tanpa Stok, Pakai 'form')
    private val initialMedicines = listOf(
        Medicine(name = "Paracetamol 500mg", category = "Analgesik", form = "Tablet"),
        Medicine(name = "Amoxicillin 500mg", category = "Antibiotik", form = "Kapsul"),
        Medicine(name = "CTM", category = "Antialergi", form = "Tablet"),
        Medicine(name = "OBH Sirup", category = "Obat Batuk", form = "Sirup"),
        Medicine(name = "Vitamin C 50mg", category = "Vitamin", form = "Tablet"),
        Medicine(name = "Betadine Antiseptik", category = "Antiseptik", form = "Cairan"),
        Medicine(name = "Oralit", category = "Elektrolit", form = "Serbuk"),
        Medicine(name = "Antasida Doen", category = "Lambung", form = "Tablet Kunyah"),
        Medicine(name = "Ibuprofen 400mg", category = "Analgesik", form = "Tablet"),
        Medicine(name = "Dexamethasone", category = "Kortikosteroid", form = "Tablet"),
        Medicine(name = "Salep 88", category = "Kulit", form = "Salep"),
        Medicine(name = "Sanmol Sirup", category = "Analgesik", form = "Sirup"),
        Medicine(name = "Amlodipine 5mg", category = "Hipertensi", form = "Tablet"),
        Medicine(name = "Metformin 500mg", category = "Diabetes", form = "Tablet"),
        Medicine(name = "Omeprazole 20mg", category = "Lambung", form = "Kapsul"),
        Medicine(name = "Alkohol 70%", category = "Antiseptik", form = "Cairan"),
        Medicine(name = "Neurobion", category = "Vitamin", form = "Tablet"),
        Medicine(name = "Insto", category = "Mata", form = "Tetes Mata"),
        Medicine(name = "Minyak Kayu Putih", category = "Herbal", form = "Cairan"),
        Medicine(name = "Simvastatin 10mg", category = "Kolesterol", form = "Tablet")
    )

    fun seedData() {
        val firestore = FirebaseFirestore.getInstance()
        val collection = firestore.collection("medicines")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Cek apakah data sudah ada
                val snapshot = collection.limit(1).get().await()
                if (!snapshot.isEmpty) {
                    Log.d("MedicineSeeder", "Data obat sudah ada. Skip seeding.")
                    return@launch
                }

                val batch = firestore.batch()
                initialMedicines.forEach { medicine ->
                    val docRef = collection.document()
                    // Pastikan ID diset
                    val dataWithId = medicine.copy(id = docRef.id)
                    batch.set(docRef, dataWithId)
                }

                batch.commit().await()
                Log.d("MedicineSeeder", "✅ Sukses insert data obat!")

            } catch (e: Exception) {
                Log.e("MedicineSeeder", "Gagal seeding", e)
            }
        }
    }
}