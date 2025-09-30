package com.example.project_mobileapps.features.getstarted

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.project_mobileapps.ui.themes.ProjectMobileAppsTheme

class GetStartedFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ProjectMobileAppsTheme {
                    GetStartedScreen(
                        onGetStartedClick = {
                            // Kirim perintah untuk menampilkan halaman Register
                            val action = GetStartedFragmentDirections.actionGetStartedFragmentToAuthFragment(showLoginByDefault = false)
                            findNavController().navigate(action)
                        },
                        onLoginClick = {
                            // Kirim perintah untuk menampilkan halaman Login
                            val action = GetStartedFragmentDirections.actionGetStartedFragmentToAuthFragment(showLoginByDefault = true)
                            findNavController().navigate(action)
                        }
                    )
                }
            }
        }
    }
}