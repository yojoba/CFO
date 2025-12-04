package com.example.protfoliomanagerv1.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Manages JWT token storage and retrieval
 * Uses SharedPreferences for secure local storage
 */
class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "auth_prefs",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_TOKEN_EXPIRES = "token_expires"
        private const val TAG = "TokenManager"
    }
    
    /**
     * Save JWT token and user information
     */
    fun saveToken(
        token: String,
        userId: String,
        email: String,
        fullName: String?,
        expiresAt: Long
    ) {
        prefs.edit().apply {
            putString(KEY_JWT_TOKEN, token)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, fullName)
            putLong(KEY_TOKEN_EXPIRES, expiresAt)
            apply()
        }
        Log.d(TAG, "Token saved for user: $email")
    }
    
    /**
     * Get stored JWT token
     */
    fun getToken(): String? {
        return prefs.getString(KEY_JWT_TOKEN, null)
    }
    
    /**
     * Get Bearer token for Authorization header
     */
    fun getBearerToken(): String? {
        val token = getToken()
        return if (token != null) "Bearer $token" else null
    }
    
    /**
     * Get stored user ID
     */
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }
    
    /**
     * Get stored user email
     */
    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }
    
    /**
     * Get stored user name
     */
    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        val token = getToken()
        if (token == null) return false
        
        // Check if token is expired
        val expiresAt = prefs.getLong(KEY_TOKEN_EXPIRES, 0)
        val now = System.currentTimeMillis()
        
        if (expiresAt > 0 && now > expiresAt) {
            Log.d(TAG, "Token expired, clearing...")
            clearToken()
            return false
        }
        
        return true
    }
    
    /**
     * Clear all stored authentication data
     */
    fun clearToken() {
        prefs.edit().apply {
            remove(KEY_JWT_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NAME)
            remove(KEY_TOKEN_EXPIRES)
            apply()
        }
        Log.d(TAG, "Token cleared")
    }
    
    /**
     * Check if token will expire soon (within 24 hours)
     */
    fun isTokenExpiringSoon(): Boolean {
        val expiresAt = prefs.getLong(KEY_TOKEN_EXPIRES, 0)
        if (expiresAt == 0L) return false
        
        val now = System.currentTimeMillis()
        val twentyFourHours = 24 * 60 * 60 * 1000
        
        return (expiresAt - now) < twentyFourHours
    }
}


