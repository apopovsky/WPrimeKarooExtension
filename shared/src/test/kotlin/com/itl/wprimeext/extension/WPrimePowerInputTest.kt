package com.itl.wprimeext.extension

import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WPrimePowerInputTest {
    private fun sample(watts: Double, time: Long, sequence: Long) = TimedStreamState(
        StreamState.Streaming(DataPoint(DataType.Type.POWER, mapOf(DataType.Field.SINGLE to watts))),
        time,
        sequence,
    )
    private fun recording() = WPrimeEngine().also { it.setRideState(WPrimeRideState.RECORDING, 0) }

    @Test fun callbackTimesPreserveNormalOneHertzIntegration() {
        val engine = recording()
        val input = WPrimePowerInput(engine)
        for (second in 1L..10L) input.accept(sample(350.0, second * 1000, second))
        assertEquals(11000.0, engine.snapshot().wPrimeJoules, 0.0001)
    }

    @Test fun queueGapExcludesUnobservedEffortAndResumesKnownSamples() {
        val engine = recording()
        val input = WPrimePowerInput(engine)
        input.accept(sample(350.0, 1000, 1))
        val before = engine.snapshot().wPrimeJoules
        val lost = input.accept(sample(1000.0, 100000, 100))
        assertEquals(98L, lost.lostSamples)
        assertEquals(before, lost.snapshot!!.wPrimeJoules, 0.0001)
        assertEquals(before - 100.0, input.accept(sample(350.0, 101000, 101)).snapshot!!.wPrimeJoules, 0.0001)
    }

    @Test fun delayedCallbackCannotRewindPublishedRideState() {
        val engine = recording()
        val input = WPrimePowerInput(engine)
        input.accept(sample(350.0, 1000, 1))
        engine.setRideState(WPrimeRideState.PAUSED, 2000)
        val before = engine.snapshot()
        assertTrue(input.accept(sample(1000.0, 1500, 2)).stale)
        assertEquals(before, engine.snapshot())
    }

    @Test fun queueLossDoesNotEraseKnownPausedRecovery() {
        val engine = recording()
        val input = WPrimePowerInput(engine)
        input.accept(sample(350.0, 1000, 1))
        engine.setRideState(WPrimeRideState.PAUSED, 1000)
        val before = engine.snapshot().wPrimeJoules
        val recovered = input.accept(sample(1000.0, 4000, 4)).snapshot!!
        assertTrue(recovered.wPrimeJoules > before)
        assertEquals(0.0, recovered.currentPower, 0.0)
    }

    @Test fun freshPowerTickerDoesNotInventRecordOrInvalidateQueuedRealSample() {
        val engine = recording()
        val input = WPrimePowerInput(engine)
        input.accept(sample(350.0, 1000, 1))
        val before = engine.snapshot()
        assertEquals(before, engine.tick(3000))
        assertEquals(11800.0, input.accept(sample(350.0, 2000, 2)).snapshot!!.wPrimeJoules, 0.0001)
    }
}
