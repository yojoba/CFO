package com.example.protfoliomanagerv1.ui.state

import com.example.protfoliomanagerv1.network.FuturesPosition
import com.example.protfoliomanagerv1.network.PortfolioItem

sealed class PortfolioUiState {
    object Loading : PortfolioUiState()
    data class Success(
        val portfolio: List<PortfolioItem>,
        val totalValue: Double,
        val totalChangePercentage: Double?,
        val totalTrend: String?,
        val exchangeTotals: Map<String, Double>,
        val futuresPositions: Map<String, List<FuturesPosition>>
    ) : PortfolioUiState()
    data class Error(val message: String) : PortfolioUiState()
}

sealed class ClosePositionState {
    object Idle : ClosePositionState()
    object Loading : ClosePositionState()
    data class Success(val symbol: String) : ClosePositionState()
    data class Error(val message: String) : ClosePositionState()
}

sealed class SellAssetState {
    object Idle : SellAssetState()
    object Loading : SellAssetState()
    data class Success(val asset: String) : SellAssetState()
    data class Error(val message: String) : SellAssetState()
}

sealed class BuyAssetState {
    object Idle : BuyAssetState()
    object Loading : BuyAssetState()
    data class Success(val asset: String, val quantity: Double) : BuyAssetState()
    data class Error(val message: String) : BuyAssetState()
}

