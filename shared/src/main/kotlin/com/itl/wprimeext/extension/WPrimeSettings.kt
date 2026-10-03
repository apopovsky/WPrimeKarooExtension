package com.itl.wprimeext.extension

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.itl.wprimeext.utils.LogConstants
import com.itl.wprimeext.utils.WPrimeLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wprime_settings")

@Serializable
enum class AlertType { DROP, REPLENISH }

enum class CriticalPowerSource { MANUAL, KAROO_FTP }

@Serializable
data class WPrimeAlert(
    val id: String,
    val thresholdPercentage: Int, // 0-100
    val soundEnabled: Boolean,
    val alertType: AlertType = AlertType.DROP, // DROP = fires when W' falls through; REPLENISH = fires when W' rises through
)

data class WPrimeConfiguration(
    val criticalPower: Double = 250.0,
    val criticalPowerSource: CriticalPowerSource = CriticalPowerSource.MANUAL,
    val anaerobicCapacity: Double = 12000.0,
    val tauRecovery: Double = 300.0,
    val kIn: Double = 0.002,
    val recordFit: Boolean = true,
    val modelType: WPrimeModelType = WPrimeModelType.SKIBA_DIFFERENTIAL,
    val showArrow: Boolean = true,
    val useColors: Boolean = true,
    val alerts: List<WPrimeAlert> = emptyList(),
)

const val KAROO_FTP_TO_CRITICAL_POWER_FACTOR = 0.95

