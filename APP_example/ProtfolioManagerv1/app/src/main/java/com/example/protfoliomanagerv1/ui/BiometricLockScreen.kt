package com.example.protfoliomanagerv1.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.protfoliomanagerv1.auth.BiometricAuthManager
import com.example.protfoliomanagerv1.auth.BiometricCapability
import com.example.protfoliomanagerv1.ui.theme.*

@Composable
fun BiometricLockScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }
    
    // Pulse animation for fingerprint icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Check biometric capability
    val biometricCapability = remember {
        activity?.let { BiometricAuthManager.canAuthenticate(it) } ?: BiometricCapability.UNKNOWN
    }

    // Auto-trigger authentication on first composition
    LaunchedEffect(Unit) {
        if (activity != null && biometricCapability == BiometricCapability.AVAILABLE) {
            isAuthenticating = true
            BiometricAuthManager.authenticate(
                activity = activity,
                onSuccess = {
                    isAuthenticating = false
                    onAuthSuccess()
                },
                onError = { error ->
                    isAuthenticating = false
                    errorMessage = error
                }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CryptoDarkBackground,
                        CryptoDarkSurface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Lock icon with gradient background
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .                    background(
                        brush = Brush.radialGradient(
                            colors = listOf(GradientBlue.copy(alpha = 0.3f), Color.Transparent)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Biometric Lock",
                    modifier = Modifier
                        .size(80.dp)
                        .scale(if (isAuthenticating) scale else 1f),
                    tint = NeonGreen
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "Portfolio Manager",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = when (biometricCapability) {
                    BiometricCapability.AVAILABLE -> "Unlock with fingerprint or PIN"
                    BiometricCapability.NONE_ENROLLED -> "Set up biometric authentication in Settings"
                    BiometricCapability.NO_HARDWARE -> "Device PIN required"
                    else -> "Authentication required"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Error message
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = ElectricRed.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Authenticate button
            if (activity != null && !isAuthenticating) {
                Button(
                    onClick = {
                        errorMessage = null
                        isAuthenticating = true
                        BiometricAuthManager.authenticate(
                            activity = activity,
                            onSuccess = {
                                isAuthenticating = false
                                onAuthSuccess()
                            },
                            onError = { error ->
                                isAuthenticating = false
                                errorMessage = error
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GradientBlue,
                        contentColor = TextPrimary
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tap to Authenticate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (isAuthenticating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = NeonGreen,
                    strokeWidth = 4.dp
                )
            }

            // Help text
            if (biometricCapability == BiometricCapability.NONE_ENROLLED) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Go to Settings > Security > Biometric to enroll your fingerprint",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

