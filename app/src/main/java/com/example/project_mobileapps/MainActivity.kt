package com.example.project_mobileapps

import android.os.Bundle
import android.view.View
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.project_mobileapps.databinding.ActivityMainBinding
import com.google.firebase.firestore.ktx.firestore // <-- TAMBAHKAN IMPORT INI
import com.google.firebase.ktx.Firebase // <-- TAMBAHKAN IMPORT INI

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Menggunakan binding.navView untuk BottomNavigationView
        // val navView: BottomNavigationView = binding.navView // Sudah diakses melalui binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.loginFragment, R.id.registerFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        // Listener untuk perubahan tujuan navigasi
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // TAMBAHKAN getStartedFragment DI SINI
                R.id.getStartedFragment,
                R.id.loginFragment,
                R.id.registerFragment -> {
                    binding.navView.visibility = View.GONE
                    supportActionBar?.hide()
                }
                else -> {
                    binding.navView.visibility = View.VISIBLE
                    supportActionBar?.show() // Tampilkan ActionBar juga jika diinginkan
                }
            }
        }
        testFirestoreConnection()
    }

    // FUNGSI UNTUK MENGETES KONEKSI KE FIRESTORE
    private fun testFirestoreConnection() {
        val db = Firebase.firestore
        val testData = hashMapOf(
            "status" to "connected",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("test").document("connection_check")
            .set(testData)
            .addOnSuccessListener {
                Log.d("FirestoreTest", "✅ Koneksi Berhasil: Dokumen berhasil ditulis!")
            }
            .addOnFailureListener { e ->
                Log.w("FirestoreTest", "❌ Koneksi Gagal: Error menulis dokumen", e)
            }
    }
}

