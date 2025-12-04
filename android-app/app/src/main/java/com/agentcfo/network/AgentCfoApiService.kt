package com.agentcfo.network

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service interface for AgentCFO backend
 */
interface AgentCfoApiService {
    
    // ========================================================================
    // Authentication Endpoints
    // ========================================================================
    
    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<TokenResponse>
    
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<TokenResponse>
    
    @GET("api/auth/me")
    suspend fun getCurrentUser(): Response<UserResponse>
    
    // ========================================================================
    // Document Endpoints
    // ========================================================================
    
    @Multipart
    @POST("api/documents/upload")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part,
        @Part("document_type") documentType: String? = null
    ): Response<DocumentUploadResponse>
    
    @GET("api/documents/")
    suspend fun getDocuments(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): Response<List<DocumentResponse>>
    
    @GET("api/documents/{id}")
    suspend fun getDocument(
        @Path("id") documentId: Int
    ): Response<DocumentResponse>
    
    @PATCH("api/documents/{id}")
    suspend fun updateDocument(
        @Path("id") documentId: Int,
        @Body update: DocumentUpdate
    ): Response<DocumentResponse>
    
    @DELETE("api/documents/{id}")
    suspend fun deleteDocument(
        @Path("id") documentId: Int
    ): Response<Unit>
    
    @GET("api/documents/{id}/download/original")
    suspend fun downloadOriginalDocument(
        @Path("id") documentId: Int
    ): Response<ResponseBody>
    
    @GET("api/documents/{id}/download/ocr-pdf")
    suspend fun downloadOcrPdf(
        @Path("id") documentId: Int
    ): Response<ResponseBody>
    
    @GET("api/documents/{id}/preview")
    suspend fun getDocumentPreview(
        @Path("id") documentId: Int
    ): Response<ResponseBody>
    
    @GET("api/documents/statistics")
    suspend fun getDocumentStatistics(): Response<DocumentStatistics>
    
    @GET("api/documents/by-importance")
    suspend fun getDocumentsByImportance(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): Response<List<DocumentResponse>>
    
    @GET("api/documents/by-deadline")
    suspend fun getDocumentsByDeadline(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): Response<List<DocumentResponse>>
    
    @GET("api/documents/urgent")
    suspend fun getUrgentDocuments(): Response<List<DocumentResponse>>
    
    @GET("api/documents/search")
    suspend fun searchDocuments(
        @Query("q") query: String
    ): Response<List<DocumentResponse>>
    
    @GET("api/documents/categories")
    suspend fun getCategories(): Response<List<String>>
    
    // ========================================================================
    // Chat Endpoints
    // ========================================================================
    
    @POST("api/chat/accountant")
    suspend fun chatWithAccountant(
        @Body request: ChatRequest
    ): Response<ChatResponse>
    
    @POST("api/chat/legal")
    suspend fun chatWithLegal(
        @Body request: ChatRequest
    ): Response<ChatResponse>
    
    @GET("api/chat/conversations")
    suspend fun getConversations(
        @Query("agent_type") agentType: String? = null
    ): Response<List<ConversationResponse>>
    
    @GET("api/chat/conversations/{id}")
    suspend fun getConversation(
        @Path("id") conversationId: Int
    ): Response<ConversationResponse>
    
    @DELETE("api/chat/conversations/{id}")
    suspend fun deleteConversation(
        @Path("id") conversationId: Int
    ): Response<Unit>
    
    // ========================================================================
    // Dashboard Endpoint
    // ========================================================================
    
    @GET("api/dashboard/")
    suspend fun getDashboardData(): Response<Map<String, Any>>
}

