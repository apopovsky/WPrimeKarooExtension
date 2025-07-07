package com.itl.wprimeext.extension

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.itl.wprimeext.utils.WPrimeLogger
import com.itl.wprimeext.utils.LogConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wprime_settings")

data class WPrimeConfiguration(
    val criticalPower: Double = 250.0, // CP en watts
    val anaerobicCapacity: Double = 12000.0, // W' en julios (default 12000)
    val tauRecovery: Double = 300.0, // Tau en segundos (default 300)
)

class WPrimeSettings(private val context: Context) {

    companion object {
        private val CRITICAL_POWER_KEY = doublePreferencesKey("critical_power")
        private val ANAEROBIC_CAPACITY_KEY = doublePreferencesKey("anaerobic_capacity")
        private val TAU_RECOVERY_KEY = doublePreferencesKey("tau_recovery")
    }

    val configuration: Flow<WPrimeConfiguration> = context.dataStore.data.map { preferences ->
        val config = WPrimeConfiguration(
            criticalPower = preferences[CRITICAL_POWER_KEY] ?: 250.0,
            anaerobicCapacity = preferences[ANAEROBIC_CAPACITY_KEY] ?: 12000.0,
            tauRecovery = preferences[TAU_RECOVERY_KEY] ?: 300.0,
        )

        val isDefault = preferences[CRITICAL_POWER_KEY] == null
        if (isDefault) {
            WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_DEFAULT)
        } else {
            WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_LOADED)
        }
        WPrimeLogger.logConfiguration(WPrimeLogger.Module.SETTINGS, config.criticalPower, config.anaerobicCapacity, config.tauRecovery)

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

    suspend fun updateConfiguration(config: WPrimeConfiguration) {
        WPrimeLogger.logConfiguration(WPrimeLogger.Module.SETTINGS, config.criticalPower, config.anaerobicCapacity, config.tauRecovery)
        context.dataStore.edit { preferences ->
            preferences[CRITICAL_POWER_KEY] = config.criticalPower
            preferences[ANAEROBIC_CAPACITY_KEY] = config.anaerobicCapacity
            preferences[TAU_RECOVERY_KEY] = config.tauRecovery
        }
        WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_SAVED + " - Full configuration")
    }
}
