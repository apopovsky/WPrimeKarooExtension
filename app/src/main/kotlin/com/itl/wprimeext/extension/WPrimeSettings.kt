package com.itl.wprimeext.extension

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.itl.wprimeext.utils.LogConstants
import com.itl.wprimeext.utils.WPrimeLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wprime_settings")

data class WPrimeConfiguration(
    val criticalPower: Double = 250.0, // CP en watts
    val anaerobicCapacity: Double = 12000.0, // W' en julios (default 12000)
    val tauRecovery: Double = 300.0, // Tau en segundos (default 300)
    val recordFit: Boolean = true, // NUEVO: controlar si se graba al FIT
)

class WPrimeSettings(private val context: Context) {

    companion object {
        private val CRITICAL_POWER_KEY = doublePreferencesKey("critical_power")
        private val ANAEROBIC_CAPACITY_KEY = doublePreferencesKey("anaerobic_capacity")
        private val TAU_RECOVERY_KEY = doublePreferencesKey("tau_recovery")
        private val RECORD_FIT_KEY = booleanPreferencesKey("record_fit")
    }

    val configuration: Flow<WPrimeConfiguration> = context.dataStore.data.map { preferences ->
        val config = WPrimeConfiguration(
            criticalPower = preferences[CRITICAL_POWER_KEY] ?: 250.0,
            anaerobicCapacity = preferences[ANAEROBIC_CAPACITY_KEY] ?: 12000.0,
            tauRecovery = preferences[TAU_RECOVERY_KEY] ?: 300.0,
            recordFit = preferences[RECORD_FIT_KEY] ?: true,
        )

        val isDefault = preferences[CRITICAL_POWER_KEY] == null
        if (isDefault) {
            WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_DEFAULT)
        } else {
            WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_LOADED)
        }
        WPrimeLogger.d(
            WPrimeLogger.Module.SETTINGS,
            "Loaded configuration - CP: ${config.criticalPower}, W': ${config.anaerobicCapacity}, Tau: ${config.tauRecovery}, recordFit: ${config.recordFit}"
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

    suspend fun updateRecordFit(enabled: Boolean) {
        WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, "Updating recordFit: $enabled")
        context.dataStore.edit { preferences ->
            preferences[RECORD_FIT_KEY] = enabled
        }
        WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_SAVED + " - Record FIT toggle")
    }

    suspend fun updateConfiguration(config: WPrimeConfiguration) {
        context.dataStore.edit { preferences ->
            preferences[CRITICAL_POWER_KEY] = config.criticalPower
            preferences[ANAEROBIC_CAPACITY_KEY] = config.anaerobicCapacity
            preferences[TAU_RECOVERY_KEY] = config.tauRecovery
        }
        WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_SAVED + " - Full configuration")
    }
}
