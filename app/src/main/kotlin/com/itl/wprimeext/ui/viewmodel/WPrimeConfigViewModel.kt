package com.itl.wprimeext.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.itl.wprimeext.extension.WPrimeConfiguration
import com.itl.wprimeext.extension.WPrimeSettings
import com.itl.wprimeext.utils.LogConstants
import com.itl.wprimeext.utils.WPrimeLogger
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
        WPrimeLogger.d(WPrimeLogger.Module.VIEWMODEL, "WPrimeConfigViewModel initialized")
        loadConfiguration()
    }

    private fun loadConfiguration() {
        WPrimeLogger.d(WPrimeLogger.Module.VIEWMODEL, "Loading configuration from settings")
        viewModelScope.launch {
            wPrimeSettings.configuration.collect { config ->
                _configuration.value = config
                _isLoading.value = false
                WPrimeLogger.d(WPrimeLogger.Module.VIEWMODEL, "Configuration loaded and UI updated")
            }
        }
    }

    fun updateCriticalPower(power: Double) {
        WPrimeLogger.d(WPrimeLogger.Module.VIEWMODEL, LogConstants.UI_VALUES_CHANGED + " - CP: ${power}W")
        viewModelScope.launch { wPrimeSettings.updateCriticalPower(power) }
    }

    fun updateAnaerobicCapacity(capacity: Double) {
        WPrimeLogger.d(WPrimeLogger.Module.VIEWMODEL, LogConstants.UI_VALUES_CHANGED + " - W': ${capacity}J")
        viewModelScope.launch { wPrimeSettings.updateAnaerobicCapacity(capacity) }
    }

    fun updateTauRecovery(tau: Double) {
        WPrimeLogger.d(WPrimeLogger.Module.VIEWMODEL, LogConstants.UI_VALUES_CHANGED + " - Tau: ${tau}s")
        viewModelScope.launch { wPrimeSettings.updateTauRecovery(tau) }
    }

    fun updateRecordFit(enabled: Boolean) {
        WPrimeLogger.d(WPrimeLogger.Module.VIEWMODEL, LogConstants.UI_VALUES_CHANGED + " - recordFit: $enabled")
        viewModelScope.launch { wPrimeSettings.updateRecordFit(enabled) }
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
