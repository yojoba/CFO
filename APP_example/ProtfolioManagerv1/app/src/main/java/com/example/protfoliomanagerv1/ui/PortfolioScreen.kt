package com.example.protfoliomanagerv1.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.graphicsLayer
import com.example.protfoliomanagerv1.network.FuturesPosition
import com.example.protfoliomanagerv1.network.PortfolioItem
import com.example.protfoliomanagerv1.ui.animations.*
import com.example.protfoliomanagerv1.ui.components.*
import com.example.protfoliomanagerv1.ui.navigation.*
import com.example.protfoliomanagerv1.ui.state.*
import com.example.protfoliomanagerv1.ui.state.SellAssetState
import com.example.protfoliomanagerv1.ui.theme.*
import com.example.protfoliomanagerv1.viewmodel.PortfolioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    modifier: Modifier = Modifier,
    viewModel: PortfolioViewModel = viewModel(),
    onLogout: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val closePositionState by viewModel.closePositionState.collectAsState()
    val sellAssetState by viewModel.sellAssetState.collectAsState()
    val buyAssetState by viewModel.buyAssetState.collectAsState()
    val usdtBalances by viewModel.usdtBalances.collectAsState()
    var currentScreen by remember { mutableStateOf("portfolio") }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle close position feedback
    LaunchedEffect(closePositionState) {
        when (closePositionState) {
            is ClosePositionState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Position ${(closePositionState as ClosePositionState.Success).symbol} closed successfully",
                    duration = SnackbarDuration.Short
                )
                viewModel.resetClosePositionState()
            }
            is ClosePositionState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Error: ${(closePositionState as ClosePositionState.Error).message}",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetClosePositionState()
            }
            else -> {}
        }
    }
    
    // Handle sell asset feedback
    LaunchedEffect(sellAssetState) {
        when (sellAssetState) {
            is SellAssetState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "${(sellAssetState as SellAssetState.Success).asset} sold successfully",
                    duration = SnackbarDuration.Short
                )
                viewModel.resetSellAssetState()
            }
            is SellAssetState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Error: ${(sellAssetState as SellAssetState.Error).message}",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetSellAssetState()
            }
            else -> {}
        }
    }
    
    // Handle buy asset feedback
    LaunchedEffect(buyAssetState) {
        when (buyAssetState) {
            is BuyAssetState.Success -> {
                val state = buyAssetState as BuyAssetState.Success
                snackbarHostState.showSnackbar(
                    message = "Bought ${String.format("%.6f", state.quantity)} ${state.asset}",
                    duration = SnackbarDuration.Short
                )
                viewModel.resetBuyAssetState()
            }
            is BuyAssetState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Error: ${(buyAssetState as BuyAssetState.Error).message}",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetBuyAssetState()
            }
            else -> {}
        }
    }
    
    // Fetch USDT balances on screen load
    LaunchedEffect(Unit) {
        viewModel.fetchUsdtBalances()
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            CryptoBottomNavigation(
                currentRoute = currentScreen,
                onNavigate = { currentScreen = it }
            )
        },
        containerColor = CryptoDarkBackground
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is PortfolioUiState.Loading -> {
                    LoadingScreen()
                }
                is PortfolioUiState.Success -> {
                    val successState = uiState as PortfolioUiState.Success
                    when (currentScreen) {
                        "portfolio" -> PortfolioContent(
                            portfolio = successState.portfolio,
                            totalValue = successState.totalValue,
                            totalChangePercentage = successState.totalChangePercentage,
                            totalTrend = successState.totalTrend,
                            exchangeTotals = successState.exchangeTotals,
                            onRefresh = { 
                                viewModel.fetchPortfolio()
                                viewModel.fetchUsdtBalances()
                            },
                            onSellAsset = { exchange, asset, quantity ->
                                viewModel.sellSpotAsset(exchange, asset, quantity)
                            },
                            onBuyAsset = { exchange, asset, usdtAmount ->
                                viewModel.buySpotAsset(exchange, asset, usdtAmount)
                            },
                            usdtBalances = mapOf(
                                "binance" to (usdtBalances?.binance ?: 0.0),
                                "bybit" to (usdtBalances?.bybit ?: 0.0),
                                "mexc" to (usdtBalances?.mexc ?: 0.0)
                            ),
                            isSelling = sellAssetState is SellAssetState.Loading,
                            isBuying = buyAssetState is BuyAssetState.Loading,
                            viewModel = viewModel,
                            onNavigateToSettings = onNavigateToSettings
                        )
                        "futures" -> FuturesContent(
                            futuresPositions = successState.futuresPositions,
                            onClosePosition = { exchange, symbol, size ->
                                viewModel.closePosition(exchange, symbol, size)
                            },
                            isClosing = closePositionState is ClosePositionState.Loading,
                            onRefresh = { viewModel.fetchPortfolio() }
                        )
                        "history" -> HistoryContent(
                            exchangeTotals = successState.exchangeTotals,
                            viewModel = viewModel
                        )
                    }
                }
                is PortfolioUiState.Error -> {
                    ErrorScreen(
                        message = (uiState as PortfolioUiState.Error).message,
                        onRetry = { viewModel.fetchPortfolio() }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = GradientBlue,
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Loading portfolio...",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⚠️",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error Loading Portfolio",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = GradientPurple
            )
        ) {
            Text("Retry")
        }
    }
}

