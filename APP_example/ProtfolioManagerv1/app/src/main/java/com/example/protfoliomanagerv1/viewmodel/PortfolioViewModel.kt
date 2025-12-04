package com.example.protfoliomanagerv1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.protfoliomanagerv1.auth.TokenManager
import com.example.protfoliomanagerv1.data.PortfolioRepository
import com.example.protfoliomanagerv1.data.PortfolioRepositoryV2
import com.example.protfoliomanagerv1.ui.state.PortfolioUiState
import com.example.protfoliomanagerv1.ui.state.ClosePositionState
import com.example.protfoliomanagerv1.ui.state.SellAssetState
import com.example.protfoliomanagerv1.ui.state.BuyAssetState
import com.example.protfoliomanagerv1.network.PortfolioHistoryResponse
import com.example.protfoliomanagerv1.network.UsdtBalancesResponse
import com.example.protfoliomanagerv1.network.TokenSearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenManager = TokenManager(application)
    private val token = tokenManager.getToken() ?: ""
    
    // Use V2 repository with JWT token for multi-user support
    private val repository = PortfolioRepositoryV2(token)

    private val _uiState = MutableStateFlow<PortfolioUiState>(PortfolioUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _closePositionState = MutableStateFlow<ClosePositionState>(ClosePositionState.Idle)
    val closePositionState = _closePositionState.asStateFlow()

    private val _sellAssetState = MutableStateFlow<SellAssetState>(SellAssetState.Idle)
    val sellAssetState = _sellAssetState.asStateFlow()

    private val _buyAssetState = MutableStateFlow<BuyAssetState>(BuyAssetState.Idle)
    val buyAssetState = _buyAssetState.asStateFlow()

    private val _usdtBalances = MutableStateFlow<UsdtBalancesResponse?>(null)
    val usdtBalances = _usdtBalances.asStateFlow()

    private val _historyData = MutableStateFlow<PortfolioHistoryResponse?>(null)
    val historyData = _historyData.asStateFlow()

    private val _searchResults = MutableStateFlow<List<TokenSearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    init {
        fetchPortfolio()
        fetchUsdtBalances()
    }

    fun fetchPortfolio() {
        viewModelScope.launch {
            _uiState.value = PortfolioUiState.Loading
            repository.fetchPortfolio()
                .onSuccess { response ->
                    _uiState.value = PortfolioUiState.Success(
                        portfolio = response.portfolio,
                        totalValue = response.total_usd_value,
                        totalChangePercentage = response.total_change_percentage,
                        totalTrend = response.total_trend,
                        exchangeTotals = response.exchange_totals,
                        futuresPositions = response.futures_positions ?: emptyMap()
                    )
                }
                .onFailure { error ->
                    _uiState.value = PortfolioUiState.Error(
                        error.localizedMessage ?: "Unknown error occurred"
                    )
                }
        }
    }

    fun closePosition(exchange: String, symbol: String, size: Double) {
        viewModelScope.launch {
            _closePositionState.value = ClosePositionState.Loading
            repository.closePosition(exchange, symbol, size)
                .onSuccess {
                    _closePositionState.value = ClosePositionState.Success(symbol)
                    // Refresh portfolio after closing position
                    fetchPortfolio()
                }
                .onFailure { error ->
                    _closePositionState.value = ClosePositionState.Error(
                        error.localizedMessage ?: "Failed to close position"
                    )
                }
        }
    }

    fun resetClosePositionState() {
        _closePositionState.value = ClosePositionState.Idle
    }

    fun fetchHistory() {
        viewModelScope.launch {
            repository.fetchPortfolioHistory()
                .onSuccess { history ->
                    _historyData.value = history
                }
                .onFailure { error ->
                    // Handle error silently or log it
                }
        }
    }

    fun sellSpotAsset(exchange: String, asset: String, quantity: Double) {
        viewModelScope.launch {
            _sellAssetState.value = SellAssetState.Loading
            repository.sellSpotAsset(exchange, asset, quantity)
                .onSuccess {
                    _sellAssetState.value = SellAssetState.Success(asset)
                    // Refresh portfolio and USDT balances after selling
                    fetchPortfolio()
                    fetchUsdtBalances()
                }
                .onFailure { error ->
                    _sellAssetState.value = SellAssetState.Error(
                        error.localizedMessage ?: "Failed to sell asset"
                    )
                }
        }
    }

    fun resetSellAssetState() {
        _sellAssetState.value = SellAssetState.Idle
    }

    fun buySpotAsset(exchange: String, asset: String, usdtAmount: Double) {
        viewModelScope.launch {
            _buyAssetState.value = BuyAssetState.Loading
            repository.buySpotAsset(exchange, asset, usdtAmount)
                .onSuccess { response ->
                    val quantity = response.filled_quantity ?: 0.0
                    _buyAssetState.value = BuyAssetState.Success(asset, quantity)
                    // Refresh portfolio and USDT balances after buying
                    fetchPortfolio()
                    fetchUsdtBalances()
                }
                .onFailure { error ->
                    _buyAssetState.value = BuyAssetState.Error(
                        error.localizedMessage ?: "Failed to buy asset"
                    )
                }
        }
    }

    fun resetBuyAssetState() {
        _buyAssetState.value = BuyAssetState.Idle
    }

    fun fetchUsdtBalances() {
        viewModelScope.launch {
            repository.getUsdtBalances()
                .onSuccess { balances ->
                    _usdtBalances.value = balances
                }
                .onFailure { error ->
                    // Handle error silently or log it
                    _usdtBalances.value = null
                }
        }
    }

    fun searchTokens(query: String) {
        // Cancel previous search job
        searchJob?.cancel()
        
        if (query.length < 2) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        
        // Debounce search by 500ms
        searchJob = viewModelScope.launch {
            delay(500)
            _isSearching.value = true
            repository.searchTokens(query)
                .onSuccess { results ->
                    _searchResults.value = results
                }
                .onFailure {
                    _searchResults.value = emptyList()
                }
            _isSearching.value = false
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
        _isSearching.value = false
        searchJob?.cancel()
    }
    
    suspend fun getTokenPrice(exchange: String, symbol: String): Result<Double> {
        return repository.getTokenPrice(exchange, symbol)
    }
}
