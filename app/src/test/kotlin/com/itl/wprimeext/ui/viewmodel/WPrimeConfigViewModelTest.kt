package com.itl.wprimeext.ui.viewmodel

import com.itl.wprimeext.extension.AlertType
import com.itl.wprimeext.extension.CriticalPowerSource
import com.itl.wprimeext.extension.WPrimeSettings
import fi.nikosavola.karooext.testing.FakeKarooSystem
import fi.nikosavola.karooext.testing.robolectric.FakeKarooRule
import io.hammerhead.karooext.KarooSystemService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WPrimeConfigViewModelTest {
    @get:Rule val karoo = FakeKarooRule()

    private lateinit var settings: WPrimeSettings
    private lateinit var viewModel: WPrimeConfigViewModel

    @Before
    fun setUp() {
        settings = WPrimeSettings(karoo.app)
        runBlocking {
            settings.updateCriticalPower(200.0)
            settings.updateCriticalPowerSource(CriticalPowerSource.MANUAL)
            settings.updateAnaerobicCapacity(11000.0)
            settings.updateAlerts(emptyList())
        }
        viewModel = WPrimeConfigViewModel(settings, KarooSystemService(karoo.app))
    }

    @Test
    fun `exposes the persisted configuration and the Karoo FTP`() {
        val configuration = karoo.awaitValue {
            viewModel.configuration.value.takeIf { it.anaerobicCapacity == 11000.0 }
        }
        assertEquals(200.0, configuration.criticalPower, 0.0)
        assertFalse(viewModel.isLoading.value)

        karoo.system.setUserProfile(FakeKarooSystem.metricProfile().copy(ftp = 280))
        val ftp = karoo.awaitValue { viewModel.karooFtp.value }
        assertEquals(280, ftp)
    }

    @Test
    fun `alert CRUD persists through the view model`() {
        karoo.awaitValue { true.takeIf { viewModel.configuration.value.alerts.isEmpty() } }

        viewModel.addAlert(thresholdPercentage = 40, soundEnabled = false, alertType = AlertType.DROP)
        val added = karoo.awaitValue { viewModel.configuration.value.alerts.singleOrNull() }
        assertTrue(added.id.isNotBlank())
        assertEquals(40, added.thresholdPercentage)

        viewModel.updateAlert(added.id, thresholdPercentage = 55, soundEnabled = true, alertType = AlertType.REPLENISH)
        val updated = karoo.awaitValue {
            viewModel.configuration.value.alerts.singleOrNull()?.takeIf { it.thresholdPercentage == 55 }
        }
        assertTrue(updated.soundEnabled)
        assertEquals(AlertType.REPLENISH, updated.alertType)

        viewModel.deleteAlert(added.id)
        karoo.awaitValue { true.takeIf { viewModel.configuration.value.alerts.isEmpty() } }
    }

    @Test
    fun `a rejected settings write surfaces a save error`() {
        karoo.awaitValue { true.takeIf { !viewModel.isLoading.value } }

        // Critical power must be positive; the write throws and the view model reports it.
        viewModel.updateCriticalPower(0.0)

        val error = karoo.awaitValue { viewModel.saveError.value }
        assertTrue(error.isNotBlank())
    }
}
