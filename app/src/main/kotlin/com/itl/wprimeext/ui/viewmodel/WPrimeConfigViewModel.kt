package com.itl.wprimeext.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.itl.wprimeext.extension.WPrimeAlert
import com.itl.wprimeext.extension.WPrimeConfiguration
import com.itl.wprimeext.extension.WPrimeModelType
import com.itl.wprimeext.extension.WPrimeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class WPrimeConfigViewModel(private val settings: WPrimeSettings) : ViewModel() {

    private val _configuration = MutableStateFlow(WPrimeConfiguration())
    val configuration: StateFlow<WPrimeConfiguration> = _configuration.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _configuration.value = settings.configuration.first()
            _isLoading.value = false
        }
    }

    fun updateCriticalPower(power: Double) {
        viewModelScope.launch {
            settings.updateCriticalPower(power)
            _configuration.value = _configuration.value.copy(criticalPower = power)
        }
    }

    fun updateAnaerobicCapacity(capacity: Double) {
        viewModelScope.launch {
            settings.updateAnaerobicCapacity(capacity)
            _configuration.value = _configuration.value.copy(anaerobicCapacity = capacity)
        }
    }

    fun updateTauRecovery(tau: Double) {
        viewModelScope.launch {
            settings.updateTauRecovery(tau)
            _configuration.value = _configuration.value.copy(tauRecovery = tau)
        }
    }

    fun updateKIn(kIn: Double) {
        viewModelScope.launch {
            settings.updateKIn(kIn)
            _configuration.value = _configuration.value.copy(kIn = kIn)
        }
    }

    fun updateRecordFit(enabled: Boolean) {
        viewModelScope.launch {
            settings.updateRecordFit(enabled)
            _configuration.value = _configuration.value.copy(recordFit = enabled)
        }
    }

    fun updateShowArrow(enabled: Boolean) {
        viewModelScope.launch {
            settings.updateShowArrow(enabled)
            _configuration.value = _configuration.value.copy(showArrow = enabled)
        }
    }

    fun updateUseColors(enabled: Boolean) {
        viewModelScope.launch {
            settings.updateUseColors(enabled)
            _configuration.value = _configuration.value.copy(useColors = enabled)
        }
    }

    fun updateModelType(modelType: WPrimeModelType) {
        viewModelScope.launch {
            settings.updateModelType(modelType)
            _configuration.value = _configuration.value.copy(modelType = modelType)
        }
    }

    fun addAlert(thresholdPercentage: Int, soundEnabled: Boolean) {
        viewModelScope.launch {
            val newAlert = WPrimeAlert(
                id = Uuid.random().toString(),
                thresholdPercentage = thresholdPercentage,
                soundEnabled = soundEnabled
            )
            val updatedAlerts = _configuration.value.alerts + newAlert
            settings.updateAlerts(updatedAlerts)
            _configuration.value = _configuration.value.copy(alerts = updatedAlerts)
        }
    }

    fun updateAlert(alertId: String, thresholdPercentage: Int, soundEnabled: Boolean) {
        viewModelScope.launch {
            val updatedAlerts = _configuration.value.alerts.map { alert ->
                if (alert.id == alertId) {
                    alert.copy(thresholdPercentage = thresholdPercentage, soundEnabled = soundEnabled)
                } else {
                    alert
                }
            }
            settings.updateAlerts(updatedAlerts)
            _configuration.value = _configuration.value.copy(alerts = updatedAlerts)
        }
    }

    fun deleteAlert(alertId: String) {
        viewModelScope.launch {
            val updatedAlerts = _configuration.value.alerts.filter { it.id != alertId }
            settings.updateAlerts(updatedAlerts)
            _configuration.value = _configuration.value.copy(alerts = updatedAlerts)
        }
    }

    fun testAlert(alertId: String) {
        // This will be handled by the UI layer to dispatch the test alert
        // We just need to find the alert and pass it back
        val alert = _configuration.value.alerts.find { it.id == alertId }
        if (alert != null) {
            // The actual dispatch will happen in the main activity/screen
            // We expose this through a flow that the UI can observe
        }
    }
}

class WPrimeConfigViewModelFactory(private val settings: WPrimeSettings) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WPrimeConfigViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WPrimeConfigViewModel(settings) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
