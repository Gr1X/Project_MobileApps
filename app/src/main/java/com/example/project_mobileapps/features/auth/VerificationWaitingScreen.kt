package com.example.project_mobileapps.features.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_mobileapps.ui.themes.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun VerificationWaitingScreen(
    email: String,
    isSuccess: Boolean,
    onAnimationFinished: () -> Unit,
    onCancel: () -> Unit
) {
    // Jika sukses, tunggu 2 detik untuk pamer animasi centang, baru pindah
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            delay(2000)
            onAnimationFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ANIMASI PERUBAHAN ICON
            AnimatedContent(
                targetState = isSuccess,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) + scaleIn() togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "VerificationAnim"
            ) { success ->
                if (success) {
                    // TAMPILAN SUKSES (CENTANG HIJAU)
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(Color(0xFF4CAF50), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Success",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                } else {
                    // TAMPILAN MENUNGGU (LOADING PULSE / EMAIL)
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Email,
                            contentDescription = "Email",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        // Loading Indicator di sekeliling
                        CircularProgressIndicator(
                            modifier = Modifier.size(120.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // TEXT STATUS
            Text(
                text = if (isSuccess) "Verifikasi Berhasil!" else "Cek Email Anda",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isSuccess)
                    "Akun Anda telah aktif.\nMengalihkan ke beranda..."
                else
                    "Kami telah mengirim link verifikasi ke:\n$email\n\nSilakan klik link tersebut. Aplikasi akan mendeteksi secara otomatis.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            if (!isSuccess) {
                Spacer(modifier = Modifier.height(48.dp))
                TextButton(onClick = onCancel) {
                    Text("Salah email? Kembali")
                }
            }
        }
    }
}