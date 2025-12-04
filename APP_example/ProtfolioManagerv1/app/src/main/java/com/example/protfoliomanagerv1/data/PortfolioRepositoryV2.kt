package com.example.protfoliomanagerv1.data

import android.util.Log
import com.example.protfoliomanagerv1.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for V2 multi-user portfolio operations
 * Uses JWT authentication instead of hardcoded API key
 */
class PortfolioRepositoryV2(private val token: String) {
    
    companion object {
        private const val TAG = "PortfolioRepositoryV2"
    }
    
    private val bearerToken = "Bearer $token"
    
    suspend fun fetchPortfolio(): Result<PortfolioResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching portfolio from V2 API...")
            val response = AuthApi.portfolioV2Service.getPortfolioSummary(bearerToken)
            Log.d(TAG, "Portfolio fetched successfully!")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching portfolio: ${e.message}")
            Log.e(TAG, "Error details: ${e.toString()}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun fetchPortfolioHistory(): Result<PortfolioHistoryResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching portfolio history from V2 API...")
            val response = AuthApi.portfolioV2Service.getPortfolioHistory(bearerToken)
            Log.d(TAG, "History fetched: ${response.minute_snapshots.size} snapshots")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching history: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun getUsdtBalances(): Result<UsdtBalancesResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching USDT balances from V2 API")
            val response = AuthApi.portfolioV2Service.getUsdtBalances(bearerToken)
            Log.d(TAG, "USDT balances: binance=${response.binance}, bybit=${response.bybit}, mexc=${response.mexc}, total=${response.total}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching USDT balances: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
    
    suspend fun buySpotAsset(
        exchange: String,
        asset: String,
        usdtAmount: Double
    ): Result<BuySpotResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Buying spot: $usdtAmount USDT of $asset on $exchange")
            val response = AuthApi.portfolioV2Service.buySpotAsset(
                bearerToken,
                BuySpotRequest(exchange, asset, usdtAmount)
            )
            Log.d(TAG, "Buy spot response: success=${response.success}, orderId=${response.orderId}, error=${response.error}")
            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.error ?: "Failed to buy asset"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error buying asset: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
    
    suspend fun sellSpotAsset(
        exchange: String,
        asset: String,
        quantity: Double
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Selling spot: $quantity $asset on $exchange")
            val response = AuthApi.portfolioV2Service.sellSpotAsset(
                bearerToken,
                SellSpotRequest(exchange, asset, quantity)
            )
            Log.d(TAG, "Sell spot response: success=${response.success}, error=${response.error}")
            if (response.success) {
                Result.success(true)
            } else {
                val rawError = response.error ?: "Unknown error"
                val errorMsg = when {
                    rawError.contains("Insufficient balance", ignoreCase = true) -> 
                        "Insufficient balance. Asset may have been sold already. Try refreshing."
                    rawError.contains("too many decimals", ignoreCase = true) ->
                        "Invalid quantity precision. Please refresh and try again."
                    rawError.contains("minimum", ignoreCase = true) ->
                        "Order size too small. Minimum order size not met."
                    rawError.contains("not tradeable", ignoreCase = true) ->
                        "Asset not tradeable on this exchange."
                    else -> rawError
                }
                Log.e(TAG, "Sell failed: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error selling asset: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
    
    suspend fun closePosition(
        exchange: String,
        symbol: String,
        size: Double
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = AuthApi.portfolioV2Service.closeFuturesPosition(
                bearerToken,
                ClosePositionRequest(exchange, symbol, size)
            )
            Log.d(TAG, "Close position response: ${response.success}")
            if (response.success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to close position"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error closing position: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
    
    // Token search still uses old endpoint (no auth needed)
    suspend fun searchTokens(query: String): Result<List<TokenSearchResult>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Searching for tokens: $query")
            val response = PortfolioApi.service.searchTokens("VVMwWBhRUWshQJ0rVM8uz4", query)
            Log.d(TAG, "Found ${response.size} tokens matching '$query'")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching tokens: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
    
    suspend fun getTokenPrice(exchange: String, symbol: String): Result<Double> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching price for $symbol on $exchange")
            val response = PortfolioApi.service.getTokenPrice("VVMwWBhRUWshQJ0rVM8uz4", exchange, symbol)
            Log.d(TAG, "$exchange $symbol price: ${response.price}")
            Result.success(response.price)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching token price: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
}


