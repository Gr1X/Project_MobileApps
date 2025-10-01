package com.example.project_mobileapps.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.project_mobileapps.R
import com.example.project_mobileapps.features.auth.AuthViewModel
import com.example.project_mobileapps.ui.themes.ProjectMobileAppsTheme

class HomeFragment : Fragment() {

    // 1. Dapatkan instance dari kedua ViewModel
    private val homeViewModel: HomeViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val uiState by homeViewModel.uiState.collectAsState()

                ProjectMobileAppsTheme {
                    HomeScreen(
                        uiState = uiState,
                        onDoctorClick = { doctorId ->
                            val action = HomeFragmentDirections.actionHomeFragmentToDoctorDetailFragment(doctorId)
                            findNavController().navigate(action)
                        },
                        onAppointmentClick = {
                            // 1. Ambil ID dari dokter pertama di daftar 'recentDoctors'
                            val doctorId = uiState.recentDoctors.firstOrNull()?.id

                            // 2. Cek apakah doctorId ada
                            if (doctorId != null) {
                                // 3. Buat aksi navigasi sambil mengirimkan doctorId
                                val action = HomeFragmentDirections.actionHomeFragmentToScheduleFragment(doctorId)
                                findNavController().navigate(action)
                            } else {
                                // 4. Jika tidak ada dokter, beri tahu pengguna
                                Toast.makeText(context, "Tidak ada dokter tersedia untuk membuat janji temu", Toast.LENGTH_SHORT).show()
                            }
                        },

                        onNewsClick = {
                            findNavController().navigate(R.id.action_homeFragment_to_newsFragment)
                        },

                        onLogOutClick = {
                            authViewModel.logOutUser()
                            // Navigasi kembali ke halaman awal dan bersihkan semua riwayat
                            val navOptions = NavOptions.Builder()
                                .setPopUpTo(findNavController().graph.startDestinationId, true)
                                .build()
                            findNavController().navigate(findNavController().graph.startDestinationId, null, navOptions)
                        }
                    )
                }
            }
        }
    }
}