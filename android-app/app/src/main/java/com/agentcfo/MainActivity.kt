package com.agentcfo

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.agentcfo.auth.AuthenticationState
import com.agentcfo.ui.BiometricLockScreen
import com.agentcfo.ui.auth.LoginScreen
import com.agentcfo.ui.auth.RegisterScreen
import com.agentcfo.ui.auth.WelcomeScreen
import com.agentcfo.ui.camera.CameraScreen
import com.agentcfo.ui.documents.DocumentDetailScreen
import com.agentcfo.ui.documents.DocumentUploadScreen
import com.agentcfo.ui.documents.DocumentsScreen
import com.agentcfo.ui.theme.AgentCFOTheme
import com.agentcfo.utils.FileUtils
import com.agentcfo.viewmodel.AuthState
import com.agentcfo.viewmodel.AuthViewModel
import com.agentcfo.viewmodel.DocumentViewModel
import kotlinx.coroutines.launch

/**
 * Main Activity - Entry point of the app
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AgentCFOTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Reset authentication state when app is destroyed
        AuthenticationState.reset()
    }
}

/**
 * App navigation setup
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val documentViewModel: DocumentViewModel = viewModel()
    
    val authState by authViewModel.authState.collectAsState()
    val documentsState by documentViewModel.documentsState.collectAsState()
    val documents by documentViewModel.documents.collectAsState()
    val uploadState by documentViewModel.uploadState.collectAsState()
    val selectedDocument by documentViewModel.selectedDocument.collectAsState()
    
    val scope = rememberCoroutineScope()
    
    // Determine start destination
    val startDestination = remember {
        scope.launch {
            if (authViewModel.isLoggedIn()) {
                "biometric"
            } else {
                "welcome"
            }
        }
        "welcome"  // Default
    }
    
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
                // Reset authentication
                AuthenticationState.reset()
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
            
            RegisterScreen(
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
                // Show main documents screen when biometric auth passed
                DocumentsScreen(
                    documents = documents,
                    documentsState = documentsState,
                    onDocumentClick = { document ->
                        documentViewModel.loadDocument(document.id)
                        navController.navigate("document/${document.id}")
                    },
                    onAddDocument = {
                        navController.navigate("upload")
                    },
                    onRefresh = {
                        documentViewModel.loadDocuments()
                    }
                )
            } else {
                // Show biometric lock screen
                BiometricLockScreen(
                    onAuthSuccess = {
                        AuthenticationState.setAuthenticated(true)
                        // Load documents
                        documentViewModel.loadDocuments()
                    }
                )
            }
        }
        
        // Document Detail Screen
        composable(
            route = "document/{documentId}",
            arguments = listOf(navArgument("documentId") { type = NavType.IntType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getInt("documentId")
            
            LaunchedEffect(documentId) {
                documentId?.let {
                    documentViewModel.loadDocument(it)
                }
            }
            
            DocumentDetailScreen(
                document = selectedDocument,
                onNavigateBack = {
                    documentViewModel.clearSelectedDocument()
                    navController.popBackStack()
                },
                onDelete = { id ->
                    documentViewModel.deleteDocument(id)
                },
                onDownload = { id ->
                    // TODO: Implement download
                }
            )
        }
        
        // Document Upload Screen
        composable("upload") {
            DocumentUploadScreen(
                onNavigateBack = {
                    documentViewModel.resetUploadState()
                    navController.popBackStack()
                },
                onNavigateToCamera = {
                    navController.navigate("camera")
                },
                onUpload = { uri, documentType ->
                    // Convert URI to File and upload
                    val context = navController.context
                    val file = FileUtils.uriToFile(context, uri)
                    file?.let {
                        val compressedFile = FileUtils.compressImage(it) ?: it
                        documentViewModel.uploadDocument(compressedFile, documentType)
                    }
                },
                uploadState = uploadState
            )
            
            // Navigate back on upload success
            LaunchedEffect(uploadState) {
                if (uploadState is com.agentcfo.viewmodel.UploadState.Success) {
                    navController.popBackStack()
                    documentViewModel.resetUploadState()
                }
            }
        }
        
        // Camera Screen
        composable("camera") {
            CameraScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPhotoTaken = { uri ->
                    // Navigate back to upload screen with photo
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("photo_uri", uri.toString())
                    navController.popBackStack()
                }
            )
        }
    }
}

