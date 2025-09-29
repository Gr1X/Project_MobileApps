package com.example.project_mobileapps.features.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.project_mobileapps.ui.themes.ProjectMobileAppsTheme

class ScheduleFragment : Fragment() {

    private val viewModel: ScheduleViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(uiState.bookingStatus) {
                    val navController = findNavController()
                    when (uiState.bookingStatus) {
                        "Success" -> {
                            Toast.makeText(context, "Booking Berhasil!", Toast.LENGTH_SHORT).show()
                            // Kembali ke halaman home (start destination)
                            navController.popBackStack(navController.graph.startDestinationId, false)
                        }
                        "Error" -> {
                            Toast.makeText(context, "Booking Gagal, coba lagi.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                ProjectMobileAppsTheme {
                    ScheduleScreen(
                        uiState = uiState,
                        onKeluhanChanged = viewModel::onKeluhanChanged,
                        onTimeSelected = viewModel::onTimeSelected,
                        onConfirmBooking = viewModel::confirmBooking
                    )
                }
            }
        }
    }
}