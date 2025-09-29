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
import androidx.navigation.fragment.findNavController
import com.example.project_mobileapps.ui.themes.ProjectMobileAppsTheme
import com.example.project_mobileapps.R

class HomeFragment : Fragment() {

    // 1. Dapatkan instance ViewModel
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                // 2. Ambil dan "amati" data (uiState) dari ViewModel
                val uiState by viewModel.uiState.collectAsState()

                // 3. Gunakan Tema aplikasi Anda
                ProjectMobileAppsTheme {

                    // 4. Panggil Composable Screen utama dan berikan data serta aksi
                    HomeScreen(
                        uiState = uiState,
                        onDoctorClick = { doctorId ->
                            // Aksi saat kartu dokter diklik
                            val action = HomeFragmentDirections.actionHomeFragmentToDoctorDetailFragment(doctorId)
                            findNavController().navigate(action)
                        },

                        onAppointmentClick = {
                            // Aksi saat ikon "Appointment" diklik
                            val doctorId = uiState.recentDoctors.firstOrNull()?.id
                            if (doctorId != null) {
                                val action = HomeFragmentDirections.actionHomeFragmentToScheduleFragment(doctorId)
                                findNavController().navigate(action)
                            } else {
                                Toast.makeText(context, "Data dokter tidak ditemukan", Toast.LENGTH_SHORT).show()
                            }
                        },

                        onNewsClick = {
                            findNavController().navigate(R.id.action_homeFragment_to_newsFragment)
                        }
                    )
                }
            }
        }
    }
}