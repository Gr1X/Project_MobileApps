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
import androidx.navigation.fragment.navArgs

class AuthFragment : Fragment() {
    private val viewModel: AuthViewModel by viewModels()
    private val args: AuthFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val authState by viewModel.authState.collectAsState()
                var showLoginScreen by remember { mutableStateOf(args.showLoginByDefault) }

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
                            onLoginClick = viewModel::loginUser,
                            onNavigateToRegister = {
                                viewModel.resetAuthState()
                                showLoginScreen = false
                            }
                        )
                    } else {
                        RegisterScreen(
                            authViewModel = viewModel,
                            onNavigateToLogin = {
                                viewModel.resetAuthState()
                                showLoginScreen = true
                            }
                        )
                    }
                }
            }
        }
    }
}