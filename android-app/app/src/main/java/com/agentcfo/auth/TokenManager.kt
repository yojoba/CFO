package com.agentcfo.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agentcfo.network.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Manages JWT authentication tokens using DataStore
 */
class TokenManager(private val context: Context) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "auth_prefs"
        )
        
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
    }
    
    /**
     * Save authentication token and user info
     */
    suspend fun saveToken(token: String, userId: Int, email: String, name: String?) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USER_ID_KEY] = userId.toString()
            preferences[USER_EMAIL_KEY] = email
            name?.let { preferences[USER_NAME_KEY] = it }
        }
        
        // Update Retrofit client with new token
        RetrofitClient.setAuthToken(token)
    }
    
    /**
     * Get current authentication token
     */
    suspend fun getToken(): String? {
        val preferences = context.dataStore.data.first()
        return preferences[TOKEN_KEY]
    }
    
    /**
     * Get token as Flow for reactive updates
     */
    fun getTokenFlow(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }
    }
    
    /**
     * Get user ID
     */
    suspend fun getUserId(): Int? {
        val preferences = context.dataStore.data.first()
        return preferences[USER_ID_KEY]?.toIntOrNull()
    }
    
    /**
     * Get user email
     */
    suspend fun getUserEmail(): String? {
        val preferences = context.dataStore.data.first()
        return preferences[USER_EMAIL_KEY]
    }
    
    /**
     * Get user name
     */
    suspend fun getUserName(): String? {
        val preferences = context.dataStore.data.first()
        return preferences[USER_NAME_KEY]
    }
    
    /**
     * Check if user is logged in
     */
    suspend fun isLoggedIn(): Boolean {
        return getToken() != null
    }
    
    /**
     * Clear all authentication data (logout)
     */
    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
        
        // Clear token from Retrofit client
        RetrofitClient.clearAuth()
    }
    
    /**
     * Initialize token in RetrofitClient if exists
     */
    suspend fun initializeToken() {
        val token = getToken()
        RetrofitClient.setAuthToken(token)
    }
}

