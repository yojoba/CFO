package com.example.protfoliomanagerv1.auth

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

/**
 * Singleton to manage authentication state for the app session.
 * Authentication persists until the app is killed (onDestroy).
 */
object AuthenticationState {
    private const val TAG = "AuthenticationState"
    
    private val _isAuthenticated = mutableStateOf(false)
    val isAuthenticated: State<Boolean> = _isAuthenticated

    /**
     * Set the authentication status
     */
    fun setAuthenticated(authenticated: Boolean) {
        Log.d(TAG, "Authentication state changed: $authenticated")
        _isAuthenticated.value = authenticated
    }

    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return _isAuthenticated.value
    }

    /**
     * Reset authentication state (called when app is destroyed)
     */
    fun reset() {
        Log.d(TAG, "Resetting authentication state")
        _isAuthenticated.value = false
    }
}

