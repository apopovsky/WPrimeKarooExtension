package com.itl.wprimeext.extension

import dagger.hilt.android.testing.HiltAndroidTest
import fi.nikosavola.karooext.testing.robolectric.RobolectricPump
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.WriteToSessionMesg
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
class WPrimeExtensionRideStatesTest : WPrimeExtensionTestBase() {

    @Test
    fun `a paused ride keeps recovering even when the sensor is explicitly lost`() {
        configure()
        startRide()
        ride(450.0, 20)
        awaitLatest(percent, 60.0)

        karoo.system.setRideState(RideState.Paused(auto = false))
        until("the session write") { fit.effectsOf<WriteToSessionMesg>().isNotEmpty() }
        karoo.system.setStreamState(DataType.Type.POWER, StreamState.NotAvailable)

        val mark = percent.mark()
        RobolectricPump.advanceBy(3.seconds)
        val recovered = percent.await(20_000, after = mark) { it is StreamState.Streaming }
        assertTrue((recovered as StreamState.Streaming).dataPoint.singleValue!! > 60.0)
    }

    @Test
    fun `ending a paused ride refills W' and stops the subscription`() {
        configure()
        startRide()
        ride(400.0, 20)
        awaitLatest(percent, 70.0)

        karoo.system.setRideState(RideState.Paused(auto = false))
        until("the session write") { fit.effectsOf<WriteToSessionMesg>().isNotEmpty() }

        karoo.system.setRideState(RideState.Idle)
        until("the power subscription to end") { !karoo.system.hasStreamConsumer(DataType.Type.POWER) }
        awaitLatest(percent, 100.0)
        awaitLatest(kilojoules, capacity / 1000.0)
    }

    @Test
    fun `changing the anaerobic capacity mid-ride preserves the fraction and rescales kJ`() {
        configure()
        startRide()
        ride(350.0, 20)
        awaitLatest(percent, 80.0)
        awaitLatest(kilojoules, 8.0)

        capacity = 20000.0
        wBal = 16000.0
        updateSettings { updateAnaerobicCapacity(20000.0) }
        awaitLatest(kilojoules, 16.0)
        awaitLatest(percent, 80.0)

        // The physiology change rebases the interval, so the next sample only re-primes.
        stepPrime(350.0, 80.0)
        ride(350.0, 10)
        awaitLatest(percent, 75.0)
    }

    @Test
    fun `an out-of-range power sample is treated as sensor loss, not effort`() {
        configure()
        startRide()
        ride(350.0, 10)
        awaitLatest(percent, 90.0)

        RobolectricPump.advanceBy(5.seconds)
        // Above the 2000 W ceiling takes the same path as a non-finite sample: explicit loss.
        feed(2500.0)
        RobolectricPump.advanceBy(1.seconds)
        // The first in-range sample back only rebases the clock, then effort counts again.
        feed(350.0)

        ride(350.0, 5)
        awaitLatest(percent, 85.0)
    }
}
