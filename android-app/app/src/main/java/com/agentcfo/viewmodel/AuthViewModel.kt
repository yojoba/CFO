package com.agentcfo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agentcfo.auth.TokenManager
import com.agentcfo.data.AuthRepository
import com.agentcfo.network.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication operations
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val tokenManager = TokenManager(application)
    private val repository = AuthRepository(tokenManager)
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<UserResponse?>(null)
    val currentUser: StateFlow<UserResponse?> = _currentUser.asStateFlow()
    
    init {
        // Initialize token in RetrofitClient
        viewModelScope.launch {
            tokenManager.initializeToken()
            
            // Check if user is logged in and fetch current user
            if (repository.isLoggedIn()) {
                fetchCurrentUser()
            }
        }
    }
    
    /**
     * Register a new user
     */
    fun register(email: String, password: String, fullName: String?) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = repository.register(email, password, fullName)
            
            result.fold(
                onSuccess = { tokenResponse ->
                    _currentUser.value = tokenResponse.user
                    _authState.value = AuthState.Authenticated(tokenResponse.user)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(
                        error.message ?: "Registration failed"
                    )
                }
            )
        }
    }
    
    /**
     * Login user
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = repository.login(email, password)
            
            result.fold(
                onSuccess = { tokenResponse ->
                    _currentUser.value = tokenResponse.user
                    _authState.value = AuthState.Authenticated(tokenResponse.user)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(
                        error.message ?: "Login failed"
                    )
                }
            )
        }
    }
    
    /**
     * Fetch current user information
     */
    fun fetchCurrentUser() {
        viewModelScope.launch {
            val result = repository.getCurrentUser()
            
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                },
                onFailure = { error ->
                    // Token might be expired, logout
                    logout()
                }
            )
        }
    }
    
    /**
     * Logout user
     */
    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    /**
     * Reset auth state to initial
     */
    fun resetAuthState() {
        _authState.value = AuthState.Initial
    }
    
    /**
     * Check if user is logged in
     */
    suspend fun isLoggedIn(): Boolean {
        return repository.isLoggedIn()
    }
}

/**
 * Authentication states
 */
sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: UserResponse) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

