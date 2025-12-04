package com.example.protfoliomanagerv1.data

import android.util.Log
import com.example.protfoliomanagerv1.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for authentication operations
 */
class AuthRepository {
    
    companion object {
        private const val TAG = "AuthRepository"
    }
    
    /**
     * Register a new user
     */
    suspend fun register(
        email: String,
        password: String,
        fullName: String
    ): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Registering user: $email")
            val response = AuthApi.authService.register(
                RegisterRequest(
                    email = email,
                    password = password,
                    full_name = fullName
                )
            )
            Log.d(TAG, "Registration successful: ${response.user.id}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed: ${e.localizedMessage}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Login with email and password
     */
    suspend fun login(
        email: String,
        password: String
    ): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Logging in user: $email")
            val response = AuthApi.authService.login(
                LoginRequest(
                    email = email,
                    password = password
                )
            )
            Log.d(TAG, "Login successful: ${response.user.id}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Login failed: ${e.localizedMessage}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Logout (invalidate session on backend)
     */
    suspend fun logout(token: String): Result<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Logging out user")
            val response = AuthApi.authService.logout("Bearer $token")
            Log.d(TAG, "Logout successful")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Logout failed: ${e.localizedMessage}")
            // Even if backend call fails, we'll clear local token
            Result.success(MessageResponse("Logged out locally"))
        }
    }
    
    /**
     * Get current user info
     */
    suspend fun getCurrentUser(token: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching current user info")
            val response = AuthApi.authService.getCurrentUser("Bearer $token")
            Log.d(TAG, "User info fetched: ${response.email}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user info: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
    
    /**
     * Update user profile
     */
    suspend fun updateProfile(
        token: String,
        fullName: String?
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating user profile")
            val response = AuthApi.authService.updateProfile("Bearer $token", fullName)
            Log.d(TAG, "Profile updated successfully")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Profile update failed: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
    
    /**
     * Change password
     */
    suspend fun changePassword(
        token: String,
        oldPassword: String,
        newPassword: String
    ): Result<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Changing password")
            val response = AuthApi.authService.changePassword(
                "Bearer $token",
                oldPassword,
                newPassword
            )
            Log.d(TAG, "Password changed successfully")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Password change failed: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
    
    /**
     * Delete account
     */
    suspend fun deleteAccount(
        token: String,
        password: String
    ): Result<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Deleting account")
            val response = AuthApi.authService.deleteAccount("Bearer $token", password)
            Log.d(TAG, "Account deleted successfully")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Account deletion failed: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
}


