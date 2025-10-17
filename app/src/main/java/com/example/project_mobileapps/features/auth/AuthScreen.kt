package com.example.project_mobileapps.features.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.R
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.ui.themes.TextSecondary

private enum class AuthTab { LOGIN, REGISTER }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    startScreen: String,
    onAuthSuccess: (User) -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    var selectedTab by remember { mutableStateOf(if (startScreen == "login") AuthTab.LOGIN else AuthTab.REGISTER) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }

    LaunchedEffect(authState.loggedInUser) {
        authState.loggedInUser?.let(onAuthSuccess)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background // Menggunakan AppBackground (abu-abu terang)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()), // <-- TAMBAHKAN INI
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Spacer untuk mendorong konten ke tengah secara vertikal
            Spacer(modifier = Modifier.weight(0.5f))

            AuthHeader(selectedTab)
            Spacer(modifier = Modifier.height(32.dp))

            AuthTabSelector(selectedTab) { newTab ->
                selectedTab = newTab
                authViewModel.resetAuthState()
            }
            Spacer(modifier = Modifier.height(32.dp))


            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200, delayMillis = 90)) togetherWith
                            fadeOut(animationSpec = tween(90))
                }, label = "Auth Form Animation"
            ) { tab ->
                if (tab == AuthTab.LOGIN) {
                    LoginFields(
                        email = email, onEmailChange = { email = it },
                        password = password, onPasswordChange = { password = it }
                    )
                } else {
                    RegisterFields(
                        name = name, onNameChange = { name = it },
                        email = email, onEmailChange = { email = it },
                        password = password, onPasswordChange = { password = it }
                    )
                }
            }

            if (selectedTab == AuthTab.LOGIN) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text("Ingat Saya", style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { /* TODO: Implement Lupa Password Flow */ }) {
                        Text(
                            "Lupa Password?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                // Spacer pengganti agar tinggi layout konsisten
                Spacer(modifier = Modifier.height(48.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))

            val buttonText = if (selectedTab == AuthTab.LOGIN) "Login" else "Daftar"
            Button(
                onClick = {
                    if (selectedTab == AuthTab.LOGIN) authViewModel.loginUser(email, password)
                    else authViewModel.registerUser(name, email, password)
                },
                enabled = !authState.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp) // Menggunakan warna PrimaryPeriwinkle
            ) {
                if (authState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text(buttonText, style = MaterialTheme.typography.labelLarge)
            }

            // --- TAMBAHKAN TOMBOL LOGIN CEPAT DI SINI ---
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { authViewModel.loginUser("pasien@gmail.com", "password") },
                enabled = !authState.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Login Cepat (Pasien)", style = MaterialTheme.typography.labelLarge)
            }

            authState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp), textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(24.dp))
            DividerWithText("Atau lanjutkan dengan")
            Spacer(modifier = Modifier.height(24.dp))

            // Tombol Social Login
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp) // Mengatur jarak vertikal antar tombol
            ) {
                SocialLoginButton(
                    text = "Google",
                    iconRes = R.drawable.google,
                    onClick = { /*TODO*/ },
                    // Modifier.weight tidak lagi diperlukan di sini
                    modifier = Modifier.fillMaxWidth() // Buat tombol memenuhi lebar
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AuthHeader(selectedTab: AuthTab) {
    val title = if (selectedTab == AuthTab.LOGIN) "Login ke Akun Anda" else "Buat Akun untuk Memulai"
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium, // Menggunakan font Poppins Bold
        textAlign = TextAlign.Center
    )
}

@Composable
private fun AuthTabSelector(selectedTab: AuthTab, onTabSelected: (AuthTab) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Warna putih
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val loginButtonColors = if (selectedTab == AuthTab.LOGIN)
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) // Warna PrimaryPeriwinkle
            else
                ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextSecondary)

            val registerButtonColors = if (selectedTab == AuthTab.REGISTER)
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            else
                ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextSecondary)

            Button(onClick = { onTabSelected(AuthTab.LOGIN) }, colors = loginButtonColors, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text("Login", style = MaterialTheme.typography.labelLarge)
            }
            Button(onClick = { onTabSelected(AuthTab.REGISTER) }, colors = registerButtonColors, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text("Daftar", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// Menggunakan OutlinedTextField agar sesuai dengan tema terang
@Composable
private fun LoginFields(email: String, onEmailChange: (String) -> Unit, password: String, onPasswordChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = email, onValueChange = onEmailChange,
            label = { Text("Alamat Email", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Outlined.Email, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp)
        )
        OutlinedTextField(
            value = password, onValueChange = onPasswordChange,
            label = { Text("Password", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun RegisterFields(name: String, onNameChange: (String) -> Unit, email: String, onEmailChange: (String) -> Unit, password: String, onPasswordChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = name, onValueChange = onNameChange,
            label = { Text("Nama Lengkap", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Outlined.Person, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp)
        )
        OutlinedTextField(
            value = email, onValueChange = onEmailChange,
            label = { Text("Alamat Email", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Outlined.Email, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp)
        )
        OutlinedTextField(
            value = password, onValueChange = onPasswordChange,
            label = { Text("Password", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun DividerWithText(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Divider(modifier = Modifier.weight(1f))
        Text(text, style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.padding(horizontal = 16.dp))
        Divider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SocialLoginButton(text: String, iconRes: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(52.dp), shape = RoundedCornerShape(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Image(painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}