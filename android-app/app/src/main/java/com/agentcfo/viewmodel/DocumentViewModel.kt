package com.agentcfo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentcfo.data.DocumentRepository
import com.agentcfo.network.DocumentResponse
import com.agentcfo.network.DocumentStatistics
import com.agentcfo.network.DocumentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for document operations
 */
class DocumentViewModel : ViewModel() {
    
    private val repository = DocumentRepository()
    
    private val _documentsState = MutableStateFlow<DocumentsState>(DocumentsState.Initial)
    val documentsState: StateFlow<DocumentsState> = _documentsState.asStateFlow()
    
    private val _documents = MutableStateFlow<List<DocumentResponse>>(emptyList())
    val documents: StateFlow<List<DocumentResponse>> = _documents.asStateFlow()
    
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()
    
    private val _selectedDocument = MutableStateFlow<DocumentResponse?>(null)
    val selectedDocument: StateFlow<DocumentResponse?> = _selectedDocument.asStateFlow()
    
    private val _statistics = MutableStateFlow<DocumentStatistics?>(null)
    val statistics: StateFlow<DocumentStatistics?> = _statistics.asStateFlow()
    
    /**
     * Load all documents
     */
    fun loadDocuments() {
        viewModelScope.launch {
            _documentsState.value = DocumentsState.Loading
            
            val result = repository.getDocuments()
            
            result.fold(
                onSuccess = { docs ->
                    _documents.value = docs
                    _documentsState.value = DocumentsState.Success
                },
                onFailure = { error ->
                    _documentsState.value = DocumentsState.Error(
                        error.message ?: "Failed to load documents"
                    )
                }
            )
        }
    }
    
    /**
     * Load documents sorted by importance
     */
    fun loadDocumentsByImportance() {
        viewModelScope.launch {
            _documentsState.value = DocumentsState.Loading
            
            val result = repository.getDocumentsByImportance()
            
            result.fold(
                onSuccess = { docs ->
                    _documents.value = docs
                    _documentsState.value = DocumentsState.Success
                },
                onFailure = { error ->
                    _documentsState.value = DocumentsState.Error(
                        error.message ?: "Failed to load documents"
                    )
                }
            )
        }
    }
    
    /**
     * Load documents sorted by deadline
     */
    fun loadDocumentsByDeadline() {
        viewModelScope.launch {
            _documentsState.value = DocumentsState.Loading
            
            val result = repository.getDocumentsByDeadline()
            
            result.fold(
                onSuccess = { docs ->
                    _documents.value = docs
                    _documentsState.value = DocumentsState.Success
                },
                onFailure = { error ->
                    _documentsState.value = DocumentsState.Error(
                        error.message ?: "Failed to load documents"
                    )
                }
            )
        }
    }
    
    /**
     * Load urgent documents
     */
    fun loadUrgentDocuments() {
        viewModelScope.launch {
            _documentsState.value = DocumentsState.Loading
            
            val result = repository.getUrgentDocuments()
            
            result.fold(
                onSuccess = { docs ->
                    _documents.value = docs
                    _documentsState.value = DocumentsState.Success
                },
                onFailure = { error ->
                    _documentsState.value = DocumentsState.Error(
                        error.message ?: "Failed to load urgent documents"
                    )
                }
            )
        }
    }
    
    /**
     * Search documents
     */
    fun searchDocuments(query: String) {
        viewModelScope.launch {
            _documentsState.value = DocumentsState.Loading
            
            val result = repository.searchDocuments(query)
            
            result.fold(
                onSuccess = { docs ->
                    _documents.value = docs
                    _documentsState.value = DocumentsState.Success
                },
                onFailure = { error ->
                    _documentsState.value = DocumentsState.Error(
                        error.message ?: "Search failed"
                    )
                }
            )
        }
    }
    
    /**
     * Upload a document
     */
    fun uploadDocument(file: File, documentType: DocumentType? = null) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Uploading(0f)
            
            val result = repository.uploadDocument(file, documentType)
            
            result.fold(
                onSuccess = { uploadResponse ->
                    _uploadState.value = UploadState.Success(uploadResponse.document)
                    // Reload documents list
                    loadDocuments()
                },
                onFailure = { error ->
                    _uploadState.value = UploadState.Error(
                        error.message ?: "Upload failed"
                    )
                }
            )
        }
    }
    
    /**
     * Load document by ID
     */
    fun loadDocument(documentId: Int) {
        viewModelScope.launch {
            val result = repository.getDocument(documentId)
            
            result.fold(
                onSuccess = { doc ->
                    _selectedDocument.value = doc
                },
                onFailure = { error ->
                    _documentsState.value = DocumentsState.Error(
                        error.message ?: "Failed to load document"
                    )
                }
            )
        }
    }
    
    /**
     * Delete a document
     */
    fun deleteDocument(documentId: Int) {
        viewModelScope.launch {
            val result = repository.deleteDocument(documentId)
            
            result.fold(
                onSuccess = {
                    // Remove from local list
                    _documents.value = _documents.value.filter { it.id != documentId }
                    _selectedDocument.value = null
                },
                onFailure = { error ->
                    _documentsState.value = DocumentsState.Error(
                        error.message ?: "Failed to delete document"
                    )
                }
            )
        }
    }
    
    /**
     * Load document statistics
     */
    fun loadStatistics() {
        viewModelScope.launch {
            val result = repository.getStatistics()
            
            result.fold(
                onSuccess = { stats ->
                    _statistics.value = stats
                },
                onFailure = { error ->
                    // Silently fail for statistics
                }
            )
        }
    }
    
    /**
     * Reset upload state
     */
    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }
    
    /**
     * Clear selected document
     */
    fun clearSelectedDocument() {
        _selectedDocument.value = null
    }
}

/**
 * Documents loading states
 */
sealed class DocumentsState {
    object Initial : DocumentsState()
    object Loading : DocumentsState()
    object Success : DocumentsState()
    data class Error(val message: String) : DocumentsState()
}

/**
 * Upload states
 */
sealed class UploadState {
    object Idle : UploadState()
    data class Uploading(val progress: Float) : UploadState()
    data class Success(val document: DocumentResponse) : UploadState()
    data class Error(val message: String) : UploadState()
}

