package com.crypto.wallet.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crypto.wallet.data.models.Coin
import com.crypto.wallet.data.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CryptoViewModel @Inject constructor(
    private val repository: CryptoRepository
) : ViewModel() {

    val coins: LiveData<List<Coin>> = repository.coins
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun refreshData() {
        viewModelScope.launch {
            try {
                repository.refreshCoins()
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            }
        }
    }
}
