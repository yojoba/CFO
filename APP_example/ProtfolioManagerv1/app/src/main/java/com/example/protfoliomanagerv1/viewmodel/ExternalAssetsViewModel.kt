package com.example.protfoliomanagerv1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.protfoliomanagerv1.auth.TokenManager
import com.example.protfoliomanagerv1.data.ExternalAssetsRepository
import com.example.protfoliomanagerv1.network.ExternalAsset
import com.example.protfoliomanagerv1.network.ExternalAssetsSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExternalAssetsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val tokenManager = TokenManager(application)
    private val token = tokenManager.getToken() ?: ""
    private val repository = ExternalAssetsRepository(token)
    
    private val _externalAssets = MutableStateFlow<List<ExternalAsset>>(emptyList())
    val externalAssets = _externalAssets.asStateFlow()
    
    private val _summary = MutableStateFlow<ExternalAssetsSummary?>(null)
    val summary = _summary.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    init {
        fetchExternalAssets()
    }
    
    fun fetchExternalAssets() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.getExternalAssets()
                .onSuccess { assets ->
                    _externalAssets.value = assets
                }
                .onFailure { error ->
                    _errorMessage.value = error.localizedMessage ?: "Failed to fetch external assets"
                }
            
            _isLoading.value = false
        }
    }
    
    fun fetchSummary() {
        viewModelScope.launch {
            repository.getExternalAssetsSummary()
                .onSuccess { summary ->
                    _summary.value = summary
                }
                .onFailure {
                    // Silently fail, not critical
                }
        }
    }
    
    fun addExternalAsset(
        assetSymbol: String,
        quantity: Double,
        label: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.addExternalAsset(assetSymbol, quantity, label, notes)
                .onSuccess {
                    fetchExternalAssets()
                    fetchSummary()
                }
                .onFailure { error ->
                    _errorMessage.value = error.localizedMessage ?: "Failed to add external asset"
                }
            
            _isLoading.value = false
        }
    }
    
    fun updateExternalAsset(
        assetId: String,
        quantity: Double?,
        label: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.updateExternalAsset(assetId, quantity, label, notes)
                .onSuccess {
                    fetchExternalAssets()
                    fetchSummary()
                }
                .onFailure { error ->
                    _errorMessage.value = error.localizedMessage ?: "Failed to update external asset"
                }
            
            _isLoading.value = false
        }
    }
    
    fun deleteExternalAsset(assetId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.deleteExternalAsset(assetId)
                .onSuccess {
                    fetchExternalAssets()
                    fetchSummary()
                }
                .onFailure { error ->
                    _errorMessage.value = error.localizedMessage ?: "Failed to delete external asset"
                }
            
            _isLoading.value = false
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}


