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

/**
 * Composable utama untuk layar Autentikasi (Login dan Register).
 * Layar ini bersifat stateful, mengamati [AuthState] dari [authViewModel].
 *
 * @param authViewModel ViewModel yang menyediakan state dan logika untuk autentikasi.
 * @param startScreen Menentukan tab mana ("login" atau "register") yang aktif saat pertama kali dibuka.
 * @param onAuthSuccess Callback yang dipanggil saat login/register berhasil, membawa data [User].
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    startScreen: String,
    onAuthSuccess: (User) -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    var selectedTab by remember { mutableStateOf(if (startScreen == "login") AuthTab.LOGIN else AuthTab.REGISTER) }
    var rememberMe by remember { mutableStateOf(false) }
    /**
     * [LaunchedEffect] ini mengamati perubahan pada [authState.loggedInUser].
     * Jika `loggedInUser` tidak lagi null (artinya login berhasil),
     * ia akan memanggil [onAuthSuccess] untuk memicu navigasi.
     */
    LaunchedEffect(authState.loggedInUser) {
        authState.loggedInUser?.let(onAuthSuccess)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.5f))
            AuthHeader(selectedTab)
            Spacer(modifier = Modifier.height(32.dp))
            AuthTabSelector(selectedTab) { newTab ->
                selectedTab = newTab
                authViewModel.resetAuthState()
            }
            Spacer(modifier = Modifier.height(32.dp))
            /**
             * [AnimatedContent] untuk menganimasikan perpindahan
             * antara form Login dan form Register.
             */
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn(tween(200, 90)) togetherWith fadeOut(tween(90)) },
                label = "Auth Form Animation"
            ) { tab ->
                if (tab == AuthTab.LOGIN) {
                    LoginFields(
                        email = authState.loginEmail, onEmailChange = authViewModel::onLoginEmailChange,
                        password = authState.loginPassword, onPasswordChange = authViewModel::onLoginPasswordChange,
                        emailError = authState.loginEmailError, passwordError = authState.loginPasswordError
                    )
                } else {
                    RegisterFields(
                        name = authState.registerName, onNameChange = authViewModel::onRegisterNameChange,
                        email = authState.registerEmail, onEmailChange = authViewModel::onRegisterEmailChange,
                        password = authState.registerPassword, onPasswordChange = authViewModel::onRegisterPasswordChange,
                        nameError = authState.registerNameError, emailError = authState.registerEmailError, passwordError = authState.registerPasswordError
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
                        Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                        Text("Ingat Saya", style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { /* TODO */ }) {
                        Text("Lupa Password?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedTab == AuthTab.LOGIN) authViewModel.loginUser()
                    else authViewModel.registerUser()
                },
                enabled = !authState.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (authState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text(if (selectedTab == AuthTab.LOGIN) "Login" else "Daftar", style = MaterialTheme.typography.labelLarge)
            }

            // Menampilkan error umum dari server (misal: "Email sudah terdaftar")
            authState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp), textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { authViewModel.loginUser("pasien@gmail.com", "password") },
                enabled = !authState.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Login Cepat (Pasien)", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(24.dp))
            DividerWithText("Atau lanjutkan dengan")
            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SocialLoginButton(text = "Google", iconRes = R.drawable.google, onClick = { /*TODO*/ }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
/**
 * Composable helper (private) untuk menampilkan judul header.
 */
@Composable
private fun AuthHeader(selectedTab: AuthTab) {
    val title = if (selectedTab == AuthTab.LOGIN) "Login ke Akun Anda" else "Buat Akun untuk Memulai"
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium, // Menggunakan font Poppins Bold
        textAlign = TextAlign.Center
    )
}
/**
 * Composable helper (private) untuk menampilkan tombol toggle Login/Daftar.
 */
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
/**
 * Composable helper (private) untuk field input form Login.
 */
@Composable
private fun LoginFields(
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    emailError: String?, passwordError: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = email, onValueChange = onEmailChange,
            label = { Text("Alamat Email") },
            leadingIcon = { Icon(Icons.Outlined.Email, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp),
            isError = emailError != null,
            supportingText = { if (emailError != null) Text(emailError, color = MaterialTheme.colorScheme.error) }
        )
        OutlinedTextField(
            value = password, onValueChange = onPasswordChange,
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp),
            isError = passwordError != null,
            supportingText = { if (passwordError != null) Text(passwordError, color = MaterialTheme.colorScheme.error) }
        )
    }
}
/**
 * Composable helper (private) untuk field input form Register.
 */
@Composable
private fun RegisterFields(
    name: String, onNameChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    nameError: String?, emailError: String?, passwordError: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name, onValueChange = onNameChange,
            label = { Text("Nama Lengkap") },
            leadingIcon = { Icon(Icons.Outlined.Person, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp),
            isError = nameError != null,
            supportingText = { if (nameError != null) Text(nameError, color = MaterialTheme.colorScheme.error) }
        )
        OutlinedTextField(
            value = email, onValueChange = onEmailChange,
            label = { Text("Alamat Email") },
            leadingIcon = { Icon(Icons.Outlined.Email, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp),
            isError = emailError != null,
            supportingText = { if (emailError != null) Text(emailError, color = MaterialTheme.colorScheme.error) }
        )
        OutlinedTextField(
            value = password, onValueChange = onPasswordChange,
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp),
            isError = passwordError != null,
            supportingText = { if (passwordError != null) Text(passwordError, color = MaterialTheme.colorScheme.error) }
        )
    }
}
/**
 * Composable helper (private) untuk divider "Atau lanjutkan dengan".
 */
@Composable
private fun DividerWithText(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Divider(modifier = Modifier.weight(1f))
        Text(text, style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.padding(horizontal = 16.dp))
        Divider(modifier = Modifier.weight(1f))
    }
}
/**
 * Composable helper (private) untuk tombol Social Login (misal: Google).
 */
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