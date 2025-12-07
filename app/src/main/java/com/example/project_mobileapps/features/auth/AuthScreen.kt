// File: features/auth/AuthScreen.kt
package com.example.project_mobileapps.features.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.project_mobileapps.R
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.ui.themes.TextSecondary
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

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
    var rememberMe by remember { mutableStateOf(false) }

    val context = LocalContext.current
    // Activity tidak lagi wajib untuk Email Auth, tapi Google Sign In butuh context

    // --- SETUP GOOGLE SIGN IN ---
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { token ->
                    authViewModel.handleGoogleSignIn(token)
                }
            } catch (e: ApiException) {
                Log.e("AuthScreen", "Google Sign In Failed", e)
            }
        }
    }

    // --- LOGIC NAVIGASI SUKSES (UPDATED) ---
    // Cukup pantau loggedInUser. Jika tidak null, berarti Login/Register/Verifikasi Email sukses.
    LaunchedEffect(authState.loggedInUser) {
        if (authState.loggedInUser != null) {
            onAuthSuccess(authState.loggedInUser!!)
        }
    }

    // --- LOGIC TAMPILAN ---
    if (authState.showVerificationDialog) {
        // Tampilkan Layar Tunggu Verifikasi Email (Magic Link)
        VerificationWaitingScreen(
            email = authState.registerEmail,
            isSuccess = authState.isVerifiedSuccess, // Trigger animasi centang hijau
            onAnimationFinished = {
                // Saat animasi selesai, cek user lagi untuk navigasi
                if (authState.loggedInUser != null) {
                    onAuthSuccess(authState.loggedInUser!!)
                }
            },
            onCancel = {
                authViewModel.stopVerificationCheck()
            }
        )
    } else {
        // TAMPILAN FORM LOGIN / REGISTER
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
                            phone = authState.registerPhone, onPhoneChange = authViewModel::onRegisterPhoneChange,
                            nameError = authState.registerNameError,
                            emailError = authState.registerEmailError,
                            passwordError = authState.registerPasswordError,
                            phoneError = authState.registerPhoneError
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

                // --- TOMBOL UTAMA ---
                Button(
                    onClick = {
                        if (selectedTab == AuthTab.LOGIN) {
                            authViewModel.loginUser()
                        } else {
                            // PERBAIKAN UTAMA DI SINI:
                            // Panggil registerUser() untuk Verifikasi Email (Gratis)
                            // Jangan panggil registerAndSendOtp() lagi (karena itu SMS berbayar)
                            authViewModel.registerUser()
                        }
                    },
                    enabled = !authState.isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (authState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text(if (selectedTab == AuthTab.LOGIN) "Login" else "Daftar", style = MaterialTheme.typography.labelLarge)
                }

                authState.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp), textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(24.dp))
                DividerWithText("Atau lanjutkan dengan")
                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SocialLoginButton(
                        text = "Google",
                        iconRes = R.drawable.google,
                        onClick = { googleLauncher.launch(googleSignInClient.signInIntent) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// --- Helper Composables ---

@Composable
private fun AuthHeader(selectedTab: AuthTab) {
    val title = if (selectedTab == AuthTab.LOGIN) "Login ke Akun Anda" else "Buat Akun untuk Memulai"
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun AuthTabSelector(selectedTab: AuthTab, onTabSelected: (AuthTab) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val loginButtonColors = if (selectedTab == AuthTab.LOGIN)
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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

@Composable
private fun RegisterFields(
    name: String, onNameChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    nameError: String?, emailError: String?, passwordError: String?, phoneError: String?
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
            value = phone, onValueChange = onPhoneChange,
            label = { Text("Nomor HP") },
            leadingIcon = { Icon(Icons.Outlined.Phone, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = phoneError != null,
            supportingText = { if (phoneError != null) Text(phoneError, color = MaterialTheme.colorScheme.error) }
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