package com.example.project_mobileapps.features.doctorDetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.project_mobileapps.ui.themes.ProjectMobileAppsTheme // Ganti dengan nama tema Anda

class DoctorDetailFragment : Fragment() {

    private val viewModel: DoctorDetailViewModel by viewModels()
    private val args: DoctorDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val doctor by viewModel.doctor.collectAsState()

                ProjectMobileAppsTheme {
                    DoctorDetailScreen(
                        doctor = doctor,
                        onBookingClick = {
                            val doctorId = args.doctorId
                            val action = DoctorDetailFragmentDirections.actionDoctorDetailFragmentToScheduleFragment(doctorId)
                            findNavController().navigate(action)
                        }
                    )
                }
            }
        }
    }
}