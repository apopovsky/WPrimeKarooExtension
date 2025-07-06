package com.itl.wprimeextension.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.itl.wprimeextension.data.WPrimeConfiguration
import com.itl.wprimeextension.data.WPrimeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WPrimeConfigViewModel(
    private val wPrimeSettings: WPrimeSettings,
) : ViewModel() {

    private val _configuration = MutableStateFlow(WPrimeConfiguration())
    val configuration: StateFlow<WPrimeConfiguration> = _configuration.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadConfiguration()
    }

    private fun loadConfiguration() {
        viewModelScope.launch {
            wPrimeSettings.configuration.collect { config ->
                _configuration.value = config
                _isLoading.value = false
            }
        }
    }

    fun updateCriticalPower(power: Double) {
        viewModelScope.launch {
            wPrimeSettings.updateCriticalPower(power)
        }
    }

    fun updateAnaerobicCapacity(capacity: Double) {
        viewModelScope.launch {
            wPrimeSettings.updateAnaerobicCapacity(capacity)
        }
    }

    fun updateTauRecovery(tau: Double) {
        viewModelScope.launch {
            wPrimeSettings.updateTauRecovery(tau)
        }
    }
}

class WPrimeConfigViewModelFactory(
    private val wPrimeSettings: WPrimeSettings,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WPrimeConfigViewModel::class.java)) {
            return WPrimeConfigViewModel(wPrimeSettings) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
