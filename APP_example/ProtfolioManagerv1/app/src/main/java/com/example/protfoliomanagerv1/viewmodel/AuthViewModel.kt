package com.example.protfoliomanagerv1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.protfoliomanagerv1.auth.TokenManager
import com.example.protfoliomanagerv1.data.AuthRepository
import com.example.protfoliomanagerv1.network.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for authentication operations
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = AuthRepository()
    private val tokenManager = TokenManager(application)
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()
    
    init {
        // Check if user is already logged in
        checkLoginStatus()
    }
    
    /**
     * Check if user is logged in on app start
     */
    fun checkLoginStatus() {
        if (tokenManager.isLoggedIn()) {
            _authState.value = AuthState.Authenticated
            // Optionally fetch current user info
            viewModelScope.launch {
                val token = tokenManager.getToken()
                if (token != null) {
                    repository.getCurrentUser(token)
                        .onSuccess { user ->
                            _currentUser.value = user
                        }
                        .onFailure {
                            // Token might be invalid, clear it
                            tokenManager.clearToken()
                            _authState.value = AuthState.Unauthenticated
                        }
                }
            }
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    /**
     * Register a new user
     */
    fun register(email: String, password: String, fullName: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            repository.register(email, password, fullName)
                .onSuccess { response ->
                    // Save token
                    val expiresAt = parseIsoDate(response.expires_at)
                    tokenManager.saveToken(
                        token = response.token,
                        userId = response.user.id,
                        email = response.user.email,
                        fullName = response.user.full_name,
                        expiresAt = expiresAt
                    )
                    
                    _currentUser.value = response.user
                    _authState.value = AuthState.Authenticated
                }
                .onFailure { error ->
                    val errorMsg = extractErrorMessage(error)
                    _authState.value = AuthState.Error(errorMsg)
                }
        }
    }
    
    /**
     * Login with email and password
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            repository.login(email, password)
                .onSuccess { response ->
                    // Save token
                    val expiresAt = parseIsoDate(response.expires_at)
                    tokenManager.saveToken(
                        token = response.token,
                        userId = response.user.id,
                        email = response.user.email,
                        fullName = response.user.full_name,
                        expiresAt = expiresAt
                    )
                    
                    _currentUser.value = response.user
                    _authState.value = AuthState.Authenticated
                }
                .onFailure { error ->
                    val errorMsg = extractErrorMessage(error)
                    _authState.value = AuthState.Error(errorMsg)
                }
        }
    }
    
    /**
     * Logout current user
     */
    fun logout() {
        viewModelScope.launch {
            val token = tokenManager.getToken()
            if (token != null) {
                repository.logout(token)
            }
            
            // Clear local token regardless of backend response
            tokenManager.clearToken()
            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    /**
     * Reset auth state to idle
     */
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
    
    /**
     * Get stored token
     */
    fun getToken(): String? {
        return tokenManager.getToken()
    }
    
    /**
     * Get bearer token for API calls
     */
    fun getBearerToken(): String? {
        return tokenManager.getBearerToken()
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn()
    }
    
    /**
     * Change password
     */
    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val token = tokenManager.getToken()
            if (token == null) {
                _authState.value = AuthState.Error("Not logged in")
                return@launch
            }
            
            repository.changePassword(token, oldPassword, newPassword)
                .onSuccess {
                    // Password changed, need to re-login
                    tokenManager.clearToken()
                    _currentUser.value = null
                    _authState.value = AuthState.Unauthenticated
                }
                .onFailure { error ->
                    val errorMsg = extractErrorMessage(error)
                    _authState.value = AuthState.Error(errorMsg)
                }
        }
    }
    
    /**
     * Delete account
     */
    fun deleteAccount(password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val token = tokenManager.getToken()
            if (token == null) {
                _authState.value = AuthState.Error("Not logged in")
                return@launch
            }
            
            repository.deleteAccount(token, password)
                .onSuccess {
                    // Account deleted, clear token and logout
                    tokenManager.clearToken()
                    _currentUser.value = null
                    _authState.value = AuthState.Unauthenticated
                }
                .onFailure { error ->
                    val errorMsg = extractErrorMessage(error)
                    _authState.value = AuthState.Error(errorMsg)
                }
        }
    }
    
    /**
     * Parse ISO 8601 date string to timestamp
     */
    private fun parseIsoDate(dateString: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            // If parsing fails, default to 7 days from now
            System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)
        }
    }
    
    /**
     * Extract user-friendly error message from exception
     */
    private fun extractErrorMessage(error: Throwable): String {
        val message = error.localizedMessage ?: error.message ?: "Unknown error"
        
        return when {
            message.contains("already registered", ignoreCase = true) -> 
                "This email is already registered"
            message.contains("invalid email or password", ignoreCase = true) -> 
                "Invalid email or password"
            message.contains("password must", ignoreCase = true) -> 
                message
            message.contains("network", ignoreCase = true) || message.contains("connect", ignoreCase = true) -> 
                "Network error. Please check your connection"
            message.contains("timeout", ignoreCase = true) -> 
                "Request timed out. Please try again"
            message.contains("401") -> 
                "Invalid credentials"
            message.contains("400") -> 
                "Invalid request. Please check your input"
            message.contains("500") -> 
                "Server error. Please try again later"
            else -> message
        }
    }
}

/**
 * Authentication state
 */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

