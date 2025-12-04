package com.example.protfoliomanagerv1.network

/**
 * Data models for authentication
 */

// User model
data class User(
    val id: String,
    val email: String,
    val full_name: String?,
    val created_at: String,
    val email_verified: Boolean
)

// Registration request
data class RegisterRequest(
    val email: String,
    val password: String,
    val full_name: String
)

// Login request
data class LoginRequest(
    val email: String,
    val password: String
)

// Auth response (login/register)
data class AuthResponse(
    val token: String,
    val user: User,
    val expires_at: String
)

// Message response
data class MessageResponse(
    val message: String
)

// Exchange key model
data class ExchangeKey(
    val id: String,
    val exchange: String,
    val label: String?,
    val is_active: Boolean,
    val created_at: String,
    val updated_at: String,
    val last_used: String?
)

// Add exchange key request
data class AddExchangeKeyRequest(
    val exchange: String,
    val api_key: String,
    val api_secret: String,
    val label: String?
)

// Update exchange key request
data class UpdateExchangeKeyRequest(
    val api_key: String?,
    val api_secret: String?,
    val label: String?,
    val is_active: Boolean?
)

// Test connection response
data class TestConnectionResponse(
    val success: Boolean,
    val message: String,
    val exchange: String
)

// External asset model
data class ExternalAsset(
    val id: String,
    val asset_symbol: String,
    val quantity: Double,
    val label: String?,
    val notes: String?,
    val current_price: Double?,
    val usd_value: Double?,
    val created_at: String,
    val updated_at: String
)

// Add external asset request
data class AddExternalAssetRequest(
    val asset_symbol: String,
    val quantity: Double,
    val label: String?,
    val notes: String?
)

// Update external asset request
data class UpdateExternalAssetRequest(
    val quantity: Double?,
    val label: String?,
    val notes: String?
)

// External assets summary
data class ExternalAssetsSummary(
    val total_usd_value: Double,
    val assets: List<ExternalAssetSummaryItem>,
    val total_assets: Int
)

data class ExternalAssetSummaryItem(
    val symbol: String,
    val total_quantity: Double,
    val current_price: Double?,
    val usd_value: Double?,
    val count: Int
)


