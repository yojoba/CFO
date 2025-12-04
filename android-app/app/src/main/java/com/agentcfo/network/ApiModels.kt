package com.agentcfo.network

import com.google.gson.annotations.SerializedName

/**
 * API Models matching backend schemas
 */

// ============================================================================
// Auth Models
// ============================================================================

data class RegisterRequest(
    val email: String,
    val password: String,
    @SerializedName("full_name")
    val fullName: String?
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class UserResponse(
    val id: Int,
    val email: String,
    @SerializedName("full_name")
    val fullName: String?,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("created_at")
    val createdAt: String
)

data class TokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    val user: UserResponse
)

// ============================================================================
// Document Models
// ============================================================================

enum class DocumentType {
    @SerializedName("invoice")
    INVOICE,
    @SerializedName("receipt")
    RECEIPT,
    @SerializedName("contract")
    CONTRACT,
    @SerializedName("letter")
    LETTER,
    @SerializedName("tax_document")
    TAX_DOCUMENT,
    @SerializedName("insurance")
    INSURANCE,
    @SerializedName("other")
    OTHER
}

enum class DocumentStatus {
    @SerializedName("uploading")
    UPLOADING,
    @SerializedName("processing")
    PROCESSING,
    @SerializedName("completed")
    COMPLETED,
    @SerializedName("failed")
    FAILED
}

data class DocumentResponse(
    val id: Int,
    val filename: String,
    @SerializedName("original_filename")
    val originalFilename: String,
    @SerializedName("display_name")
    val displayName: String?,
    @SerializedName("file_size")
    val fileSize: Int?,
    @SerializedName("document_type")
    val documentType: DocumentType,
    val status: DocumentStatus,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("extracted_text")
    val extractedText: String?,
    
    // Filing cabinet fields
    @SerializedName("storage_year")
    val storageYear: Int?,
    val category: String?,
    @SerializedName("ocr_pdf_path")
    val ocrPdfPath: String?,
    
    // Intelligence documentaire fields
    @SerializedName("importance_score")
    val importanceScore: Float?,
    @SerializedName("document_date")
    val documentDate: String?,
    val deadline: String?,
    @SerializedName("extracted_amount")
    val extractedAmount: Float?,
    val currency: String?,
    val keywords: String?,  // JSON string
    @SerializedName("classification_confidence")
    val classificationConfidence: Float?,
    
    // Duplicate detection
    @SerializedName("is_duplicate")
    val isDuplicate: Boolean?,
    @SerializedName("duplicate_of_id")
    val duplicateOfId: Int?,
    @SerializedName("similarity_score")
    val similarityScore: Float?
)

data class DocumentUploadResponse(
    val message: String,
    val document: DocumentResponse
)

data class DocumentUpdate(
    @SerializedName("display_name")
    val displayName: String? = null,
    @SerializedName("document_type")
    val documentType: String? = null,
    val category: String? = null,
    @SerializedName("importance_score")
    val importanceScore: Float? = null,
    @SerializedName("document_date")
    val documentDate: String? = null,
    val deadline: String? = null,
    @SerializedName("extracted_amount")
    val extractedAmount: Float? = null,
    val currency: String? = null
)

data class DocumentStatistics(
    @SerializedName("total_documents")
    val totalDocuments: Int,
    @SerializedName("documents_by_type")
    val documentsByType: Map<String, Int>,
    @SerializedName("documents_with_deadline")
    val documentsWithDeadline: Int,
    @SerializedName("overdue_documents")
    val overdueDocuments: Int,
    @SerializedName("upcoming_deadlines")
    val upcomingDeadlines: Int,
    @SerializedName("high_importance_documents")
    val highImportanceDocuments: Int,
    @SerializedName("average_importance_score")
    val averageImportanceScore: Float,
    @SerializedName("total_amount_extracted")
    val totalAmountExtracted: Float
)

// ============================================================================
// Chat Models
// ============================================================================

enum class AgentType {
    @SerializedName("accountant")
    ACCOUNTANT,
    @SerializedName("legal")
    LEGAL
}

enum class MessageRole {
    @SerializedName("user")
    USER,
    @SerializedName("assistant")
    ASSISTANT,
    @SerializedName("system")
    SYSTEM
}

data class ChatRequest(
    val message: String,
    @SerializedName("conversation_id")
    val conversationId: Int? = null
)

data class ChatResponse(
    @SerializedName("conversation_id")
    val conversationId: Int,
    val message: String,
    @SerializedName("agent_type")
    val agentType: AgentType
)

data class MessageResponse(
    val role: MessageRole,
    val content: String,
    @SerializedName("created_at")
    val createdAt: String
)

data class ConversationResponse(
    val id: Int,
    @SerializedName("agent_type")
    val agentType: AgentType,
    val title: String?,
    @SerializedName("created_at")
    val createdAt: String,
    val messages: List<MessageResponse>
)

// ============================================================================
// Error Models
// ============================================================================

data class ApiError(
    val detail: String
)

// ============================================================================
// Generic API Response
// ============================================================================

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

