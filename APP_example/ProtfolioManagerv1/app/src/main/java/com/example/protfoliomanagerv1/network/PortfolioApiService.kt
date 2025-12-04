package com.example.protfoliomanagerv1.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log  // ✅ Import Log

// ✅ Data Model for Portfolio Summary
data class PortfolioItem(
    val exchange: String,
    val asset: String,
    val balance: Double,
    val usd_value: Double,
    val change_percentage: Double?,
    val trend: String?
)

data class FuturesPosition(
    val symbol: String,
    val size: Double,
    val entry_price: Double,
    val position_value: Double,
    val unrealized_pnl: Double,
    val leverage: Double,
    val side: String
)

data class PortfolioResponse(
    val portfolio: List<PortfolioItem>,
    val total_usd_value: Double,
    val total_change_percentage: Double?,
    val total_trend: String?,
    val exchange_totals: Map<String, Double>,
    val futures_positions: Map<String, List<FuturesPosition>> // ✅ Add this line
)

data class PortfolioSnapshot(
    val timestamp: String,
    val total_usd_value: Double,
    val exchange_totals: Map<String, Double>
)

data class PortfolioHistoryResponse(
    val minute_snapshots: List<PortfolioSnapshot>,
    val daily_snapshots: List<PortfolioSnapshot>
)

// ✅ Data Model for Closing a Futures Position
data class ClosePositionRequest(
    val exchange: String,  // ✅ New field (Binance, Bybit, etc.)
    val symbol: String,
    val size: Double
)

data class ClosePositionResponse(
    val success: Boolean,
    val exchange: String,
    val orderId: String? = null
)

// ✅ Data Model for Selling Spot Assets
data class SellSpotRequest(
    val exchange: String,
    val asset: String,
    val quantity: Double
)

data class SellSpotResponse(
    val success: Boolean,
    val exchange: String,
    val orderId: String? = null,
    val error: String? = null  // Add error field from backend
)

// ✅ Data Model for Buying Spot Assets
data class BuySpotRequest(
    val exchange: String,
    val asset: String,
    val usdt_amount: Double
)

data class BuySpotResponse(
    val success: Boolean,
    val exchange: String,
    val orderId: String? = null,
    val filled_quantity: Double? = null,
    val error: String? = null
)

// ✅ Data Model for USDT Balances
data class UsdtBalancesResponse(
    val binance: Double,
    val bybit: Double,
    val mexc: Double,
    val total: Double
)

// ✅ Data Model for Trading Pairs
data class TradingPair(
    val symbol: String,
    val pair: String,
    val exchange: String
)

data class TradingPairsResponse(
    val pairs: List<TradingPair>,
    val count: Int
)

// ✅ Data Model for Token Search Results
data class TokenSearchResult(
    val symbol: String,
    val exchanges: List<String>,
    val currentPrice: Double?
)

// ✅ Data Model for Token Price
data class TokenPriceResponse(
    val price: Double,
    val symbol: String,
    val exchange: String
)

// ✅ Retrofit API Service
interface PortfolioApiService {
    @GET("portfolio-summary")
    suspend fun getPortfolioSummary(
        @Header("X-API-KEY") apiKey: String // ✅ Requires API Key
    ): PortfolioResponse

    @POST("close-position")
    suspend fun closeFuturesPosition(
        @Header("X-API-KEY") apiKey: String,
        @Body request: ClosePositionRequest
    ): ClosePositionResponse  // ✅ Now correctly expects a JSON object

    @GET("portfolio-history")
    suspend fun getPortfolioHistory(
        @Header("X-API-KEY") apiKey: String
    ): PortfolioHistoryResponse

    @POST("sell-spot")
    suspend fun sellSpotAsset(
        @Header("X-API-KEY") apiKey: String,
        @Body request: SellSpotRequest
    ): SellSpotResponse

    @POST("buy-spot")
    suspend fun buySpotAsset(
        @Header("X-API-KEY") apiKey: String,
        @Body request: BuySpotRequest
    ): BuySpotResponse

    @GET("usdt-balances")
    suspend fun getUsdtBalances(
        @Header("X-API-KEY") apiKey: String
    ): UsdtBalancesResponse

    @GET("search-tokens")
    suspend fun searchTokens(
        @Header("X-API-KEY") apiKey: String,
        @Query("query") query: String
    ): List<TokenSearchResult>

    @GET("token-price/{exchange}/{symbol}")
    suspend fun getTokenPrice(
        @Header("X-API-KEY") apiKey: String,
        @Path("exchange") exchange: String,
        @Path("symbol") symbol: String
    ): TokenPriceResponse
}

// ✅ Singleton Retrofit Instance
object PortfolioApi {
    // 🔧 Toggle between production and local testing
    // Set to true for local Docker testing, false for production
    private const val USE_LOCAL = false
    
    private val BASE_URL = if (USE_LOCAL) {
        "http://10.0.2.2:8003/api/"  // Local Docker (port 8003 from docker-compose.yml)
    } else {
        "https://portfoliomanager.flowbiz.ai/api/"  // Production
    }

    private val loggingInterceptor = okhttp3.logging.HttpLoggingInterceptor().apply {
        level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: PortfolioApiService = retrofit.create(PortfolioApiService::class.java)
}

// ✅ Function to send the sell request to the backend
suspend fun closeFutures(apiKey: String, exchange: String, symbol: String, size: Double): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val response = PortfolioApi.service.closeFuturesPosition(
                apiKey,
                ClosePositionRequest(exchange, symbol, size)
            )
            Log.d("Sell", "📡 API Response: ${response.success}, Order ID: ${response.orderId}") // ✅ Log response
            response.success  // ✅ Returns true if the request succeeded
        } catch (e: Exception) {
            Log.e("Sell", "❌ API Exception: ${e.localizedMessage}")
            false
        }
    }
}



// ✅ Updated Function: Fetch Portfolio Data (Includes Futures)
suspend fun fetchPortfolioData(apiKey: String): PortfolioResponse? {
    return withContext(Dispatchers.IO) {
        try {
            PortfolioApi.service.getPortfolioSummary(apiKey)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
