package com.example.protfoliomanagerv1

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.protfoliomanagerv1.auth.AuthenticationState
import com.example.protfoliomanagerv1.auth.TokenManager
import com.example.protfoliomanagerv1.ui.BiometricLockScreen
import com.example.protfoliomanagerv1.ui.PortfolioScreen
import com.example.protfoliomanagerv1.ui.auth.WelcomeScreen
import com.example.protfoliomanagerv1.ui.auth.LoginScreen
import com.example.protfoliomanagerv1.ui.auth.RegistrationScreen
import com.example.protfoliomanagerv1.ui.settings.SettingsScreen
import com.example.protfoliomanagerv1.ui.settings.ExchangeKeysScreen
import com.example.protfoliomanagerv1.ui.settings.AddExchangeKeyDialog
import com.example.protfoliomanagerv1.ui.settings.ExternalAssetsScreen
import com.example.protfoliomanagerv1.ui.settings.AddExternalAssetDialog
import com.example.protfoliomanagerv1.ui.theme.ProtfolioManagerv1Theme
import com.example.protfoliomanagerv1.viewmodel.AuthViewModel
import com.example.protfoliomanagerv1.viewmodel.AuthState
import com.example.protfoliomanagerv1.viewmodel.ExchangeKeysViewModel
import com.example.protfoliomanagerv1.viewmodel.ExternalAssetsViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProtfolioManagerv1Theme {
                AppNavigation()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Reset authentication state when app is destroyed
        AuthenticationState.reset()
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    val tokenManager = TokenManager(androidx.compose.ui.platform.LocalContext.current)
    
    // Determine start destination based on login status
    val startDestination = if (tokenManager.isLoggedIn()) "biometric" else "welcome"
    
    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                // Navigate to biometric lock after successful login/register
                navController.navigate("biometric") {
                    popUpTo("welcome") { inclusive = true }
                }
            }
            is AuthState.Unauthenticated -> {
                // Navigate to welcome screen after logout
                navController.navigate("welcome") {
                    popUpTo(0) { inclusive = true }
                }
            }
            else -> {}
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Welcome Screen
        composable("welcome") {
            WelcomeScreen(
                onNavigateToLogin = {
                    navController.navigate("login")
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }
        
        // Login Screen
        composable("login") {
            val isLoading = authState is AuthState.Loading
            val errorMessage = (authState as? AuthState.Error)?.message
            
            LoginScreen(
                onNavigateBack = {
                    navController.popBackStack()
                    authViewModel.resetAuthState()
                },
                onNavigateToRegister = {
                    navController.navigate("register") {
                        popUpTo("login") { inclusive = true }
                    }
                    authViewModel.resetAuthState()
                },
                onLoginSuccess = {
                    // Handled by LaunchedEffect above
                },
                onLogin = { email, password ->
                    authViewModel.login(email, password)
                },
                isLoading = isLoading,
                errorMessage = errorMessage
            )
        }
        
        // Registration Screen
        composable("register") {
            val isLoading = authState is AuthState.Loading
            val errorMessage = (authState as? AuthState.Error)?.message
            
            RegistrationScreen(
                onNavigateBack = {
                    navController.popBackStack()
                    authViewModel.resetAuthState()
                },
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                    authViewModel.resetAuthState()
                },
                onRegister = { email, password, fullName ->
                    authViewModel.register(email, password, fullName)
                },
                isLoading = isLoading,
                errorMessage = errorMessage
            )
        }
        
        // Biometric Lock Screen (after login)
        composable("biometric") {
            val isBiometricAuthenticated by AuthenticationState.isAuthenticated
            
            if (isBiometricAuthenticated) {
                // Show main portfolio screen when biometric auth passed
                PortfolioScreen(
                    modifier = Modifier.fillMaxSize(),
                    onLogout = {
                        authViewModel.logout()
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            } else {
                // Show biometric lock screen
                BiometricLockScreen(
                    onAuthSuccess = {
                        AuthenticationState.setAuthenticated(true)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Settings Screen
        composable("settings") {
            val currentUser by authViewModel.currentUser.collectAsState()
            val authState by authViewModel.authState.collectAsState()
            val isLoading = authState is AuthState.Loading
            val errorMessage = (authState as? AuthState.Error)?.message
            
            SettingsScreen(
                userEmail = currentUser?.email,
                userName = currentUser?.full_name,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToExchangeKeys = {
                    navController.navigate("exchange-keys")
                },
                onNavigateToExternalAssets = {
                    navController.navigate("external-assets")
                },
                onLogout = {
                    authViewModel.logout()
                },
                onChangePassword = { oldPassword, newPassword ->
                    authViewModel.changePassword(oldPassword, newPassword)
                },
                onDeleteAccount = { password ->
                    authViewModel.deleteAccount(password)
                },
                isLoading = isLoading,
                errorMessage = errorMessage
            )
        }
        
        // Exchange Keys Screen
        composable("exchange-keys") {
            val keysViewModel: ExchangeKeysViewModel = viewModel()
            val exchangeKeys by keysViewModel.exchangeKeys.collectAsState()
            val isLoading by keysViewModel.isLoading.collectAsState()
            val errorMessage by keysViewModel.errorMessage.collectAsState()
            val testResult by keysViewModel.testResult.collectAsState()
            var showAddDialog by remember { mutableStateOf(false) }
            
            ExchangeKeysScreen(
                exchangeKeys = exchangeKeys,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onAddKey = {
                    showAddDialog = true
                },
                onDeleteKey = { keyId ->
                    keysViewModel.deleteExchangeKey(keyId)
                },
                onTestConnection = { keyId ->
                    keysViewModel.testConnection(keyId)
                },
                isLoading = isLoading
            )
            
            if (showAddDialog) {
                AddExchangeKeyDialog(
                    onDismiss = {
                        showAddDialog = false
                        keysViewModel.clearError()
                    },
                    onAdd = { exchange, apiKey, apiSecret, label ->
                        keysViewModel.addExchangeKey(exchange, apiKey, apiSecret, label)
                        showAddDialog = false
                    },
                    isLoading = isLoading,
                    errorMessage = errorMessage
                )
            }
            
            // Show test result
            testResult?.let { result ->
                androidx.compose.runtime.LaunchedEffect(result) {
                    // Show snackbar or dialog with result
                    keysViewModel.clearTestResult()
                }
            }
        }
        
        // External Assets Screen
        composable("external-assets") {
            val assetsViewModel: ExternalAssetsViewModel = viewModel()
            val externalAssets by assetsViewModel.externalAssets.collectAsState()
            val summary by assetsViewModel.summary.collectAsState()
            val isLoading by assetsViewModel.isLoading.collectAsState()
            val errorMessage by assetsViewModel.errorMessage.collectAsState()
            var showAddDialog by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                assetsViewModel.fetchSummary()
            }
            
            ExternalAssetsScreen(
                externalAssets = externalAssets,
                totalValue = summary?.total_usd_value ?: 0.0,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onAddAsset = {
                    showAddDialog = true
                },
                onEditAsset = { assetId ->
                    // TODO: Implement edit dialog
                },
                onDeleteAsset = { assetId ->
                    assetsViewModel.deleteExternalAsset(assetId)
                },
                isLoading = isLoading
            )
            
            if (showAddDialog) {
                AddExternalAssetDialog(
                    onDismiss = {
                        showAddDialog = false
                        assetsViewModel.clearError()
                    },
                    onAdd = { symbol, quantity, label, notes ->
                        assetsViewModel.addExternalAsset(symbol, quantity, label, notes)
                        showAddDialog = false
                    },
                    isLoading = isLoading,
                    errorMessage = errorMessage
                )
            }
        }
    }
}

