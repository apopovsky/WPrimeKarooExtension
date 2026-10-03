package com.itl.wprimeext.extension

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WPrimeEngineTest {
    private fun recording(configuration: WPrimeConfiguration = WPrimeConfiguration()): WPrimeEngine = WPrimeEngine(configuration).also { it.setRideState(WPrimeRideState.RECORDING, 0) }

    @Test fun zeroTimestampAndDuplicateSampleIntegrateExactlyOnce() {
        val engine = recording()
        engine.updatePower(350.0, 0)
        assertEquals(11900.0, engine.updatePower(350.0, 1000).wPrimeJoules, 0.001)
        assertEquals(11900.0, engine.updatePower(350.0, 1000).wPrimeJoules, 0.001)
    }

    @Test fun cosmeticConfigurationPreservesBalanceAndTime() {
        val engine = recording()
        engine.updatePower(350.0, 1000)
        val before = engine.snapshot()
        val updated = engine.updateConfiguration(before.configuration.copy(showArrow = false, useColors = false, recordFit = false))
        assertEquals(before.wPrimeJoules, updated.wPrimeJoules, 0.001)
        assertEquals(before.wPrimeJoules - 100, engine.updatePower(350.0, 2000).wPrimeJoules, 0.001)
    }

    @Test fun idleResetsAndDoesNotConsumePower() {
        val engine = recording()
        engine.updatePower(400.0, 1000)
        engine.setRideState(WPrimeRideState.IDLE, 2000)
        assertEquals(12000.0, engine.updatePower(400.0, 3000).wPrimeJoules, 0.001)
    }

    @Test fun pausedRecoversWithoutCountingEffort() {
        val engine = recording()
        engine.updatePower(400.0, 1000)
        val depleted = engine.snapshot().wPrimeJoules
        engine.setRideState(WPrimeRideState.PAUSED, 1000)
        assertTrue(engine.tick(7000).wPrimeJoules > depleted)
        assertEquals(0.0, engine.snapshot().currentPower, 0.0)
    }

    @Test fun silentStreamRecoversButExplicitSensorLossHolds() {
        val engine = recording()
        engine.updatePower(400.0, 1000)
        val depleted = engine.snapshot().wPrimeJoules
        assertEquals(depleted, engine.tick(6000).wPrimeJoules, 0.0)
        assertTrue(engine.tick(7000).wPrimeJoules > depleted)
        engine.setSensorAvailable(false, 8000)
        val held = engine.snapshot().wPrimeJoules
        assertEquals(held, engine.tick(14000).wPrimeJoules, 0.0)
        assertEquals(held, engine.updatePower(400.0, 15000).wPrimeJoules, 0.0)
    }

    @Test fun alertAtSyntheticTimeZeroHasNoInitialCooldown() {
        val drop = WPrimeAlert("drop", 99, false, AlertType.DROP)
        val replenish = WPrimeAlert("up", 99, false, AlertType.REPLENISH)
        val engine = recording(WPrimeConfiguration(alerts = listOf(drop, replenish)))
        assertEquals(listOf(drop), engine.updatePower(400.0, 1000).alerts)
        assertTrue(engine.updatePower(400.0, 2000).alerts.isEmpty())
        engine.setRideState(WPrimeRideState.PAUSED, 2000)
        assertEquals(listOf(replenish), engine.tick(62000).alerts)
        engine.setRideState(WPrimeRideState.RECORDING, 62000)
        assertTrue(engine.updatePower(400.0, 63000).alerts.isEmpty())
    }

    @Test fun invalidPowerDoesNotPoisonOrAdvanceModel() {
        val engine = recording()
        for (power in listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 2001.0)) {
            assertThrows(IllegalArgumentException::class.java) { engine.updatePower(power, 1000) }
        }
        assertEquals(11900.0, engine.updatePower(350.0, 1000).wPrimeJoules, 0.001)
    }

    @Test fun backwardTimeIsRejectedWithoutChangingState() {
        val engine = recording()
        engine.updatePower(350.0, 1000)
        val before = engine.snapshot()
        assertThrows(IllegalArgumentException::class.java) { engine.updatePower(350.0, 999) }
        assertEquals(before, engine.snapshot())
    }

    @Test fun everyModelRemainsFiniteAndWithinCapacity() {
        for (model in WPrimeModelType.entries) {
            val engine = recording(WPrimeConfiguration(modelType = model))
            for (second in 0..120) {
                val state = engine.updatePower(if (second < 60) 500.0 else 0.0, second * 1000L)
                assertTrue(state.wPrimeJoules.isFinite())
                assertTrue(state.wPrimeJoules in 0.0..12000.0)
            }
        }
    }

    @Test fun invalidConfigurationIsRejectedAtomically() {
        val engine = recording()
        engine.updatePower(350.0, 1000)
        val before = engine.snapshot()
        for (bad in listOf(
            before.configuration.copy(criticalPower = Double.NaN),
            before.configuration.copy(anaerobicCapacity = 0.0),
            before.configuration.copy(tauRecovery = 0.0),
            before.configuration.copy(kIn = Double.POSITIVE_INFINITY),
        )) {
            assertThrows(IllegalArgumentException::class.java) { engine.updateConfiguration(bad) }
            assertEquals(before, engine.snapshot())
        }
    }

    @Test fun physiologicalConfigurationPreservesFraction() {
        val engine = recording()
        engine.updatePower(400.0, 1000)
        val fraction = engine.snapshot().percentage
        val state = engine.updateConfiguration(engine.snapshot().configuration.copy(criticalPower = 300.0, anaerobicCapacity = 20000.0))
        assertEquals(fraction, state.percentage, 0.00001)
        assertEquals(20000 * fraction / 100, state.wPrimeJoules, 0.00001)
    }

    @Test fun largeDropChoosesMostCriticalCrossedThreshold() {
        val warnings = listOf(75, 25, 10).map { WPrimeAlert("drop$it", it, false) }
        val engine = recording(WPrimeConfiguration(alerts = warnings))
        assertEquals(listOf(warnings.last()), engine.updatePower(2000.0, 7000).alerts)
    }

    @Test fun fullBalanceResumeDoesNotChargeSilentGapAsNewEffort() {
        val engine = recording()
        engine.updatePower(100.0, 1000)
        assertEquals(12000.0, engine.updatePower(400.0, 3600000).wPrimeJoules, 0.001)
        assertEquals(11850.0, engine.updatePower(400.0, 3601000).wPrimeJoules, 0.001)
    }

    @Test fun explicitFoundSignalExcludesUnknownSensorGap() {
        val engine = recording()
        engine.updatePower(400.0, 1000)
        engine.setSensorAvailable(false, 1000)
        val before = engine.snapshot().wPrimeJoules
        engine.setSensorAvailable(true, 100000)
        assertEquals(before - 150.0, engine.updatePower(400.0, 101000).wPrimeJoules, 0.001)
    }

    @Test fun sensorNotificationsDoNotErasePausedRecovery() {
        val engine = recording()
        engine.updatePower(400.0, 1000)
        val before = engine.snapshot().wPrimeJoules
        engine.setRideState(WPrimeRideState.PAUSED, 1000)
        engine.setSensorAvailable(false, 2000)
        engine.setSensorAvailable(false, 3000)
        engine.setSensorAvailable(true, 4000)
        assertEquals(
            before + (250.0 / 12000.0) * (12000.0 - before) * 3.0,
            engine.tick(4000).wPrimeJoules,
            0.001,
        )
    }
}
