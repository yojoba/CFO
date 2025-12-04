package com.example.protfoliomanagerv1.data

import android.util.Log
import com.example.protfoliomanagerv1.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for exchange keys operations
 */
class ExchangeKeysRepository(private val token: String) {
    
    companion object {
        private const val TAG = "ExchangeKeysRepository"
    }
    
    private val bearerToken = "Bearer $token"
    
    suspend fun getExchangeKeys(): Result<List<ExchangeKey>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching exchange keys")
            val response = AuthApi.keysService.getExchangeKeys(bearerToken)
            Log.d(TAG, "Fetched ${response.size} exchange keys")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch exchange keys: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
    
    suspend fun addExchangeKey(
        exchange: String,
        apiKey: String,
        apiSecret: String,
        label: String?
    ): Result<ExchangeKey> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Adding $exchange exchange key")
            val response = AuthApi.keysService.addExchangeKey(
                bearerToken,
                AddExchangeKeyRequest(exchange, apiKey, apiSecret, label)
            )
            Log.d(TAG, "Exchange key added successfully: ${response.id}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add exchange key: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
    
    suspend fun deleteExchangeKey(keyId: String): Result<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Deleting exchange key: $keyId")
            val response = AuthApi.keysService.deleteExchangeKey(bearerToken, keyId)
            Log.d(TAG, "Exchange key deleted successfully")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete exchange key: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
    
    suspend fun testConnection(keyId: String): Result<TestConnectionResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Testing connection for key: $keyId")
            val response = AuthApi.keysService.testConnection(bearerToken, keyId)
            Log.d(TAG, "Connection test result: ${response.success}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to test connection: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
}


