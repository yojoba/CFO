package com.agentcfo.data

import com.agentcfo.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for chat operations
 */
class ChatRepository {
    
    private val apiService = RetrofitClient.apiService
    
    /**
     * Send message to accountant agent
     */
    suspend fun chatWithAccountant(
        message: String,
        conversationId: Int? = null
    ): Result<ChatResponse> = withContext(Dispatchers.IO) {
        try {
            val request = ChatRequest(message, conversationId)
            val response = apiService.chatWithAccountant(request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Chat failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send message to legal agent
     */
    suspend fun chatWithLegal(
        message: String,
        conversationId: Int? = null
    ): Result<ChatResponse> = withContext(Dispatchers.IO) {
        try {
            val request = ChatRequest(message, conversationId)
            val response = apiService.chatWithLegal(request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Chat failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all conversations
     */
    suspend fun getConversations(
        agentType: AgentType? = null
    ): Result<List<ConversationResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getConversations(agentType?.name?.lowercase())
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to fetch conversations"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get conversation by ID
     */
    suspend fun getConversation(conversationId: Int): Result<ConversationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getConversation(conversationId)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to fetch conversation"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete conversation
     */
    suspend fun deleteConversation(conversationId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteConversation(conversationId)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to delete conversation"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

