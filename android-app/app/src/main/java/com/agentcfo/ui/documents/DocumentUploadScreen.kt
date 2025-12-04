package com.agentcfo.ui.documents

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.agentcfo.R
import com.agentcfo.network.DocumentType
import com.agentcfo.utils.FileUtils
import com.agentcfo.utils.rememberCameraPermissionState
import com.agentcfo.utils.rememberStoragePermissionState
import com.agentcfo.viewmodel.UploadState

/**
 * Document upload screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentUploadScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onUpload: (Uri, DocumentType?) -> Unit,
    uploadState: UploadState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedDocumentType by remember { mutableStateOf<DocumentType?>(null) }
    var showDocumentTypePicker by remember { mutableStateOf(false) }
    
    // Storage permission for gallery
    val (storagePermissionState, requestStoragePermission) = rememberStoragePermissionState()
    
    // Camera permission
    val (cameraPermissionState, requestCameraPermission) = rememberCameraPermissionState(
        onPermissionGranted = { onNavigateToCamera() },
        onPermissionDenied = { /* Show message */ }
    )
    
    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedImageUri = it }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.upload_document)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedImageUri != null) {
                // Show selected image preview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "Selected document",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                
                // Document type picker
                OutlinedButton(
                    onClick = { showDocumentTypePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedDocumentType?.name?.replace("_", " ")?.lowercase()
                            ?.replaceFirstChar { it.uppercase() }
                            ?: "Sélectionner le type de document"
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Upload button
                Button(
                    onClick = {
                        selectedImageUri?.let { uri ->
                            onUpload(uri, selectedDocumentType)
                        }
                    },
                    enabled = uploadState !is UploadState.Uploading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uploadState is UploadState.Uploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = when (uploadState) {
                            is UploadState.Uploading -> stringResource(R.string.uploading)
                            else -> "Uploader"
                        }
                    )
                }
                
                // Error message
                if (uploadState is UploadState.Error) {
                    Text(
                        text = uploadState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Change image button
                OutlinedButton(
                    onClick = { selectedImageUri = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Choisir une autre image")
                }
            } else {
                // Show source selection buttons
                Text(
                    text = "Choisissez la source",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
                
                // Camera button
                Button(
                    onClick = { requestCameraPermission() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    Text(
                        text = "📷",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.take_photo),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                // Gallery button
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    Text(
                        text = "🖼️",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.choose_from_gallery),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
    
    // Document type picker dialog
    if (showDocumentTypePicker) {
        AlertDialog(
            onDismissRequest = { showDocumentTypePicker = false },
            title = { Text(stringResource(R.string.document_type)) },
            text = {
                Column {
                    DocumentType.values().forEach { type ->
                        TextButton(
                            onClick = {
                                selectedDocumentType = type
                                showDocumentTypePicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = type.name.replace("_", " ").lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDocumentTypePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

