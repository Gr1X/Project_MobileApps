package com.example.project_mobileapps.features.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.project_mobileapps.R
import com.example.project_mobileapps.ui.themes.ProjectMobileAppsTheme

class AuthFragment : Fragment() {
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val authState by viewModel.authState.collectAsState()

                // State untuk mengatur layar mana yang tampil (Login atau Register)
                var showLoginScreen by remember { mutableStateOf(true) }

                LaunchedEffect(authState.isSuccess) {
                    if (authState.isSuccess) {
                        Toast.makeText(context, "Otentikasi Berhasil!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_authFragment_to_homeFragment)
                    }
                }

                ProjectMobileAppsTheme {
                    if (showLoginScreen) {
                        LoginScreen(
                            authState = authState,
                            onLoginClick = { email, pass ->
                                viewModel.loginUser(email, pass)
                            },
                            onNavigateToRegister = {
                                viewModel.resetAuthState() // Hapus pesan error lama
                                showLoginScreen = false // Pindah ke layar register
                            }
                        )
                    } else {
                        // Modifikasi RegisterScreen agar bisa kembali ke login
                        RegisterScreen(
                            authViewModel = viewModel,
                            onNavigateToLogin = {
                                viewModel.resetAuthState() // Hapus pesan error lama
                                showLoginScreen = true // Pindah ke layar login
                            }
                        )
                    }
                }
            }
        }
    }
}