package com.itl.wprimeext.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.itl.wprimeext.extension.AlertType
import com.itl.wprimeext.extension.WPrimeAlert
import com.itl.wprimeext.extension.WPrimeConfiguration
import com.itl.wprimeext.extension.WPrimeModelType
import com.itl.wprimeext.extension.WPrimeSettings
import com.itl.wprimeext.extension.userProfileFlow
import com.itl.wprimeext.utils.WPrimeLogger
import io.hammerhead.karooext.KarooSystemService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class WPrimeConfigViewModel(
    private val settings: WPrimeSettings,
    private val karooSystem: KarooSystemService,
) : ViewModel() {

    private val _configuration = MutableStateFlow(WPrimeConfiguration())
    val configuration: StateFlow<WPrimeConfiguration> = _configuration.asStateFlow()

    private val _karooFtp = MutableStateFlow<Int?>(null)
    val karooFtp: StateFlow<Int?> = _karooFtp.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            settings.configuration.collect { configuration ->
                _configuration.value = configuration
                _isLoading.value = false
            }
        }
        runCatching { karooSystem.connect() }
        viewModelScope.launch {
            karooSystem.userProfileFlow().collect { profile ->
                _karooFtp.value = profile?.ftp?.takeIf { it > 0 }
            }
        }
    }

    private fun persist(update: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                update()
                _saveError.value = null
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _saveError.value = "Unable to save settings. Please try again."
                WPrimeLogger.e(WPrimeLogger.Module.SETTINGS, error, "Unable to save settings")
            }
        }
    }

    fun updateCriticalPower(power: Double) {
        persist {
            settings.updateCriticalPower(power)
        }
    }

    fun updateUseKarooFtpForCriticalPower(enabled: Boolean) {
        persist {
            settings.useKarooFtp(enabled, _karooFtp.value)
        }
    }

    fun updateAnaerobicCapacity(capacity: Double) {
        persist {
            settings.updateAnaerobicCapacity(capacity)
        }
    }

    fun updateTauRecovery(tau: Double) {
        persist {
            settings.updateTauRecovery(tau)
        }
    }

    fun updateKIn(kIn: Double) {
        persist {
            settings.updateKIn(kIn)
        }
    }

    fun updateRecordFit(enabled: Boolean) {
        persist {
            settings.updateRecordFit(enabled)
        }
    }

    fun updateShowArrow(enabled: Boolean) {
        persist {
            settings.updateShowArrow(enabled)
        }
    }

    fun updateUseColors(enabled: Boolean) {
        persist {
            settings.updateUseColors(enabled)
        }
    }

    fun updateModelType(modelType: WPrimeModelType) {
        persist {
            settings.updateModelType(modelType)
        }
    }

    fun addAlert(thresholdPercentage: Int, soundEnabled: Boolean, alertType: AlertType = AlertType.DROP) {
        persist {
            val newAlert = WPrimeAlert(
                id = Uuid.random().toString(),
                thresholdPercentage = thresholdPercentage,
                soundEnabled = soundEnabled,
                alertType = alertType,
            )
            settings.addAlert(newAlert)
        }
    }

    fun updateAlert(alertId: String, thresholdPercentage: Int, soundEnabled: Boolean, alertType: AlertType) {
        persist {
            settings.updateAlert(WPrimeAlert(alertId, thresholdPercentage, soundEnabled, alertType))
        }
    }

    fun deleteAlert(alertId: String) {
        persist {
            settings.deleteAlert(alertId)
        }
    }

    override fun onCleared() {
        runCatching { karooSystem.disconnect() }
        super.onCleared()
    }
}

class WPrimeConfigViewModelFactory(
    private val settings: WPrimeSettings,
    private val karooSystem: KarooSystemService,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WPrimeConfigViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WPrimeConfigViewModel(settings, karooSystem) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
