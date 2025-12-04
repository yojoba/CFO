package com.example.protfoliomanagerv1.auth

import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthManager {
    private const val TAG = "BiometricAuthManager"

    /**
     * Check if biometric authentication is available on this device
     */
    fun canAuthenticate(activity: FragmentActivity): BiometricCapability {
        val biometricManager = BiometricManager.from(activity)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(TAG, "Biometric authentication is available")
                BiometricCapability.AVAILABLE
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.w(TAG, "No biometric hardware available")
                BiometricCapability.NO_HARDWARE
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.w(TAG, "Biometric hardware unavailable")
                BiometricCapability.HARDWARE_UNAVAILABLE
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.w(TAG, "No biometric credentials enrolled")
                BiometricCapability.NONE_ENROLLED
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                Log.w(TAG, "Security update required")
                BiometricCapability.SECURITY_UPDATE_REQUIRED
            }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                Log.w(TAG, "Biometric authentication unsupported")
                BiometricCapability.UNSUPPORTED
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                Log.w(TAG, "Biometric status unknown")
                BiometricCapability.UNKNOWN
            }
            else -> {
                Log.w(TAG, "Unknown biometric status")
                BiometricCapability.UNKNOWN
            }
        }
    }

    /**
     * Authenticate user for app entry
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Portfolio Manager",
        subtitle: String = "Authenticate to access your portfolio",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(TAG, "Authentication error: $errString (code: $errorCode)")
                    
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                            // User clicked negative button (Cancel)
                            onError("Authentication cancelled")
                        }
                        BiometricPrompt.ERROR_USER_CANCELED -> {
                            onError("Authentication cancelled by user")
                        }
                        BiometricPrompt.ERROR_LOCKOUT -> {
                            onError("Too many attempts. Please try again later.")
                        }
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            onError("Biometric authentication locked. Please use device PIN.")
                        }
                        else -> {
                            onError(errString.toString())
                        }
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d(TAG, "Authentication succeeded")
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w(TAG, "Authentication failed - biometric not recognized")
                    // Don't call onError here - this is just a single failed attempt
                    // The prompt will stay open for retry
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Authenticate user for sell confirmation
     */
    fun authenticateForSell(
        activity: FragmentActivity,
        asset: String,
        quantity: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        authenticate(
            activity = activity,
            title = "Confirm Sale",
            subtitle = "Authenticate to sell $quantity $asset",
            onSuccess = onSuccess,
            onError = onError
        )
    }
}

enum class BiometricCapability {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    SECURITY_UPDATE_REQUIRED,
    UNSUPPORTED,
    UNKNOWN
}

