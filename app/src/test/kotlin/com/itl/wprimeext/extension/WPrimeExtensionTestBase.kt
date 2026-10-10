package com.itl.wprimeext.extension

import android.content.Intent
import dagger.hilt.android.testing.HiltAndroidRule
import fi.nikosavola.karooext.testing.FakeKarooHost
import fi.nikosavola.karooext.testing.FitRecorder
import fi.nikosavola.karooext.testing.StreamRecorder
import fi.nikosavola.karooext.testing.robolectric.FakeKarooRule
import fi.nikosavola.karooext.testing.robolectric.RobolectricPump
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.InRideAlert
import io.hammerhead.karooext.models.PlayBeepPattern
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.locks.LockSupport
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

/**
 * Shared scaffolding for the Robolectric extension tests: a bound extension, the two field streams,
 * a FIT session, and a small Skiba-differential oracle so each fed sample can be awaited exactly.
 *
 * The oracle assumes one sample per virtual second. It deliberately busy-waits (no looper pump) so
 * the wait does not move the extension's SystemClock between samples; recorder waits would pump and
 * advance the clock, which the exact-match awaits cannot tolerate.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = dagger.hilt.android.testing.HiltTestApplication::class)
abstract class WPrimeExtensionTestBase {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val karoo = FakeKarooRule()

    protected lateinit var host: FakeKarooHost
    protected lateinit var percent: StreamRecorder
    protected lateinit var kilojoules: StreamRecorder
    protected lateinit var fit: FitRecorder

    protected var criticalPower = CP
    protected var capacity = CAPACITY
    protected var wBal = CAPACITY

    @Before
    fun inject() {
        hilt.inject()
    }

    protected fun configure(
        criticalPower: Double = CP,
        source: CriticalPowerSource = CriticalPowerSource.MANUAL,
        capacity: Double = CAPACITY,
        recordFit: Boolean = true,
        alerts: List<WPrimeAlert> = emptyList(),
    ) {
        this.criticalPower = criticalPower
        this.capacity = capacity
        wBal = capacity
        // The DataStore is a static per-class-loader singleton, so write every key the tests vary.
        runBlocking {
            WPrimeSettings(karoo.app).apply {
                updateCriticalPower(criticalPower)
                updateCriticalPowerSource(source)
                updateAnaerobicCapacity(capacity)
                updateRecordFit(recordFit)
                updateAlerts(alerts)
            }
        }
    }

    /** Starts the extension and streams, then the ride, so the first power sample can be fed. */
    protected fun startRide() {
        host = karoo.host<WPrimeExtension>()
        percent = host.startStream("wprime")
        kilojoules = host.startStream("wprime-kj")
        fit = host.startFit()
        awaitLatest(kilojoules, capacity / 1000.0)
        karoo.awaitConsumer(RideState.Params)
        karoo.awaitConsumer(io.hammerhead.karooext.models.UserProfile.Params)
        karoo.system.setRideState(RideState.Recording)
        karoo.awaitStreamConsumer(DataType.Type.POWER)
        // Rebased at ride start, so the first sample would integrate the real wait: prime at 0 W.
        feed(0.0)
    }

    /**
     * Starts a fresh ride from a known full balance on the current host, e.g. after Idle. Parks a
     * 0 W point first: the fake replays the latest stream state to a late consumer, so without it
     * the new subscription would immediately re-integrate the previous ride's last hard effort.
     */
    protected fun resumeRide() {
        wBal = capacity
        feed(0.0)
        karoo.system.setRideState(RideState.Recording)
        karoo.awaitStreamConsumer(DataType.Type.POWER)
        feed(0.0)
    }

    protected fun feed(watts: Double) = karoo.system.setDataPoint(
        DataPoint(DataType.Type.POWER, mapOf(DataType.Field.POWER to watts)),
    )

    /**
     * Feeds one sample per virtual second and waits for each result. An unwaited burst can overflow
     * the extension's 64-sample queue, which it reads as a sensor gap.
     */
    protected fun ride(watts: Double, seconds: Int, paused: Boolean = false) = repeat(seconds) {
        RobolectricPump.advanceBy(1.seconds)
        feed(watts)
        val effective = if (paused) 0.0 else watts
        wBal = if (effective > criticalPower) {
            (wBal - (effective - criticalPower)).coerceAtLeast(0.0)
        } else {
            wBal + (criticalPower - effective) / capacity * (capacity - wBal)
        }
        awaitLatest(percent, wBal / capacity * 100.0)
    }

    /** One sample that settles a rebase without integrating, awaited by the current percentage. */
    protected fun stepPrime(watts: Double, currentPercent: Double) {
        RobolectricPump.advanceBy(1.seconds)
        feed(watts)
        awaitLatest(percent, currentPercent)
    }

    /** Applies a settings change on a background thread and waits for the write to finish. */
    protected fun updateSettings(block: suspend WPrimeSettings.() -> Unit) {
        val job = CoroutineScope(Dispatchers.IO).launch { WPrimeSettings(karoo.app).block() }
        until("the settings write") { job.isCompleted }
    }

    protected fun awaitLatest(stream: StreamRecorder, expected: Double) = until("stream to show $expected, last ${stream.items.lastOrNull()}") {
        val last = (stream.items.lastOrNull() as? StreamState.Streaming)?.dataPoint?.singleValue
        last != null && abs(last - expected) < 1e-6
    }

    protected fun waitFor(forMs: Long, condition: () -> Boolean): Boolean {
        val deadline = System.nanoTime() + forMs * 1_000_000
        while (!condition()) {
            if (System.nanoTime() >= deadline) return false
            LockSupport.parkNanos(1_000_000)
        }
        return true
    }

    protected fun until(what: String, timeoutMs: Long = 10_000, condition: () -> Boolean) = check(waitFor(timeoutMs, condition)) { "Timed out waiting for $what" }

    protected fun alerts(after: Int = 0) = karoo.system.effects.drop(after).filterIsInstance<InRideAlert>()

    /** Same wait as karoo.awaitEffect without its looper pump, which would move the clock. */
    protected fun awaitAlert(id: String): InRideAlert {
        until("alert $id") { alerts().any { it.id == id } }
        return alerts().first { it.id == id }
    }

    protected fun assertNoNewAlert(after: Int) = assertFalse("Unexpected alerts ${alerts(after)}", waitFor(300) { alerts(after).isNotEmpty() })

    protected fun sendTestAlert(id: String, threshold: Int = 50, sound: Boolean = false, type: String? = null) = karoo.app.sendBroadcast(
        Intent("io.hammerhead.wprime.TEST_ALERT")
            .putExtra("alertId", id)
            .putExtra("threshold", threshold)
            .putExtra("soundEnabled", sound)
            .apply { if (type != null) putExtra("alertType", type) },
    )

    // The extension registers its receivers on a background thread after onCreate.
    protected fun awaitReceiver(action: String) = karoo.awaitValue {
        shadowOf(karoo.app).registeredReceivers.firstOrNull { it.intentFilter.hasAction(action) }
    }

    protected fun beepPatterns(): List<PlayBeepPattern> = karoo.system.effectsOf<PlayBeepPattern>()

    protected companion object {
        const val CP = 250.0

        // Not the 12000 default, so applying it shows on the kJ stream and marks the settings as loaded.
        const val CAPACITY = 10000.0
    }
}