@Composable
private fun PortfolioContent(
    portfolio: List<PortfolioItem>,
    totalValue: Double,
    totalChangePercentage: Double?,
    totalTrend: String?,
    exchangeTotals: Map<String, Double>,
    onRefresh: () -> Unit,
    onSellAsset: (String, String, Double) -> Unit,
    onBuyAsset: (String, String, Double) -> Unit,
    usdtBalances: Map<String, Double>,
    isSelling: Boolean,
    isBuying: Boolean,
    viewModel: PortfolioViewModel,
    onNavigateToSettings: () -> Unit = {}
) {
    // Search state
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    
    // State for new token purchase
    var selectedToken by remember { mutableStateOf<String?>(null) }
    var selectedExchanges by remember { mutableStateOf<List<String>>(emptyList()) }
    var showExchangeDialog by remember { mutableStateOf(false) }
    var showBuyDialogForNewToken by remember { mutableStateOf(false) }
    var selectedExchange by remember { mutableStateOf<String?>(null) }
    var tokenPrice by remember { mutableStateOf<Double?>(null) }
    var isFetchingPrice by remember { mutableStateOf(false) }
    
    // Clear search when buy is successful
    LaunchedEffect(isBuying) {
        if (!isBuying && searchQuery.isNotEmpty()) {
            // Don't clear immediately, let user see the result
        }
    }
    
    // Fetch token price when exchange is selected
    LaunchedEffect(selectedExchange, selectedToken) {
        if (selectedExchange != null && selectedToken != null && isFetchingPrice) {
            viewModel.getTokenPrice(selectedExchange!!, selectedToken!!)
                .onSuccess { price ->
                    tokenPrice = price
                    isFetchingPrice = false
                    showBuyDialogForNewToken = true
                }
                .onFailure {
                    isFetchingPrice = false
                    tokenPrice = null
                }
        }
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Header Section with Search
        item {
            PortfolioHeader(
                totalValue = totalValue,
                totalChangePercentage = totalChangePercentage,
                totalTrend = totalTrend,
                onRefresh = onRefresh,
                searchQuery = searchQuery,
                onSearchQueryChange = { query ->
                    searchQuery = query
                    if (query.length >= 2) {
                        viewModel.searchTokens(query)
                    } else {
                        viewModel.clearSearchResults()
                    }
                },
                isSearching = isSearching,
                onNavigateToSettings = onNavigateToSettings
            )
        }
        
        // Search Results
        if (searchQuery.length >= 2) {
            item {
                SearchResultsList(
                    searchResults = searchResults,
                    isSearching = isSearching,
                    onTokenClick = { symbol, exchanges ->
                        selectedToken = symbol
                        selectedExchanges = exchanges
                        
                        if (exchanges.size == 1) {
                            // Direct buy on single exchange
                            selectedExchange = exchanges[0]
                            isFetchingPrice = true
                        } else {
                            // Show exchange selection dialog
                            showExchangeDialog = true
                        }
                    }
                )
            }
        }
        
        // Exchange Distribution Chart
        if (searchQuery.isEmpty()) {
            item {
                ExchangeDistributionChart(exchangeTotals = exchangeTotals)
            }
        }
        
        // Assets Section Header
        if (searchQuery.isEmpty()) {
            item {
                Text(
                    text = "Your Assets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        
            // Portfolio Items
            val filteredPortfolio = portfolio.filter { it.usd_value >= 5.0 }
            val sortedPortfolio = filteredPortfolio.sortedByDescending { it.usd_value }
            
            items(sortedPortfolio) { item ->
                // Get USDT balance for this exchange
                val exchangeUsdt = when (item.exchange.lowercase()) {
                    "binance" -> usdtBalances["binance"] ?: 0.0
                    "bybit" -> usdtBalances["bybit"] ?: 0.0
                    "mexc" -> usdtBalances["mexc"] ?: 0.0
                    else -> 0.0
                }
                
                ModernPortfolioCard(
                    item = item,
                    onSellClick = onSellAsset,
                    onBuyClick = onBuyAsset,
                    availableUsdt = exchangeUsdt,
                    isSelling = isSelling,
                    isBuying = isBuying
                )
            }
        }
    }
    
    // Exchange Selection Dialog
    if (showExchangeDialog && selectedToken != null) {
        ExchangeSelectionDialog(
            symbol = selectedToken!!,
            exchanges = selectedExchanges,
            usdtBalances = usdtBalances,
            onExchangeSelected = { exchange ->
                showExchangeDialog = false
                selectedExchange = exchange
                isFetchingPrice = true
            },
            onDismiss = {
                showExchangeDialog = false
                selectedToken = null
                selectedExchanges = emptyList()
            }
        )
    }
    
    // Buy Dialog for New Token
    if (showBuyDialogForNewToken && selectedToken != null && selectedExchange != null && tokenPrice != null) {
        val exchangeUsdt = usdtBalances[selectedExchange!!] ?: 0.0
        
        // Create synthetic PortfolioItem for new token
        val syntheticItem = PortfolioItem(
            exchange = selectedExchange!!,
            asset = selectedToken!!,
            balance = 0.0,
            usd_value = 0.0,
            change_percentage = null,
            trend = null
        )
        
        BuyAssetDialog(
            item = syntheticItem,
            availableUsdt = exchangeUsdt,
            onConfirm = { usdtAmount ->
                showBuyDialogForNewToken = false
                onBuyAsset(selectedExchange!!, selectedToken!!, usdtAmount)
                // Clear search after successful buy
                searchQuery = ""
                viewModel.clearSearchResults()
                selectedToken = null
                selectedExchange = null
                tokenPrice = null
            },
            onDismiss = {
                showBuyDialogForNewToken = false
                selectedToken = null
                selectedExchange = null
                tokenPrice = null
            },
            isBuying = isBuying,
            isNewAsset = true,
            currentPrice = tokenPrice
        )
    }
    
    // Loading indicator for price fetch
    if (isFetchingPrice) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = CryptoDarkSurface,
            tonalElevation = 8.dp,
            title = {
                Text(
                    text = "Loading Price...",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = GradientBlue,
                        modifier = Modifier.size(40.dp)
                    )
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun PortfolioHeader(
    totalValue: Double,
    totalChangePercentage: Double?,
    totalTrend: String?,
    onRefresh: () -> Unit,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    isSearching: Boolean = false,
    onNavigateToSettings: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CryptoDarkSurface,
                        CryptoDarkBackground
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Portfolio",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Row {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = GradientBlue
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = GradientBlue
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Total Value Display
            Text(
                text = "Total Value",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$${String.format("%,.2f", totalValue)}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            // 24h Change
            if (totalChangePercentage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val changeColor = if (totalChangePercentage >= 0) NeonGreen else ElectricRed
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = totalTrend ?: "",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${String.format("%.2f", totalChangePercentage)}% (24h)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = changeColor
                    )
                }
            }
            
            // Search Bar
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { 
                    Text(
                        text = "Search tokens to buy...",
                        color = TextSecondary
                    ) 
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = GradientBlue
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextSecondary
                            )
                        }
                    } else if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = GradientBlue,
                            strokeWidth = 2.dp
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GradientBlue,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = GradientBlue
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }
    }
}

