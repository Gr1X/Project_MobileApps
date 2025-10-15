package com.example.project_mobileapps.features.auth

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_mobileapps.data.model.User

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory()),
    startScreen: String,
    onAuthSuccess: (User) -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current
    var showLoginScreen by remember { mutableStateOf(startScreen == "login") }

    LaunchedEffect(authState.loggedInUser) {
        authState.loggedInUser?.let { user ->
            Toast.makeText(context, "Login Berhasil!",
                Toast.LENGTH_SHORT).show()
            onAuthSuccess(user)
        }
    }

    if (showLoginScreen) {
        LoginScreen(
            authState = authState,
            onLoginClick = authViewModel::loginUser,
            onNavigateToRegister = {
                authViewModel.resetAuthState()
                showLoginScreen = false // Pindah ke layar register
            }
        )
    } else {
        RegisterScreen(
            authViewModel = authViewModel,
            onNavigateToLogin = {
                authViewModel.resetAuthState()
                showLoginScreen = true // Pindah ke layar login
            }
        )
    }
}