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
import com.example.project_mobileapps.ui.components.PasswordTextField
import com.example.project_mobileapps.ui.components.PrimaryTextField
import com.example.project_mobileapps.ui.themes.TextSecondary
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.example.project_mobileapps.utils.PasswordStrength

private enum class AuthTab { LOGIN, REGISTER }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    startScreen: String,
    onAuthSuccess: (User) -> Unit,
    onNavigateToCompleteProfile: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    var selectedTab by remember { mutableStateOf(if (startScreen == "login") AuthTab.LOGIN else AuthTab.REGISTER) }
    val context = LocalContext.current
    var showForgotPassDialog by remember { mutableStateOf(false) }

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

    // --- LOGIC NAVIGASI UTAMA ---
    LaunchedEffect(authState.loggedInUser, authState.isProfileIncomplete) {
        // Prioritas 1: Jika User Login & Lengkap -> Masuk Home
        if (authState.loggedInUser != null) {
            onAuthSuccess(authState.loggedInUser!!)
        }
        // Prioritas 2: Jika User Login tapi Belum Lengkap -> Masuk Halaman Lengkapi Data
        else if (authState.isProfileIncomplete) {
            onNavigateToCompleteProfile()
        }
    }

    if (showForgotPassDialog) {
        ForgotPasswordDialog(
            onDismiss = { showForgotPassDialog = false },
            onSendClick = { email ->
                authViewModel.resetPassword(email) { showForgotPassDialog = false }
            }
        )
    }

    // --- LOGIC TAMPILAN ---
    if (authState.showVerificationDialog) {
        // Tampilkan Layar Tunggu Verifikasi Email (Magic Link)
        VerificationWaitingScreen(
            email = authState.registerEmail,
            isSuccess = authState.isVerifiedSuccess, // Trigger animasi centang hijau
            onAnimationFinished = {
                // Saat animasi selesai, cek mau kemana
                if (authState.isProfileIncomplete) {
                    onNavigateToCompleteProfile()
                } else if (authState.loggedInUser != null) {
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
                        RegisterFieldsSimple(
                            name = authState.registerName, onNameChange = authViewModel::onRegisterNameChange,
                            email = authState.registerEmail, onEmailChange = authViewModel::onRegisterEmailChange,
                            password = authState.registerPassword, onPasswordChange = authViewModel::onRegisterPasswordChange,
                            confirmPassword = authState.confirmPassword, onConfirmPasswordChange = authViewModel::onConfirmPasswordChange,
                            passwordStrength = authState.passwordStrength,
                            isPrivacyAccepted = authState.isPrivacyAccepted, onPrivacyChange = authViewModel::onPrivacyChange,
                            nameError = authState.registerNameError, emailError = authState.registerEmailError,
                            passwordError = authState.registerPasswordError, confirmPasswordError = authState.confirmPasswordError,
                            privacyError = authState.privacyError
                        )
                    }
                }

                if (selectedTab == AuthTab.LOGIN) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showForgotPassDialog = true }) {
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
                        if (selectedTab == AuthTab.LOGIN) authViewModel.loginUser()
                        else authViewModel.registerInitial() // Gunakan registerInitial (Auth Only)
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
                        iconRes = R.drawable.google, // Pastikan icon ada
                        onClick = {
                            // [PERBAIKAN]: Sign Out dulu dari Google Client agar dialog pemilihan akun muncul lagi
                            googleSignInClient.signOut().addOnCompleteListener {
                                // Setelah logout lokal sukses, baru luncurkan intent login
                                googleLauncher.launch(googleSignInClient.signInIntent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// --- Helper Composables ---

// (Sertakan juga fungsi-fungsi helper lama: AuthHeader, AuthTabSelector, LoginFields, DividerWithText, SocialLoginButton, PasswordStrengthBar)
// Copy paste fungsi-fungsi helper tersebut dari file AuthScreen.kt sebelumnya.
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PrimaryTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Alamat Email",
            leadingIcon = Icons.Outlined.Email,
            errorMessage = emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        PasswordTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            leadingIcon = Icons.Outlined.Lock,
            errorMessage = passwordError
        )
    }
}

@Composable
private fun RegisterFieldsSimple(
    name: String, onNameChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    confirmPassword: String, onConfirmPasswordChange: (String) -> Unit,
    passwordStrength: com.example.project_mobileapps.utils.PasswordStrength,
    isPrivacyAccepted: Boolean, onPrivacyChange: (Boolean) -> Unit,
    nameError: String?, emailError: String?, passwordError: String?, confirmPasswordError: String?, privacyError: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PrimaryTextField(value = name, onValueChange = onNameChange, label = "Username", leadingIcon = Icons.Outlined.Person, errorMessage = nameError)
        PrimaryTextField(value = email, onValueChange = onEmailChange, label = "Email", leadingIcon = Icons.Outlined.Email, errorMessage = emailError, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PasswordTextField(value = password, onValueChange = onPasswordChange, label = "Password", leadingIcon = Icons.Outlined.Lock, errorMessage = passwordError)
            if (password.isNotEmpty()) PasswordStrengthBar(strength = passwordStrength)
        }

        PasswordTextField(value = confirmPassword, onValueChange = onConfirmPasswordChange, label = "Ulangi Password", leadingIcon = Icons.Outlined.LockClock, errorMessage = confirmPasswordError)

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onPrivacyChange(!isPrivacyAccepted) }
            ) {
                Checkbox(checked = isPrivacyAccepted, onCheckedChange = onPrivacyChange, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary))
                Text("Saya setuju dengan Syarat & Ketentuan.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
            }
            if (privacyError != null) Text(text = privacyError, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
private fun RegisterFields(
    name: String, onNameChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,

    nameError: String?, emailError: String?, passwordError: String?,
    phoneError: String?, confirmPassword: String, privacyError: String?,

    onConfirmPasswordChange: (String) -> Unit,
    confirmPasswordError: String?,
    passwordStrength: PasswordStrength, // Parameter Baru
    isPrivacyAccepted: Boolean,         // Parameter Baru
    onPrivacyChange: (Boolean) -> Unit, // Parameter Baru
    onDone: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PrimaryTextField(
            value = email, onValueChange = onEmailChange, label = "Alamat Email",
            leadingIcon = Icons.Outlined.Email, errorMessage = emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PasswordTextField(
                value = password, onValueChange = onPasswordChange, label = "Buat Password",
                leadingIcon = Icons.Outlined.Lock, errorMessage = passwordError
            )
            // INDIKATOR KEKUATAN PASSWORD
            if (password.isNotEmpty()) {
                PasswordStrengthBar(strength = passwordStrength)
            }
        }

        // --- KONFIRMASI PASSWORD ---
        PasswordTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Ulangi Password",
            leadingIcon = Icons.Outlined.LockClock, // Icon beda dikit biar variasi
            errorMessage = confirmPasswordError
        )

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPrivacyChange(!isPrivacyAccepted) } // Klik teks juga bisa centang
            ) {
                Checkbox(
                    checked = isPrivacyAccepted,
                    onCheckedChange = onPrivacyChange,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "Saya setuju dengan Syarat & Ketentuan serta Kebijakan Privasi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            // Tampilkan error jika belum dicentang saat klik Daftar
            if (privacyError != null) {
                Text(
                    text = privacyError,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
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

// --- KOMPONEN BARU: PasswordStrengthBar ---
@Composable
fun PasswordStrengthBar(strength: PasswordStrength) {
    val animatedProgress by animateFloatAsState(targetValue = strength.progress, label = "strength")

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kekuatan: ${strength.label}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(strength.color)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Progress Bar Kustom
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(strength.color),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // Tips kecil (Opsional)
        if (strength == PasswordStrength.WEAK) {
            Text(
                text = "Gunakan kombinasi huruf besar, angka, dan simbol.",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}