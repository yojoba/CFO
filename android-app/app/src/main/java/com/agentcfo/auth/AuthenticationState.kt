package com.agentcfo.auth

import androidx.compose.runtime.mutableStateOf

/**
 * Global authentication state for biometric lock
 * Similar to the example app
 */
object AuthenticationState {
    
    /**
     * Is user authenticated with biometric?
     */
    val isAuthenticated = mutableStateOf(false)
    
    /**
     * Set authentication state
     */
    fun setAuthenticated(authenticated: Boolean) {
        isAuthenticated.value = authenticated
    }
    
    /**
     * Reset authentication (on logout or app restart)
     */
    fun reset() {
        isAuthenticated.value = false
    }
}

