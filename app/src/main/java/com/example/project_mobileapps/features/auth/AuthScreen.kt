// File: features/auth/AuthScreen.kt
package com.example.project_mobileapps.features.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_mobileapps.R
import com.example.project_mobileapps.data.model.User
import com.example.project_mobileapps.ui.components.PrimaryButton
import com.example.project_mobileapps.ui.components.PrimaryTextField
import com.example.project_mobileapps.ui.themes.TextSecondary
import com.example.project_mobileapps.utils.PasswordStrength
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private enum class AuthTab { LOGIN, REGISTER }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    startScreen: String = "login", // Default value added
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

    // --- DIALOG LUPA PASSWORD ---
    if (showForgotPassDialog) {
        ForgotPasswordDialog(
            onDismiss = { showForgotPassDialog = false },
            onSendClick = { email ->
                authViewModel.resetPassword(email) { showForgotPassDialog = false }
            }
        )
    }

    // --- UI UTAMA ---
    if (authState.showVerificationDialog) {
        // Tampilkan Layar Tunggu Verifikasi Email (Magic Link)
        VerificationWaitingScreen(
            email = authState.registerEmail,
            isSuccess = authState.isVerifiedSuccess,
            onAnimationFinished = {
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
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(0.2f)) // Spacer dinamis

                // 1. HEADER
                AuthHeader(selectedTab)

                Spacer(modifier = Modifier.height(32.dp))

                // 2. TAB SELECTOR (Login / Register)
                AuthTabSelector(selectedTab) { newTab ->
                    selectedTab = newTab
                    authViewModel.resetAuthState()
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 3. FORM CONTENT (ANIMATED)
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    label = "Auth Form Animation"
                ) { tab ->
                    if (tab == AuthTab.LOGIN) {
                        LoginFields(
                            email = authState.loginEmail,
                            onEmailChange = authViewModel::onLoginEmailChange,
                            password = authState.loginPassword,
                            onPasswordChange = authViewModel::onLoginPasswordChange,
                            emailError = authState.loginEmailError,
                            passwordError = authState.loginPasswordError
                        )
                    } else {
                        // Menggunakan RegisterFieldsSimple yang Anda minta (tanpa HP, karena HP di Complete Profile)
                        RegisterFieldsSimple(
                            name = authState.registerName,
                            onNameChange = authViewModel::onRegisterNameChange,
                            email = authState.registerEmail,
                            onEmailChange = authViewModel::onRegisterEmailChange,
                            password = authState.registerPassword,
                            onPasswordChange = authViewModel::onRegisterPasswordChange,
                            confirmPassword = authState.confirmPassword,
                            onConfirmPasswordChange = authViewModel::onConfirmPasswordChange,
                            passwordStrength = authState.passwordStrength,
                            isPrivacyAccepted = authState.isPrivacyAccepted,
                            onPrivacyChange = authViewModel::onPrivacyChange,
                            nameError = authState.registerNameError,
                            emailError = authState.registerEmailError,
                            passwordError = authState.registerPasswordError,
                            confirmPasswordError = authState.confirmPasswordError,
                            privacyError = authState.privacyError
                        )
                    }
                }

                // 4. LUPA PASSWORD (Hanya di Tab Login)
                if (selectedTab == AuthTab.LOGIN) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(onClick = { showForgotPassDialog = true }) {
                            Text(
                                "Lupa Password?",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. TOMBOL UTAMA (LOGIN/DAFTAR)
                PrimaryButton(
                    text = if (selectedTab == AuthTab.LOGIN) "Masuk" else "Daftar Sekarang",
                    onClick = {
                        if (selectedTab == AuthTab.LOGIN) authViewModel.loginUser()
                        else authViewModel.registerInitial()
                    },
                    isLoading = authState.isLoading
                )

                // Error Global Message
                authState.error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 6. DIVIDER
                DividerWithText("Atau lanjutkan dengan")

                Spacer(modifier = Modifier.height(24.dp))

                // 7. SOCIAL LOGIN (GOOGLE)
                SocialLoginButton(
                    text = "Google",
                    // Pastikan Anda punya icon R.drawable.ic_google atau ganti dengan R.drawable.ic_launcher_foreground sementara
                    iconRes = R.drawable.google
                    ,
                    onClick = {
                        // Sign Out dulu agar prompt akun muncul kembali
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleLauncher.launch(googleSignInClient.signInIntent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))
                Spacer(modifier = Modifier.weight(0.5f))
            }
        }
    }
}

// ===========================================================================
// HELPER COMPOSABLES (Semua Composable yang Anda minta ada di sini)
// ===========================================================================

@Composable
private fun AuthHeader(selectedTab: AuthTab) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (selectedTab == AuthTab.LOGIN) "Selamat Datang!" else "Buat Akun Baru",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (selectedTab == AuthTab.LOGIN) "Silakan masuk untuk melanjutkan." else "Bergabunglah untuk layanan kesehatan.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AuthTabSelector(selectedTab: AuthTab, onTabSelected: (AuthTab) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tombol Tab Login
            Button(
                onClick = { onTabSelected(AuthTab.LOGIN) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == AuthTab.LOGIN) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (selectedTab == AuthTab.LOGIN) MaterialTheme.colorScheme.onPrimary else TextSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                elevation = if (selectedTab == AuthTab.LOGIN) ButtonDefaults.buttonElevation(2.dp) else ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text("Masuk", style = MaterialTheme.typography.labelLarge)
            }

            // Tombol Tab Daftar
            Button(
                onClick = { onTabSelected(AuthTab.REGISTER) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == AuthTab.REGISTER) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (selectedTab == AuthTab.REGISTER) MaterialTheme.colorScheme.onPrimary else TextSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                elevation = if (selectedTab == AuthTab.REGISTER) ButtonDefaults.buttonElevation(2.dp) else ButtonDefaults.buttonElevation(0.dp)
            ) {
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )
        // Menggunakan PrimaryTextField dengan mode Password
        PrimaryTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Kata Sandi",
            leadingIcon = Icons.Outlined.Lock,
            isPassword = true,
            errorMessage = passwordError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
        )
    }
}

@Composable
private fun RegisterFieldsSimple(
    name: String, onNameChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    confirmPassword: String, onConfirmPasswordChange: (String) -> Unit,
    passwordStrength: PasswordStrength,
    isPrivacyAccepted: Boolean, onPrivacyChange: (Boolean) -> Unit,
    nameError: String?, emailError: String?, passwordError: String?, confirmPasswordError: String?, privacyError: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PrimaryTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Nama Lengkap",
            leadingIcon = Icons.Outlined.Person,
            errorMessage = nameError,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
        )
        PrimaryTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email",
            leadingIcon = Icons.Outlined.Email,
            errorMessage = emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PrimaryTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Buat Password",
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                errorMessage = passwordError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
            )
            if (password.isNotEmpty()) PasswordStrengthBar(strength = passwordStrength)
        }

        PrimaryTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Ulangi Password",
            leadingIcon = Icons.Outlined.LockClock,
            isPassword = true,
            errorMessage = confirmPasswordError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
        )

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPrivacyChange(!isPrivacyAccepted) }
            ) {
                Checkbox(
                    checked = isPrivacyAccepted,
                    onCheckedChange = onPrivacyChange,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "Saya setuju dengan Syarat & Ketentuan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SocialLoginButton(text: String, iconRes: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

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
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(strength.color),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

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