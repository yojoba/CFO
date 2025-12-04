package com.agentcfo.data

import com.agentcfo.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import java.io.File

/**
 * Repository for document operations
 */
class DocumentRepository {
    
    private val apiService = RetrofitClient.apiService
    
    /**
     * Upload a document
     */
    suspend fun uploadDocument(
        file: File,
        documentType: DocumentType? = null
    ): Result<DocumentUploadResponse> = withContext(Dispatchers.IO) {
        try {
            // Create multipart body part for file
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData(
                "file",
                file.name,
                requestFile
            )
            
            val response = apiService.uploadDocument(
                file = filePart,
                documentType = documentType?.name?.lowercase()
            )
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Upload failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all documents
     */
    suspend fun getDocuments(
        skip: Int = 0,
        limit: Int = 100
    ): Result<List<DocumentResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDocuments(skip, limit)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to fetch documents"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get document by ID
     */
    suspend fun getDocument(documentId: Int): Result<DocumentResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDocument(documentId)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to fetch document"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update document metadata
     */
    suspend fun updateDocument(
        documentId: Int,
        update: DocumentUpdate
    ): Result<DocumentResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateDocument(documentId, update)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to update document"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete document
     */
    suspend fun deleteDocument(documentId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteDocument(documentId)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to delete document"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Download document preview
     */
    suspend fun getDocumentPreview(documentId: Int): Result<ResponseBody> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDocumentPreview(documentId)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to fetch preview"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get documents sorted by importance
     */
    suspend fun getDocumentsByImportance(
        skip: Int = 0,
        limit: Int = 100
    ): Result<List<DocumentResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDocumentsByImportance(skip, limit)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to fetch documents"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get documents sorted by deadline
     */
    suspend fun getDocumentsByDeadline(
        skip: Int = 0,
        limit: Int = 100
    ): Result<List<DocumentResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDocumentsByDeadline(skip, limit)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to fetch documents"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get urgent documents
     */
    suspend fun getUrgentDocuments(): Result<List<DocumentResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUrgentDocuments()
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to fetch urgent documents"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Search documents
     */
    suspend fun searchDocuments(query: String): Result<List<DocumentResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchDocuments(query)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Search failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get document statistics
     */
    suspend fun getStatistics(): Result<DocumentStatistics> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDocumentStatistics()
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to fetch statistics"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all categories
     */
    suspend fun getCategories(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCategories()
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to fetch categories"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