class WPrimeSettings(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.dataStore)

    companion object {
        private val CRITICAL_POWER_KEY = doublePreferencesKey("critical_power")
        private val CRITICAL_POWER_SOURCE_KEY = stringPreferencesKey("critical_power_source")
        private val ANAEROBIC_CAPACITY_KEY = doublePreferencesKey("anaerobic_capacity")
        private val TAU_RECOVERY_KEY = doublePreferencesKey("tau_recovery")
        private val K_IN_KEY = doublePreferencesKey("k_in")
        private val RECORD_FIT_KEY = booleanPreferencesKey("record_fit")
        private val MODEL_TYPE_KEY = stringPreferencesKey("model_type")
        private val SHOW_ARROW_KEY = booleanPreferencesKey("show_arrow")
        private val USE_COLORS_KEY = booleanPreferencesKey("use_colors")
        private val ALERTS_KEY = stringPreferencesKey("alerts")

        private val json = Json { ignoreUnknownKeys = true }
    }

    val configuration: Flow<WPrimeConfiguration> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { preferences ->
        val modelName = preferences[MODEL_TYPE_KEY] ?: WPrimeModelType.SKIBA_DIFFERENTIAL.name
        val modelType = WPrimeModelType.entries.firstOrNull { it.name == modelName } ?: WPrimeModelType.SKIBA_DIFFERENTIAL

        val alerts = decodeAlerts(preferences[ALERTS_KEY])

        val config = WPrimeConfiguration(
            criticalPower = preferences[CRITICAL_POWER_KEY]?.takeIf { it.isFinite() && it > 0.0 } ?: 250.0,
            criticalPowerSource = preferences[CRITICAL_POWER_SOURCE_KEY]
                ?.let { runCatching { CriticalPowerSource.valueOf(it) }.getOrNull() }
                ?: CriticalPowerSource.MANUAL,
            anaerobicCapacity = preferences[ANAEROBIC_CAPACITY_KEY]?.takeIf { it.isFinite() && it > 0.0 } ?: 12000.0,
            tauRecovery = preferences[TAU_RECOVERY_KEY]?.takeIf { it.isFinite() && it > 0.0 } ?: 300.0,
            kIn = preferences[K_IN_KEY]?.takeIf { it.isFinite() && it > 0.0 } ?: 0.002,
            recordFit = preferences[RECORD_FIT_KEY] ?: true,
            modelType = modelType,
            showArrow = preferences[SHOW_ARROW_KEY] ?: true,
            useColors = preferences[USE_COLORS_KEY] ?: true,
            alerts = alerts,
        )

        config
    }.distinctUntilChanged()

    suspend fun updateCriticalPower(power: Double) {
        require(power.isFinite() && power > 0.0) { "Value must be finite and positive" }
        WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, "Updating CP: ${power}W")
        dataStore.edit { preferences ->
            preferences[CRITICAL_POWER_KEY] = power
        }
        WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_SAVED + " - Critical Power")
    }

    suspend fun updateCriticalPowerSource(source: CriticalPowerSource) {
        WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, "Updating CP source: ${source.name}")
        dataStore.edit { preferences ->
            preferences[CRITICAL_POWER_SOURCE_KEY] = source.name
        }
        WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_SAVED + " - Critical Power Source")
    }

    suspend fun updateAnaerobicCapacity(capacity: Double) {
        require(capacity.isFinite() && capacity > 0.0) { "Value must be finite and positive" }
        WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, "Updating W': ${capacity}J")
        dataStore.edit { preferences ->
            preferences[ANAEROBIC_CAPACITY_KEY] = capacity
        }
        WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_SAVED + " - Anaerobic Capacity")
    }

    suspend fun updateTauRecovery(tau: Double) {
        require(tau.isFinite() && tau > 0.0) { "Value must be finite and positive" }
        WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, "Updating Tau: ${tau}s")
        dataStore.edit { preferences ->
            preferences[TAU_RECOVERY_KEY] = tau
        }
        WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_SAVED + " - Tau Recovery")
    }

    suspend fun updateKIn(kIn: Double) {
        require(kIn.isFinite() && kIn > 0.0) { "Value must be finite and positive" }
        WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, "Updating kIn: $kIn")
        dataStore.edit { preferences ->
            preferences[K_IN_KEY] = kIn
        }
        WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_SAVED + " - kIn Parameter")
    }

    suspend fun updateRecordFit(enabled: Boolean) {
        WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, "Updating recordFit: $enabled")
        dataStore.edit { preferences ->
            preferences[RECORD_FIT_KEY] = enabled
        }
        WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_SAVED + " - Record Fit")
    }

    suspend fun updateShowArrow(enabled: Boolean) {
        WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, "Updating showArrow: $enabled")
        dataStore.edit { preferences ->
            preferences[SHOW_ARROW_KEY] = enabled
        }
        WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_SAVED + " - Show Arrow")
    }

    suspend fun updateUseColors(enabled: Boolean) {
        WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, "Updating useColors: $enabled")
        dataStore.edit { preferences ->
            preferences[USE_COLORS_KEY] = enabled
        }
        WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_SAVED + " - Use Colors")
    }

    suspend fun updateModelType(modelType: WPrimeModelType) {
        WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, "Updating Model: ${modelType.name}")
        dataStore.edit { preferences ->
            preferences[MODEL_TYPE_KEY] = modelType.name
        }
        WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_SAVED + " - Model Type")
    }

    suspend fun updateAlerts(alerts: List<WPrimeAlert>) {
        require(alerts.all { it.id.isNotBlank() && it.thresholdPercentage in 0..100 })
        require(alerts.map { it.id }.distinct().size == alerts.size)
        WPrimeLogger.d(WPrimeLogger.Module.SETTINGS, "Updating Alerts: ${alerts.size} alerts")
        dataStore.edit { preferences ->
            preferences[ALERTS_KEY] = json.encodeToString(alerts)
        }
        WPrimeLogger.i(WPrimeLogger.Module.SETTINGS, LogConstants.SETTINGS_SAVED + " - Alerts")
    }

    suspend fun useKarooFtp(enabled: Boolean, ftp: Int?) {
        dataStore.edit { preferences ->
            if (!enabled && preferences[CRITICAL_POWER_SOURCE_KEY] == CriticalPowerSource.KAROO_FTP.name) {
                ftp?.takeIf { it > 0 }?.let { preferences[CRITICAL_POWER_KEY] = it * KAROO_FTP_TO_CRITICAL_POWER_FACTOR }
            }
            preferences[CRITICAL_POWER_SOURCE_KEY] = if (enabled) CriticalPowerSource.KAROO_FTP.name else CriticalPowerSource.MANUAL.name
        }
    }

    suspend fun addAlert(alert: WPrimeAlert) = mutateAlerts { current -> current.filterNot { it.id == alert.id } + alert }

    suspend fun updateAlert(alert: WPrimeAlert) = mutateAlerts { current -> current.map { if (it.id == alert.id) alert else it } }

    suspend fun deleteAlert(id: String) = mutateAlerts { current -> current.filterNot { it.id == id } }

    private suspend fun mutateAlerts(transform: (List<WPrimeAlert>) -> List<WPrimeAlert>) {
        dataStore.edit { preferences ->
            val alerts = transform(decodeAlerts(preferences[ALERTS_KEY]))
            require(alerts.all { it.id.isNotBlank() && it.thresholdPercentage in 0..100 })
            preferences[ALERTS_KEY] = json.encodeToString(alerts)
        }
    }

    private fun decodeAlerts(encoded: String?): List<WPrimeAlert> = encoded?.let { runCatching { json.decodeFromString<List<WPrimeAlert>>(it) }.getOrNull() }
        .orEmpty().filter { it.id.isNotBlank() && it.thresholdPercentage in 0..100 }.distinctBy { it.id }
}
