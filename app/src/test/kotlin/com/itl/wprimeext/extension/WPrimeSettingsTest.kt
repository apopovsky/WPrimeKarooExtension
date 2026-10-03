package com.itl.wprimeext.extension

import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WPrimeSettingsTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun withSettings(test: suspend (WPrimeSettings, androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>) -> Unit) = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file = temporaryFolder.newFolder().resolve("settings.preferences_pb")
        // Android FileStorage uses File.renameTo, which cannot replace an existing file on Windows.
        val store = PreferenceDataStoreFactory.create(
            storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) { file.absolutePath.toPath() },
            scope = scope,
        )
        try {
            test(WPrimeSettings(store), store)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun invalidStoredValuesAndEnumsFallBackToDefaults() = withSettings { settings, store ->
        store.edit {
            it[doublePreferencesKey("critical_power")] = Double.NaN
            it[doublePreferencesKey("anaerobic_capacity")] = -10.0
            it[doublePreferencesKey("tau_recovery")] = Double.POSITIVE_INFINITY
            it[stringPreferencesKey("model_type")] = "removed-model"
            it[stringPreferencesKey("alerts")] = "invalid-json"
        }
        assertEquals(WPrimeConfiguration(), settings.configuration.first())
    }

    @Test
    fun invalidPhysiologicalWritesAreRejected() = withSettings { settings, _ ->
        for (value in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertTrue(runCatching { settings.updateCriticalPower(value) }.exceptionOrNull() is IllegalArgumentException)
            assertTrue(runCatching { settings.updateAnaerobicCapacity(value) }.exceptionOrNull() is IllegalArgumentException)
            assertTrue(runCatching { settings.updateTauRecovery(value) }.exceptionOrNull() is IllegalArgumentException)
            assertTrue(runCatching { settings.updateKIn(value) }.exceptionOrNull() is IllegalArgumentException)
        }
        assertEquals(WPrimeConfiguration(), settings.configuration.first())
    }

    @Test
    fun concurrentAlertChangesPreserveOtherAlerts() = withSettings { settings, _ ->
        kotlinx.coroutines.coroutineScope {
            repeat(20) { index -> launch { settings.addAlert(WPrimeAlert("$index", index, false)) } }
        }
        assertEquals(20, settings.configuration.first().alerts.size)
        settings.updateAlert(WPrimeAlert("2", 90, true, AlertType.REPLENISH))
        settings.deleteAlert("3")
        val alerts = settings.configuration.first().alerts
        assertEquals(19, alerts.size)
        assertEquals(AlertType.REPLENISH, alerts.first { it.id == "2" }.alertType)
        assertTrue(alerts.none { it.id == "3" })
    }

    @Test
    fun disablingFtpPersistsResolvedPowerAndSourceTogether() = withSettings { settings, _ ->
        settings.useKarooFtp(true, 300)
        settings.useKarooFtp(false, 300)
        val config = settings.configuration.first()
        assertEquals(CriticalPowerSource.MANUAL, config.criticalPowerSource)
        assertEquals(285.0, config.criticalPower, 0.0)
    }

    @Test
    fun readIoFailureUsesDefaultsButProgrammingErrorsPropagate() = runBlocking {
        fun failingSettings(error: Exception) = WPrimeSettings(object : androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
            override val data = kotlinx.coroutines.flow.flow<androidx.datastore.preferences.core.Preferences> { throw error }
            override suspend fun updateData(transform: suspend (androidx.datastore.preferences.core.Preferences) -> androidx.datastore.preferences.core.Preferences): androidx.datastore.preferences.core.Preferences = throw error
        })
        assertEquals(WPrimeConfiguration(), failingSettings(java.io.IOException("unreadable")).configuration.first())
        val failure = runCatching { failingSettings(IllegalStateException("bug")).configuration.first() }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
    }
}
