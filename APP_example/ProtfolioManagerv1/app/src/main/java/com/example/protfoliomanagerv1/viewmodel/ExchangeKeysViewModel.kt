package com.example.protfoliomanagerv1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.protfoliomanagerv1.auth.TokenManager
import com.example.protfoliomanagerv1.data.ExchangeKeysRepository
import com.example.protfoliomanagerv1.network.ExchangeKey
import com.example.protfoliomanagerv1.network.TestConnectionResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExchangeKeysViewModel(application: Application) : AndroidViewModel(application) {
    
    private val tokenManager = TokenManager(application)
    private val token = tokenManager.getToken() ?: ""
    private val repository = ExchangeKeysRepository(token)
    
    private val _exchangeKeys = MutableStateFlow<List<ExchangeKey>>(emptyList())
    val exchangeKeys = _exchangeKeys.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    private val _testResult = MutableStateFlow<TestConnectionResponse?>(null)
    val testResult = _testResult.asStateFlow()
    
    init {
        fetchExchangeKeys()
    }
    
    fun fetchExchangeKeys() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.getExchangeKeys()
                .onSuccess { keys ->
                    _exchangeKeys.value = keys
                }
                .onFailure { error ->
                    _errorMessage.value = error.localizedMessage ?: "Failed to fetch exchange keys"
                }
            
            _isLoading.value = false
        }
    }
    
    fun addExchangeKey(exchange: String, apiKey: String, apiSecret: String, label: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.addExchangeKey(exchange, apiKey, apiSecret, label)
                .onSuccess {
                    fetchExchangeKeys()
                }
                .onFailure { error ->
                    _errorMessage.value = error.localizedMessage ?: "Failed to add exchange key"
                }
            
            _isLoading.value = false
        }
    }
    
    fun deleteExchangeKey(keyId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.deleteExchangeKey(keyId)
                .onSuccess {
                    fetchExchangeKeys()
                }
                .onFailure { error ->
                    _errorMessage.value = error.localizedMessage ?: "Failed to delete exchange key"
                }
            
            _isLoading.value = false
        }
    }
    
    fun testConnection(keyId: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            
            repository.testConnection(keyId)
                .onSuccess { result ->
                    _testResult.value = result
                }
                .onFailure { error ->
                    _errorMessage.value = error.localizedMessage ?: "Failed to test connection"
                }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun clearTestResult() {
        _testResult.value = null
    }
}


