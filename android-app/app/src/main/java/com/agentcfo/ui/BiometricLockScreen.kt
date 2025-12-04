package com.agentcfo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.agentcfo.R
import com.agentcfo.auth.BiometricAuthManager
import com.agentcfo.auth.BiometricAvailability

/**
 * Biometric lock screen
 * Shows biometric prompt when user opens app
 */
@Composable
fun BiometricLockScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var biometricAvailable by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        activity?.let { fragmentActivity ->
            val biometricManager = BiometricAuthManager(fragmentActivity)
            val availability = biometricManager.isBiometricAvailable()
            biometricAvailable = availability.isAvailable()
            
            if (biometricAvailable) {
                // Automatically show biometric prompt
                biometricManager.authenticate(
                    title = "Déverrouillez AgentCFO",
                    subtitle = "Authentifiez-vous pour continuer",
                    description = "Utilisez votre empreinte digitale ou reconnaissance faciale",
                    negativeButtonText = "Annuler",
                    onSuccess = {
                        errorMessage = null
                        onAuthSuccess()
                    },
                    onError = { _, message ->
                        errorMessage = message
                    },
                    onFailed = {
                        errorMessage = "Authentification échouée"
                    }
                )
            } else {
                // Biometric not available, automatically proceed
                onAuthSuccess()
            }
        } ?: run {
            // Not a FragmentActivity, proceed anyway
            onAuthSuccess()
        }
    }
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Fingerprint icon
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Biometric",
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title
            Text(
                text = stringResource(R.string.biometric_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Subtitle
            Text(
                text = if (biometricAvailable)
                    stringResource(R.string.biometric_subtitle)
                else
                    "Biométrie non disponible",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Error message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Retry button
            if (biometricAvailable && errorMessage != null) {
                Button(
                    onClick = {
                        activity?.let { fragmentActivity ->
                            val biometricManager = BiometricAuthManager(fragmentActivity)
                            biometricManager.authenticate(
                                title = "Déverrouillez AgentCFO",
                                subtitle = "Authentifiez-vous pour continuer",
                                description = "Utilisez votre empreinte digitale ou reconnaissance faciale",
                                negativeButtonText = "Annuler",
                                onSuccess = {
                                    errorMessage = null
                                    onAuthSuccess()
                                },
                                onError = { _, message ->
                                    errorMessage = message
                                },
                                onFailed = {
                                    errorMessage = "Authentification échouée"
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Réessayer")
                }
            }
            
            // Skip button (if biometric failed multiple times)
            if (!biometricAvailable || errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onAuthSuccess) {
                    Text("Continuer sans biométrie")
                }
            }
        }
    }
}

