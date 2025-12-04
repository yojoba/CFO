package com.example.protfoliomanagerv1.data

import android.util.Log
import com.example.protfoliomanagerv1.network.BuySpotRequest
import com.example.protfoliomanagerv1.network.BuySpotResponse
import com.example.protfoliomanagerv1.network.ClosePositionRequest
import com.example.protfoliomanagerv1.network.SellSpotRequest
import com.example.protfoliomanagerv1.network.PortfolioApi
import com.example.protfoliomanagerv1.network.PortfolioResponse
import com.example.protfoliomanagerv1.network.PortfolioHistoryResponse
import com.example.protfoliomanagerv1.network.UsdtBalancesResponse
import com.example.protfoliomanagerv1.network.TokenSearchResult
import com.example.protfoliomanagerv1.network.TokenPriceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PortfolioRepository(private val apiKey: String) {
    
    suspend fun fetchPortfolio(): Result<PortfolioResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("PortfolioRepository", "Fetching portfolio from API...")
            val response = PortfolioApi.service.getPortfolioSummary(apiKey)
            Log.d("PortfolioRepository", "Portfolio fetched successfully!")
            Result.success(response)
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "Error fetching portfolio: ${e.message}")
            Log.e("PortfolioRepository", "Error details: ${e.toString()}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun closePosition(
        exchange: String,
        symbol: String,
        size: Double
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = PortfolioApi.service.closeFuturesPosition(
                apiKey,
                ClosePositionRequest(exchange, symbol, size)
            )
            Log.d("PortfolioRepository", "Close position response: ${response.success}")
            if (response.success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to close position"))
            }
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "Error closing position: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun fetchPortfolioHistory(): Result<PortfolioHistoryResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("PortfolioRepository", "Fetching portfolio history...")
            val response = PortfolioApi.service.getPortfolioHistory(apiKey)
            Log.d("PortfolioRepository", "History fetched: ${response.minute_snapshots.size} snapshots")
            Result.success(response)
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "Error fetching history: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun sellSpotAsset(
        exchange: String,
        asset: String,
        quantity: Double
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d("PortfolioRepository", "Selling spot: $quantity $asset on $exchange")
            val response = PortfolioApi.service.sellSpotAsset(
                apiKey,
                SellSpotRequest(exchange, asset, quantity)
            )
            Log.d("PortfolioRepository", "Sell spot response: success=${response.success}, error=${response.error}")
            if (response.success) {
                Result.success(true)
            } else {
                // Return the actual error message from the exchange
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
                Log.e("PortfolioRepository", "Sell failed: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "Error selling asset: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun buySpotAsset(
        exchange: String,
        asset: String,
        usdtAmount: Double
    ): Result<BuySpotResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("PortfolioRepository", "Buying spot: $usdtAmount USDT of $asset on $exchange")
            val response = PortfolioApi.service.buySpotAsset(
                apiKey,
                BuySpotRequest(exchange, asset, usdtAmount)
            )
            Log.d("PortfolioRepository", "Buy spot response: success=${response.success}, orderId=${response.orderId}, error=${response.error}")
            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.error ?: "Failed to buy asset"))
            }
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "Error buying asset: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun getUsdtBalances(): Result<UsdtBalancesResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("PortfolioRepository", "Fetching USDT balances")
            val response = PortfolioApi.service.getUsdtBalances(apiKey)
            Log.d("PortfolioRepository", "USDT balances: binance=${response.binance}, bybit=${response.bybit}, mexc=${response.mexc}, total=${response.total}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "Error fetching USDT balances: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun searchTokens(query: String): Result<List<TokenSearchResult>> = withContext(Dispatchers.IO) {
        try {
            Log.d("PortfolioRepository", "Searching for tokens: $query")
            val response = PortfolioApi.service.searchTokens(apiKey, query)
            Log.d("PortfolioRepository", "Found ${response.size} tokens matching '$query'")
            Result.success(response)
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "Error searching tokens: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun getTokenPrice(exchange: String, symbol: String): Result<Double> = withContext(Dispatchers.IO) {
        try {
            Log.d("PortfolioRepository", "Fetching price for $symbol on $exchange")
            val response = PortfolioApi.service.getTokenPrice(apiKey, exchange, symbol)
            Log.d("PortfolioRepository", "$exchange $symbol price: ${response.price}")
            Result.success(response.price)
        } catch (e: Exception) {
            Log.e("PortfolioRepository", "Error fetching token price: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
}

