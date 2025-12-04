package com.agentcfo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentcfo.data.ChatRepository
import com.agentcfo.network.AgentType
import com.agentcfo.network.ChatResponse
import com.agentcfo.network.ConversationResponse
import com.agentcfo.network.MessageResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for chat operations
 */
class ChatViewModel : ViewModel() {
    
    private val repository = ChatRepository()
    
    private val _chatState = MutableStateFlow<ChatState>(ChatState.Idle)
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()
    
    private val _messages = MutableStateFlow<List<MessageResponse>>(emptyList())
    val messages: StateFlow<List<MessageResponse>> = _messages.asStateFlow()
    
    private val _currentConversationId = MutableStateFlow<Int?>(null)
    val currentConversationId: StateFlow<Int?> = _currentConversationId.asStateFlow()
    
    private val _conversations = MutableStateFlow<List<ConversationResponse>>(emptyList())
    val conversations: StateFlow<List<ConversationResponse>> = _conversations.asStateFlow()
    
    /**
     * Send message to accountant agent
     */
    fun sendToAccountant(message: String) {
        viewModelScope.launch {
            _chatState.value = ChatState.Loading
            
            val result = repository.chatWithAccountant(
                message = message,
                conversationId = _currentConversationId.value
            )
            
            result.fold(
                onSuccess = { response ->
                    _currentConversationId.value = response.conversationId
                    _chatState.value = ChatState.Success(response.message)
                    // Reload conversation to get updated messages
                    loadConversation(response.conversationId)
                },
                onFailure = { error ->
                    _chatState.value = ChatState.Error(
                        error.message ?: "Chat failed"
                    )
                }
            )
        }
    }
    
    /**
     * Send message to legal agent
     */
    fun sendToLegal(message: String) {
        viewModelScope.launch {
            _chatState.value = ChatState.Loading
            
            val result = repository.chatWithLegal(
                message = message,
                conversationId = _currentConversationId.value
            )
            
            result.fold(
                onSuccess = { response ->
                    _currentConversationId.value = response.conversationId
                    _chatState.value = ChatState.Success(response.message)
                    // Reload conversation to get updated messages
                    loadConversation(response.conversationId)
                },
                onFailure = { error ->
                    _chatState.value = ChatState.Error(
                        error.message ?: "Chat failed"
                    )
                }
            )
        }
    }
    
    /**
     * Load a conversation
     */
    fun loadConversation(conversationId: Int) {
        viewModelScope.launch {
            val result = repository.getConversation(conversationId)
            
            result.fold(
                onSuccess = { conversation ->
                    _messages.value = conversation.messages
                    _currentConversationId.value = conversation.id
                },
                onFailure = { error ->
                    _chatState.value = ChatState.Error(
                        error.message ?: "Failed to load conversation"
                    )
                }
            )
        }
    }
    
    /**
     * Load all conversations for an agent type
     */
    fun loadConversations(agentType: AgentType? = null) {
        viewModelScope.launch {
            val result = repository.getConversations(agentType)
            
            result.fold(
                onSuccess = { convs ->
                    _conversations.value = convs
                },
                onFailure = { error ->
                    // Silently fail
                }
            )
        }
    }
    
    /**
     * Start new conversation
     */
    fun startNewConversation() {
        _currentConversationId.value = null
        _messages.value = emptyList()
        _chatState.value = ChatState.Idle
    }
    
    /**
     * Delete a conversation
     */
    fun deleteConversation(conversationId: Int) {
        viewModelScope.launch {
            val result = repository.deleteConversation(conversationId)
            
            result.fold(
                onSuccess = {
                    // Remove from local list
                    _conversations.value = _conversations.value.filter { it.id != conversationId }
                    
                    // If this was the current conversation, reset
                    if (_currentConversationId.value == conversationId) {
                        startNewConversation()
                    }
                },
                onFailure = { error ->
                    _chatState.value = ChatState.Error(
                        error.message ?: "Failed to delete conversation"
                    )
                }
            )
        }
    }
    
    /**
     * Reset chat state
     */
    fun resetChatState() {
        _chatState.value = ChatState.Idle
    }
}

/**
 * Chat states
 */
sealed class ChatState {
    object Idle : ChatState()
    object Loading : ChatState()
    data class Success(val message: String) : ChatState()
    data class Error(val message: String) : ChatState()
}