@Composable
private fun ModernPortfolioCard(
    item: PortfolioItem,
    onSellClick: (String, String, Double) -> Unit,
    onBuyClick: (String, String, Double) -> Unit,
    availableUsdt: Double,
    isSelling: Boolean,
    isBuying: Boolean
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showSellDialog by remember { mutableStateOf(false) }
    var showBuyDialog by remember { mutableStateOf(false) }
    
    val bgColor = when {
        item.change_percentage != null && item.change_percentage > 0 -> ProfitGreenBg
        item.change_percentage != null && item.change_percentage < 0 -> LossRedBg
        else -> CryptoDarkSurface
    }
    
    // All assets from actual exchanges can be traded
    // External assets are manually tracked and can't be traded via API
    val canTrade = item.exchange.lowercase() in listOf("binance", "bybit", "mexc")
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { if (canTrade) isExpanded = !isExpanded }
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.asset,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = item.exchange.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "Balance: ${String.format("%.4f", item.balance)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${String.format("%,.2f", item.usd_value)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                item.change_percentage?.let {
                    val changeColor = if (it >= 0) NeonGreen else ElectricRed
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.trend ?: "",
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${String.format("%.2f", it)}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = changeColor
                        )
                    }
                }
                }
            }
            
            // Buy and Sell action buttons - only show when expanded
            AnimatedVisibility(
                visible = isExpanded && canTrade,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Buy Button
                        Button(
                            onClick = { showBuyDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen.copy(alpha = 0.2f),
                                contentColor = NeonGreen
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Buy",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        
                        // Sell Button
                        Button(
                            onClick = { showSellDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricRed.copy(alpha = 0.2f),
                                contentColor = ElectricRed
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Sell",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showSellDialog && canTrade) {
        SellAssetDialog(
            item = item,
            onConfirm = { quantity ->
                showSellDialog = false
                onSellClick(item.exchange, item.asset, quantity)
            },
            onDismiss = { showSellDialog = false },
            isSelling = isSelling
        )
    }
    
    if (showBuyDialog && canTrade) {
        BuyAssetDialog(
            item = item,
            availableUsdt = availableUsdt,
            onConfirm = { usdtAmount ->
                showBuyDialog = false
                onBuyClick(item.exchange, item.asset, usdtAmount)
            },
            onDismiss = { showBuyDialog = false },
            isBuying = isBuying
        )
    }
}

@Composable
fun SellAssetDialog(
    item: PortfolioItem,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
    isSelling: Boolean
) {
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    var authError by remember { mutableStateOf<String?>(null) }
    var sliderPosition by remember { mutableStateOf(1f) } // Start at 100%
    var usdAmount by remember { mutableStateOf(String.format("%.2f", item.usd_value)) }
    var sellError by remember { mutableStateOf<String?>(null) }
    
    // Calculate USD amount from slider
    val calculatedUsd = (item.usd_value * sliderPosition).toDouble()
    
    // Calculate asset quantity to sell based on USD amount
    val pricePerUnit = if (item.balance > 0) item.usd_value / item.balance else 0.0
    val quantityToSell = if (pricePerUnit > 0) {
        if (usdAmount.isNotEmpty()) {
            (usdAmount.toDoubleOrNull() ?: 0.0) / pricePerUnit
        } else {
            calculatedUsd / pricePerUnit
        }
    } else {
        0.0
    }
    
    // Sync slider with manual input
    LaunchedEffect(usdAmount) {
        val amount = usdAmount.toDoubleOrNull()
        if (amount != null && item.usd_value > 0) {
            sliderPosition = (amount / item.usd_value).toFloat().coerceIn(0f, 1f)
        }
    }
    
    // Sync manual input with slider
    LaunchedEffect(sliderPosition) {
        if (usdAmount.isEmpty() || usdAmount.toDoubleOrNull() == null) {
            usdAmount = String.format("%.2f", calculatedUsd)
        }
    }
    
    AlertDialog(
        onDismissRequest = if (!isSelling) onDismiss else { {} },
        containerColor = CryptoDarkSurface,
        tonalElevation = 8.dp,
        title = {
            Text(
                text = "Sell ${item.asset}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "How much do you want to sell?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Asset info
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CryptoDarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        DialogDetailRow("Exchange", item.exchange.uppercase())
                        DialogDetailRow("Asset", item.asset)
                        DialogDetailRow("Current Price", "$${String.format("%.4f", pricePerUnit)}")
                        DialogDetailRow("Total Balance", "${String.format("%.4f", item.balance)} ($${String.format("%.2f", item.usd_value)})")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Slider with percentage markers
                Column {
                    Text(
                        text = "Select Amount to Sell",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Slider(
                        value = sliderPosition,
                        onValueChange = { newValue ->
                            sliderPosition = newValue
                            usdAmount = String.format("%.2f", item.usd_value * newValue)
                        },
                        valueRange = 0f..1f,
                        steps = 3,  // Creates stops at 0%, 25%, 50%, 75%, 100%
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricRed,
                            activeTrackColor = ElectricRed,
                            inactiveTrackColor = TextSecondary.copy(alpha = 0.3f)
                        )
                    )
                    
                    // Percentage labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("0%", "25%", "50%", "75%", "100%").forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Manual USD input
                OutlinedTextField(
                    value = usdAmount,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            usdAmount = newValue
                            sellError = null
                        }
                    },
                    label = { Text("USD Amount to Sell") },
                    placeholder = { Text("Enter amount") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricRed,
                        unfocusedBorderColor = TextSecondary,
                        focusedLabelColor = ElectricRed,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = ElectricRed
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Estimated quantity to sell
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElectricRed.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "You Will Sell",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format("%.6f", quantityToSell)} ${item.asset}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ElectricRed
                        )
                        Text(
                            text = "≈ $${usdAmount.toDoubleOrNull()?.let { String.format("%.2f", it) } ?: "0.00"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                
                // Error messages
                if (sellError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = sellError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricRed,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (authError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = authError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = usdAmount.toDoubleOrNull()
                    when {
                        amount == null || amount <= 0 -> {
                            sellError = "Please enter a valid amount"
                        }
                        amount > item.usd_value -> {
                            sellError = "Amount exceeds available balance"
                        }
                        quantityToSell > item.balance -> {
                            sellError = "Quantity exceeds available balance"
                        }
                        else -> {
                            sellError = null
                            // Trigger biometric authentication before confirming sell
                            if (activity != null) {
                                authError = null
                                com.example.protfoliomanagerv1.auth.BiometricAuthManager.authenticateForSell(
                                    activity = activity,
                                    asset = item.asset,
                                    quantity = String.format("%.6f", quantityToSell),
                                    onSuccess = {
                                        onConfirm(quantityToSell) // Execute the sell with the calculated quantity
                                    },
                                    onError = { error ->
                                        authError = error
                                    }
                                )
                            } else {
                                // Fallback if activity is not available
                                onConfirm(quantityToSell)
                            }
                        }
                    }
                },
                enabled = !isSelling,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricRed,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isSelling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = TextPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSelling) "Selling..." else "Authenticate to Sell",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSelling,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
fun BuyAssetDialog(
    item: PortfolioItem,
    availableUsdt: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
    isBuying: Boolean,
    isNewAsset: Boolean = false,
    currentPrice: Double? = null
) {
    var sliderPosition by remember { mutableStateOf(0f) }
    var usdtAmount by remember { mutableStateOf("") }
    var buyError by remember { mutableStateOf<String?>(null) }

    // Calculate USDT amount from slider
    val calculatedUsdt = (availableUsdt * sliderPosition).toDouble()
    
    // Calculate price per unit - for new assets use provided price, for existing use calculated
    val pricePerUnit = if (isNewAsset && currentPrice != null) {
        currentPrice
    } else if (item.balance > 0 && item.usd_value > 0) {
        item.usd_value / item.balance
    } else {
        currentPrice ?: 0.0
    }
    
    // Calculate estimated asset quantity based on current price
    val estimatedQuantity = if (pricePerUnit > 0) {
        if (usdtAmount.isNotEmpty()) {
            usdtAmount.toDoubleOrNull()?.div(pricePerUnit) ?: 0.0
        } else {
            calculatedUsdt / pricePerUnit
        }
    } else {
        0.0
    }

    // Sync slider with manual input
    LaunchedEffect(usdtAmount) {
        val amount = usdtAmount.toDoubleOrNull()
        if (amount != null && availableUsdt > 0) {
            sliderPosition = (amount / availableUsdt).toFloat().coerceIn(0f, 1f)
        }
    }

    // Sync manual input with slider
    LaunchedEffect(sliderPosition) {
        if (usdtAmount.isEmpty() || usdtAmount.toDoubleOrNull() == null) {
            usdtAmount = String.format("%.2f", calculatedUsdt)
        }
    }

    AlertDialog(
        onDismissRequest = if (!isBuying) onDismiss else { {} },
        containerColor = CryptoDarkSurface,
        tonalElevation = 8.dp,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Buy ${item.asset}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (isNewAsset) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = GradientBlue.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "NEW",
                                style = MaterialTheme.typography.labelSmall,
                                color = GradientBlue,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column {
                Text(
                    text = "How much USDT do you want to spend?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Available USDT display
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CryptoDarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        DialogDetailRow("Exchange", item.exchange.uppercase())
                        DialogDetailRow("Asset", item.asset)
                        DialogDetailRow("Current Price", "$${String.format("%.4f", pricePerUnit)}")
                        DialogDetailRow("Available USDT", "$${String.format("%.2f", availableUsdt)}")
                        if (isNewAsset) {
                            DialogDetailRow("Status", "Not in portfolio")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Slider with percentage markers
                Column {
                    Text(
                        text = "Select Amount",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = sliderPosition,
                        onValueChange = { newValue ->
                            sliderPosition = newValue
                            usdtAmount = String.format("%.2f", availableUsdt * newValue)
                        },
                        valueRange = 0f..1f,
                        steps = 3,  // Creates stops at 0%, 25%, 50%, 75%, 100%
                        colors = SliderDefaults.colors(
                            thumbColor = NeonGreen,
                            activeTrackColor = NeonGreen,
                            inactiveTrackColor = TextSecondary.copy(alpha = 0.3f)
                        )
                    )

                    // Percentage labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("0%", "25%", "50%", "75%", "100%").forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Manual USDT input
                OutlinedTextField(
                    value = usdtAmount,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            usdtAmount = newValue
                            buyError = null
                        }
                    },
                    label = { Text("USDT Amount") },
                    placeholder = { Text("Enter amount") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = TextSecondary,
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = NeonGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Estimated quantity display
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeonGreen.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Estimated Purchase",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format("%.6f", estimatedQuantity)} ${item.asset}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                        Text(
                            text = "≈ $${usdtAmount.toDoubleOrNull()?.let { String.format("%.2f", it) } ?: "0.00"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Error message
                if (buyError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = buyError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = usdtAmount.toDoubleOrNull()
                    when {
                        amount == null || amount <= 0 -> {
                            buyError = "Please enter a valid amount"
                        }
                        amount > availableUsdt -> {
                            buyError = "Insufficient USDT balance"
                        }
                        else -> {
                            buyError = null
                            onConfirm(amount)
                        }
                    }
                },
                enabled = !isBuying,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = CryptoDarkBackground
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isBuying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = CryptoDarkBackground,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isBuying) "Buying..." else "Confirm Buy",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isBuying,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
private fun DialogDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
private fun SearchResultsList(
    searchResults: List<com.example.protfoliomanagerv1.network.TokenSearchResult>,
    isSearching: Boolean,
    onTokenClick: (String, List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSearching) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = GradientBlue,
                strokeWidth = 2.dp
            )
        }
    } else if (searchResults.isNotEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CryptoDarkSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "Search Results",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                
                searchResults.take(10).forEach { result ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTokenClick(result.symbol, result.exchanges) },
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result.symbol,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                
                                // Exchange chips
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    result.exchanges.forEach { exchange ->
                                        val chipColor = when (exchange.lowercase()) {
                                            "binance" -> Color(0xFFF3BA2F)
                                            "bybit" -> Color(0xFFF7A600)
                                            "mexc" -> GradientBlue
                                            else -> TextSecondary
                                        }
                                        
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = chipColor.copy(alpha = 0.2f),
                                            modifier = Modifier.padding(0.dp)
                                        ) {
                                            Text(
                                                text = exchange.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = chipColor,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buy",
                                tint = NeonGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    if (result != searchResults.take(10).last()) {
                        HorizontalDivider(
                            color = TextSecondary.copy(alpha = 0.1f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
                
                if (searchResults.size > 10) {
                    Text(
                        text = "Showing 10 of ${searchResults.size} results",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(8.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ExchangeSelectionDialog(
    symbol: String,
    exchanges: List<String>,
    usdtBalances: Map<String, Double>,
    onExchangeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CryptoDarkSurface,
        tonalElevation = 8.dp,
        title = {
            Column {
                Text(
                    text = "Select Exchange",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Choose where to buy $symbol",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                exchanges.forEach { exchange ->
                    val exchangeLower = exchange.lowercase()
                    val balance = usdtBalances[exchangeLower] ?: 0.0
                    val chipColor = when (exchangeLower) {
                        "binance" -> Color(0xFFF3BA2F)
                        "bybit" -> Color(0xFFF7A600)
                        "mexc" -> GradientBlue
                        else -> TextSecondary
                    }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExchangeSelected(exchangeLower) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = chipColor.copy(alpha = 0.1f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = exchange.uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = chipColor
                                )
                                Text(
                                    text = "Available: $${String.format("%.2f", balance)} USDT",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            
                            if (balance > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = NeonGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Ready",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ElectricRed.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "No USDT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricRed,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
private fun FuturesContent(
    futuresPositions: Map<String, List<FuturesPosition>>,
    onClosePosition: (String, String, Double) -> Unit,
    isClosing: Boolean,
    onRefresh: () -> Unit
) {
    val allPositions = futuresPositions.values.flatten()
    val totalPnL = allPositions.sumOf { it.unrealized_pnl }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Futures Header
        item {
            FuturesHeader(
                totalPnL = totalPnL,
                positionCount = allPositions.size,
                onRefresh = onRefresh
            )
        }
        
        // Positions by Exchange
        futuresPositions.forEach { (exchange, positions) ->
            if (positions.isNotEmpty()) {
                item {
                    Text(
                        text = "${exchange.uppercase()} Positions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                items(positions) { position ->
                    EnhancedPositionCard(
                        position = position,
                        exchange = exchange,
                        onCloseClick = onClosePosition,
                        isClosing = isClosing
                    )
                }
            }
        }
        
        if (allPositions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No open positions",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun FuturesHeader(
    totalPnL: Double,
    positionCount: Int,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CryptoDarkSurface,
                        CryptoDarkBackground
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Futures",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = GradientBlue
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total PnL",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "$${String.format("%,.2f", totalPnL)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (totalPnL >= 0) NeonGreen else ElectricRed
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Open Positions",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = positionCount.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryContent(
    exchangeTotals: Map<String, Double>,
    viewModel: PortfolioViewModel
) {
    val historyData by viewModel.historyData.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.fetchHistory()
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Portfolio History",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        // Portfolio Value Chart (Last 24 hours)
        item {
            historyData?.let { history ->
                if (history.minute_snapshots.isNotEmpty()) {
                    val values = history.minute_snapshots.map { it.total_usd_value }
                    val minValue = values.minOrNull() ?: 0.0
                    val maxValue = values.maxOrNull() ?: 0.0
                    val change = if (values.size >= 2) {
                        ((values.last() - values.first()) / values.first() * 100)
                    } else 0.0
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = CryptoDarkSurface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "24 Hour Performance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Min: $${String.format("%,.2f", minValue)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "Max: $${String.format("%,.2f", maxValue)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    text = "${if (change >= 0) "+" else ""}${String.format("%.2f", change)}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (change >= 0) NeonGreen else ElectricRed
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            SimpleLineChart(
                                data = values,
                                color = if (change >= 0) NeonGreen else ElectricRed,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                    }
                } else {
                    // No data available - show helpful message
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = CryptoDarkSurface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "📊",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No History Data Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Portfolio history will appear here once data is collected.\nCheck back in a few minutes!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } ?: run {
                // Loading state
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CryptoDarkSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = GradientBlue)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading history...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
        
        // Exchange Distribution
        item {
            ExchangeDistributionChart(exchangeTotals = exchangeTotals)
        }
    }
}
