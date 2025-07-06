package com.itl.wprimeext.extension

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
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
        WPrimeConfiguration(
            criticalPower = preferences[CRITICAL_POWER_KEY] ?: 250.0,
            anaerobicCapacity = preferences[ANAEROBIC_CAPACITY_KEY] ?: 12000.0,
            tauRecovery = preferences[TAU_RECOVERY_KEY] ?: 300.0,
        )
    }

    suspend fun updateCriticalPower(power: Double) {
        context.dataStore.edit { preferences ->
            preferences[CRITICAL_POWER_KEY] = power
        }
    }

    suspend fun updateAnaerobicCapacity(capacity: Double) {
        context.dataStore.edit { preferences ->
            preferences[ANAEROBIC_CAPACITY_KEY] = capacity
        }
    }

    suspend fun updateTauRecovery(tau: Double) {
        context.dataStore.edit { preferences ->
            preferences[TAU_RECOVERY_KEY] = tau
        }
    }

    suspend fun updateConfiguration(config: WPrimeConfiguration) {
        context.dataStore.edit { preferences ->
            preferences[CRITICAL_POWER_KEY] = config.criticalPower
            preferences[ANAEROBIC_CAPACITY_KEY] = config.anaerobicCapacity
            preferences[TAU_RECOVERY_KEY] = config.tauRecovery
        }
    }
}
