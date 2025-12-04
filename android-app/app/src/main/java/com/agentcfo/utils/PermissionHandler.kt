package com.agentcfo.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Permission state
 */
sealed class PermissionState {
    object Granted : PermissionState()
    object Denied : PermissionState()
    object PermanentlyDenied : PermissionState()
    object NotRequested : PermissionState()
}

/**
 * Check if a permission is granted
 */
fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Get required storage permission based on Android version
 */
fun getStoragePermission(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

/**
 * Composable to handle camera permission
 */
@Composable
fun rememberCameraPermissionState(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {}
): Pair<PermissionState, () -> Unit> {
    val context = LocalContext.current
    var permissionState by remember { mutableStateOf<PermissionState>(PermissionState.NotRequested) }
    
    // Check initial permission state
    LaunchedEffect(Unit) {
        permissionState = when {
            context.hasPermission(Manifest.permission.CAMERA) -> PermissionState.Granted
            else -> PermissionState.NotRequested
        }
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionState = if (isGranted) {
            PermissionState.Granted
        } else {
            PermissionState.Denied
        }
        
        if (isGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }
    
    val requestPermission: () -> Unit = {
        if (!context.hasPermission(Manifest.permission.CAMERA)) {
            launcher.launch(Manifest.permission.CAMERA)
        } else {
            permissionState = PermissionState.Granted
            onPermissionGranted()
        }
    }
    
    return Pair(permissionState, requestPermission)
}

/**
 * Composable to handle storage permission
 */
@Composable
fun rememberStoragePermissionState(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {}
): Pair<PermissionState, () -> Unit> {
    val context = LocalContext.current
    var permissionState by remember { mutableStateOf<PermissionState>(PermissionState.NotRequested) }
    val permission = getStoragePermission()
    
    // Check initial permission state
    LaunchedEffect(Unit) {
        permissionState = when {
            context.hasPermission(permission) -> PermissionState.Granted
            else -> PermissionState.NotRequested
        }
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionState = if (isGranted) {
            PermissionState.Granted
        } else {
            PermissionState.Denied
        }
        
        if (isGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }
    
    val requestPermission: () -> Unit = {
        if (!context.hasPermission(permission)) {
            launcher.launch(permission)
        } else {
            permissionState = PermissionState.Granted
            onPermissionGranted()
        }
    }
    
    return Pair(permissionState, requestPermission)
}

/**
 * Composable to handle multiple permissions
 */
@Composable
fun rememberMultiplePermissionsState(
    permissions: List<String>,
    onAllPermissionsGranted: () -> Unit = {},
    onPermissionsDenied: (List<String>) -> Unit = {}
): Pair<Map<String, PermissionState>, () -> Unit> {
    val context = LocalContext.current
    var permissionsState by remember { 
        mutableStateOf<Map<String, PermissionState>>(permissions.associateWith { PermissionState.NotRequested })
    }
    
    // Check initial permission states
    LaunchedEffect(Unit) {
        permissionsState = permissions.associateWith { permission ->
            if (context.hasPermission(permission)) {
                PermissionState.Granted
            } else {
                PermissionState.NotRequested
            }
        }
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsState = results.mapValues { (_, isGranted) ->
            if (isGranted) PermissionState.Granted else PermissionState.Denied
        }
        
        val deniedPermissions = results.filter { !it.value }.keys.toList()
        
        if (deniedPermissions.isEmpty()) {
            onAllPermissionsGranted()
        } else {
            onPermissionsDenied(deniedPermissions)
        }
    }
    
    val requestPermissions: () -> Unit = {
        val notGrantedPermissions = permissions.filter { !context.hasPermission(it) }
        
        if (notGrantedPermissions.isEmpty()) {
            onAllPermissionsGranted()
        } else {
            launcher.launch(notGrantedPermissions.toTypedArray())
        }
    }
    
    return Pair(permissionsState, requestPermissions)
}

