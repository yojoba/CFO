package com.example.protfoliomanagerv1.data

import android.util.Log
import com.example.protfoliomanagerv1.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for external assets operations
 */
class ExternalAssetsRepository(private val token: String) {
    
    companion object {
        private const val TAG = "ExternalAssetsRepository"
    }
    
    private val bearerToken = "Bearer $token"
    
    suspend fun getExternalAssets(): Result<List<ExternalAsset>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching external assets")
            val response = AuthApi.externalService.getExternalAssets(bearerToken)
            Log.d(TAG, "Fetched ${response.size} external assets")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch external assets: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
    
    suspend fun addExternalAsset(
        assetSymbol: String,
        quantity: Double,
        label: String?,
        notes: String?
    ): Result<ExternalAsset> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Adding external asset: $assetSymbol")
            val response = AuthApi.externalService.addExternalAsset(
                bearerToken,
                AddExternalAssetRequest(assetSymbol, quantity, label, notes)
            )
            Log.d(TAG, "External asset added successfully: ${response.id}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add external asset: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
    
    suspend fun updateExternalAsset(
        assetId: String,
        quantity: Double?,
        label: String?,
        notes: String?
    ): Result<ExternalAsset> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating external asset: $assetId")
            val response = AuthApi.externalService.updateExternalAsset(
                bearerToken,
                assetId,
                UpdateExternalAssetRequest(quantity, label, notes)
            )
            Log.d(TAG, "External asset updated successfully")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update external asset: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
    
    suspend fun deleteExternalAsset(assetId: String): Result<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Deleting external asset: $assetId")
            val response = AuthApi.externalService.deleteExternalAsset(bearerToken, assetId)
            Log.d(TAG, "External asset deleted successfully")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete external asset: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
    
    suspend fun getExternalAssetsSummary(): Result<ExternalAssetsSummary> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching external assets summary")
            val response = AuthApi.externalService.getExternalAssetsSummary(bearerToken)
            Log.d(TAG, "Summary: Total value $${response.total_usd_value}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch summary: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
}


