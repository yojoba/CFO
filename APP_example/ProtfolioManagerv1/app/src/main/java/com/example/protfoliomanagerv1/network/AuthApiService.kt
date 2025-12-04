package com.example.protfoliomanagerv1.network

import retrofit2.http.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * API Service for authentication endpoints
 */
interface AuthApiService {
    
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): AuthResponse
    
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): AuthResponse
    
    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): MessageResponse
    
    @GET("auth/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): User
    
    @PUT("auth/me")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Query("full_name") fullName: String?
    ): User
    
    @POST("auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Query("old_password") oldPassword: String,
        @Query("new_password") newPassword: String
    ): MessageResponse
    
    @DELETE("auth/me")
    suspend fun deleteAccount(
        @Header("Authorization") token: String,
        @Query("password") password: String
    ): MessageResponse
}

/**
 * API Service for exchange keys management
 */
interface ExchangeKeysApiService {
    
    @POST("exchange-keys")
    suspend fun addExchangeKey(
        @Header("Authorization") token: String,
        @Body request: AddExchangeKeyRequest
    ): ExchangeKey
    
    @GET("exchange-keys")
    suspend fun getExchangeKeys(
        @Header("Authorization") token: String
    ): List<ExchangeKey>
    
    @GET("exchange-keys/{keyId}")
    suspend fun getExchangeKey(
        @Header("Authorization") token: String,
        @Path("keyId") keyId: String
    ): ExchangeKey
    
    @PUT("exchange-keys/{keyId}")
    suspend fun updateExchangeKey(
        @Header("Authorization") token: String,
        @Path("keyId") keyId: String,
        @Body request: UpdateExchangeKeyRequest
    ): ExchangeKey
    
    @DELETE("exchange-keys/{keyId}")
    suspend fun deleteExchangeKey(
        @Header("Authorization") token: String,
        @Path("keyId") keyId: String
    ): MessageResponse
    
    @POST("exchange-keys/{keyId}/test")
    suspend fun testConnection(
        @Header("Authorization") token: String,
        @Path("keyId") keyId: String
    ): TestConnectionResponse
    
    @GET("exchange-keys/exchange/{exchange}")
    suspend fun getKeysByExchange(
        @Header("Authorization") token: String,
        @Path("exchange") exchange: String
    ): List<ExchangeKey>
}

/**
 * API Service for external assets management
 */
interface ExternalAssetsApiService {
    
    @POST("external-assets")
    suspend fun addExternalAsset(
        @Header("Authorization") token: String,
        @Body request: AddExternalAssetRequest
    ): ExternalAsset
    
    @GET("external-assets")
    suspend fun getExternalAssets(
        @Header("Authorization") token: String
    ): List<ExternalAsset>
    
    @GET("external-assets/{assetId}")
    suspend fun getExternalAsset(
        @Header("Authorization") token: String,
        @Path("assetId") assetId: String
    ): ExternalAsset
    
    @PUT("external-assets/{assetId}")
    suspend fun updateExternalAsset(
        @Header("Authorization") token: String,
        @Path("assetId") assetId: String,
        @Body request: UpdateExternalAssetRequest
    ): ExternalAsset
    
    @DELETE("external-assets/{assetId}")
    suspend fun deleteExternalAsset(
        @Header("Authorization") token: String,
        @Path("assetId") assetId: String
    ): MessageResponse
    
    @GET("external-assets/symbol/{symbol}")
    suspend fun getAssetsBySymbol(
        @Header("Authorization") token: String,
        @Path("symbol") symbol: String
    ): List<ExternalAsset>
    
    @GET("external-assets/summary/total")
    suspend fun getExternalAssetsSummary(
        @Header("Authorization") token: String
    ): ExternalAssetsSummary
}

/**
 * API Service for V2 multi-user portfolio endpoints
 */
interface PortfolioV2ApiService {
    
    @GET("v2/portfolio-summary")
    suspend fun getPortfolioSummary(
        @Header("Authorization") token: String
    ): PortfolioResponse
    
    @GET("v2/usdt-balances")
    suspend fun getUsdtBalances(
        @Header("Authorization") token: String
    ): UsdtBalancesResponse
    
    @GET("v2/futures-positions")
    suspend fun getFuturesPositions(
        @Header("Authorization") token: String
    ): Map<String, List<FuturesPosition>>
    
    @GET("v2/portfolio-history")
    suspend fun getPortfolioHistory(
        @Header("Authorization") token: String
    ): PortfolioHistoryResponse
    
    @POST("v2/buy-spot")
    suspend fun buySpotAsset(
        @Header("Authorization") token: String,
        @Body request: BuySpotRequest
    ): BuySpotResponse
    
    @POST("v2/sell-spot")
    suspend fun sellSpotAsset(
        @Header("Authorization") token: String,
        @Body request: SellSpotRequest
    ): SellSpotResponse
    
    @POST("v2/close-position")
    suspend fun closeFuturesPosition(
        @Header("Authorization") token: String,
        @Body request: ClosePositionRequest
    ): ClosePositionResponse
    
    @GET("v2/health")
    suspend fun healthCheck(
        @Header("Authorization") token: String
    ): Map<String, String>
}

/**
 * Singleton for Auth API
 */
object AuthApi {
    private const val USE_LOCAL = false
    
    private val BASE_URL = if (USE_LOCAL) {
        "http://10.0.2.2:8003/api/"
    } else {
        "https://portfoliomanager.flowbiz.ai/api/"
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
    
    val authService: AuthApiService = retrofit.create(AuthApiService::class.java)
    val keysService: ExchangeKeysApiService = retrofit.create(ExchangeKeysApiService::class.java)
    val externalService: ExternalAssetsApiService = retrofit.create(ExternalAssetsApiService::class.java)
    val portfolioV2Service: PortfolioV2ApiService = retrofit.create(PortfolioV2ApiService::class.java)
}

