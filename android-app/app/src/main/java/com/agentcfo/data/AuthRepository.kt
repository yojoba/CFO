package com.agentcfo.data

import com.agentcfo.auth.TokenManager
import com.agentcfo.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for authentication operations
 */
class AuthRepository(private val tokenManager: TokenManager) {
    
    private val apiService = RetrofitClient.apiService
    
    /**
     * Register a new user
     */
    suspend fun register(
        email: String,
        password: String,
        fullName: String?
    ): Result<TokenResponse> = withContext(Dispatchers.IO) {
        try {
            val request = RegisterRequest(email, password, fullName)
            val response = apiService.register(request)
            
            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                
                // Save token and user info
                tokenManager.saveToken(
                    token = tokenResponse.accessToken,
                    userId = tokenResponse.user.id,
                    email = tokenResponse.user.email,
                    name = tokenResponse.user.fullName
                )
                
                Result.success(tokenResponse)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Registration failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Login user
     */
    suspend fun login(
        email: String,
        password: String
    ): Result<TokenResponse> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(email, password)
            val response = apiService.login(request)
            
            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                
                // Save token and user info
                tokenManager.saveToken(
                    token = tokenResponse.accessToken,
                    userId = tokenResponse.user.id,
                    email = tokenResponse.user.email,
                    name = tokenResponse.user.fullName
                )
                
                Result.success(tokenResponse)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Login failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current user info
     */
    suspend fun getCurrentUser(): Result<UserResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCurrentUser()
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to fetch user"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Logout user (clear local storage)
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        tokenManager.clearToken()
    }
    
    /**
     * Check if user is logged in
     */
    suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        tokenManager.isLoggedIn()
    }
}

