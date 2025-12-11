package com.itl.wprimeext.extension

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.itl.wprimeext.utils.LogConstants
import com.itl.wprimeext.utils.WPrimeLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wprime_settings")

data class WPrimeConfiguration(
    val criticalPower: Double = 250.0,
    val anaerobicCapacity: Double = 12000.0,
    val tauRecovery: Double = 300.0,
    val kIn: Double = 0.002,
    val recordFit: Boolean = true,
    val modelType: WPrimeModelType = WPrimeModelType.SKIBA_DIFFERENTIAL
)

class WPrimeSettings(private val context: Context) {

    companion object {
        private val CRITICAL_POWER_KEY = doublePreferencesKey("critical_power")
        private val ANAEROBIC_CAPACITY_KEY = doublePreferencesKey("anaerobic_capacity")
        private val TAU_RECOVERY_KEY = doublePreferencesKey("tau_recovery")
        private val K_IN_KEY = doublePreferencesKey("k_in")
        private val RECORD_FIT_KEY = booleanPreferencesKey("record_fit")
        private val MODEL_TYPE_KEY = stringPreferencesKey("model_type")
    }

    val configuration: Flow<WPrimeConfiguration> = context.dataStore.data.map { preferences ->
        val modelName = preferences[MODEL_TYPE_KEY] ?: WPrimeModelType.SKIBA_DIFFERENTIAL.name
        val modelType = WPrimeModelType.valueOf(modelName)

        val config = WPrimeConfiguration(
            criticalPower = preferences[CRITICAL_POWER_KEY] ?: 250.0,
            anaerobicCapacity = preferences[ANAEROBIC_CAPACITY_KEY] ?: 12000.0,
            tauRecovery = preferences[TAU_RECOVERY_KEY] ?: 300.0,
            kIn = preferences[K_IN_KEY] ?: 0.002,
            recordFit = preferences[RECORD_FIT_KEY] ?: true,
            modelType = modelType
        )

        val isDefault = preferences[CRITICAL_POWER_KEY] == null
        if (isDefault) {
            WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_DEFAULT)
        } else {
            WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_LOADED)
        }
        WPrimeLogger.d(
            WPrimeLogger.Module.SETTINGS,
            "Loaded configuration - Model: ${config.modelType}, CP: ${config.criticalPower}, W': ${config.anaerobicCapacity}, Tau: ${config.tauRecovery}, kIn: ${config.kIn}, recordFit: ${config.recordFit}",
        )

        config
    }

    suspend fun updateCriticalPower(power: Double) {
        WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, "Updating CP: ${power}W")
        context.dataStore.edit { preferences ->
            preferences[CRITICAL_POWER_KEY] = power
        }
        WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_SAVED + " - Critical Power")
    }

    suspend fun updateAnaerobicCapacity(capacity: Double) {
        WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, "Updating W': ${capacity}J")
        context.dataStore.edit { preferences ->
            preferences[ANAEROBIC_CAPACITY_KEY] = capacity
        }
        WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_SAVED + " - Anaerobic Capacity")
    }

    suspend fun updateTauRecovery(tau: Double) {
        WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, "Updating Tau: ${tau}s")
        context.dataStore.edit { preferences ->
            preferences[TAU_RECOVERY_KEY] = tau
        }
        WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_SAVED + " - Tau Recovery")
    }

    suspend fun updateKIn(kIn: Double) {
        WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, "Updating kIn: $kIn")
        context.dataStore.edit { preferences ->
            preferences[K_IN_KEY] = kIn
        }
        WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_SAVED + " - kIn Parameter")
    }

    suspend fun updateRecordFit(enabled: Boolean) {
        WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, "Updating recordFit: $enabled")
        context.dataStore.edit { preferences ->
            preferences[RECORD_FIT_KEY] = enabled
        }
        WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_SAVED + " - Record FIT toggle")
    }

    suspend fun updateModelType(modelType: WPrimeModelType) {
        WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, "Updating Model: ${modelType.name}")
        context.dataStore.edit { preferences ->
            preferences[MODEL_TYPE_KEY] = modelType.name
        }
        WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_SAVED + " - Model Type")
    }

}
