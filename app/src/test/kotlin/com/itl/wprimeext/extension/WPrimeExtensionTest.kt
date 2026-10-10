package com.itl.wprimeext.extension

import android.content.Intent
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fi.nikosavola.karooext.testing.AwaitTimeoutException
import fi.nikosavola.karooext.testing.FakeKarooHost
import fi.nikosavola.karooext.testing.FakeKarooSystem
import fi.nikosavola.karooext.testing.FitRecorder
import fi.nikosavola.karooext.testing.StreamRecorder
import fi.nikosavola.karooext.testing.ViewUpdate
import fi.nikosavola.karooext.testing.developerValues
import fi.nikosavola.karooext.testing.inflate
import fi.nikosavola.karooext.testing.robolectric.FakeKarooRule
import fi.nikosavola.karooext.testing.robolectric.RobolectricPump
import fi.nikosavola.karooext.testing.texts
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.InRideAlert
import io.hammerhead.karooext.models.MarkLap
import io.hammerhead.karooext.models.PauseRide
import io.hammerhead.karooext.models.PlayBeepPattern
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile
import io.hammerhead.karooext.models.ViewConfig
import io.hammerhead.karooext.models.WriteToRecordMesg
import io.hammerhead.karooext.models.WriteToSessionMesg
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.locks.LockSupport
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = HiltTestApplication::class)
class WPrimeExtensionTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val karoo = FakeKarooRule()

    private lateinit var host: FakeKarooHost
    private lateinit var percent: StreamRecorder
    private lateinit var kilojoules: StreamRecorder
    private lateinit var fit: FitRecorder

    // The same textbook Skiba differential step the extension defaults to, kept here as an oracle
    // so each fed sample can wait for exactly its own result.
    private var criticalPower = CP
    private var wBal = CAPACITY

    @Before
    fun setUp() {
        hilt.inject()
    }

    private fun configure(
        criticalPower: Double = CP,
        source: CriticalPowerSource = CriticalPowerSource.MANUAL,
        recordFit: Boolean = true,
        alerts: List<WPrimeAlert> = emptyList(),
    ) {
        this.criticalPower = criticalPower
        // The DataStore is a static per-class-loader singleton, so write every key the tests vary.
        runBlocking {
            WPrimeSettings(karoo.app).apply {
                updateCriticalPower(criticalPower)
                updateCriticalPowerSource(source)
                updateAnaerobicCapacity(CAPACITY)
                updateRecordFit(recordFit)
                updateAlerts(alerts)
            }
        }
    }

    /** Starts the extension and streams, then the ride, so the first power sample can be fed. */
    private fun startRide() {
        host = karoo.host<WPrimeExtension>()
        percent = host.startStream("wprime")
        kilojoules = host.startStream("wprime-kj")
        fit = host.startFit()
        // The extension reads its settings on its own thread; a ride starting first would use defaults.
        awaitLatest(kilojoules, CAPACITY / 1000.0)
        karoo.awaitConsumer(RideState.Params)
        karoo.awaitConsumer(UserProfile.Params)
        karoo.system.setRideState(RideState.Recording)
        karoo.awaitStreamConsumer(DataType.Type.POWER)
        // Rebased at ride start, so the first sample would integrate the real wait: prime at 0 W.
        feed(0.0)
    }

    private fun feed(watts: Double) = karoo.system.setDataPoint(
        DataPoint(DataType.Type.POWER, mapOf(DataType.Field.POWER to watts)),
    )

    /**
     * Feeds one sample per virtual second and waits for each result. Recorder waits would pump the
     * looper and move the extension's clock (SystemClock), and an unwaited burst can overflow the
     * extension's 64-sample queue, which it reads as a sensor gap.
     */
    private fun ride(watts: Double, seconds: Int, paused: Boolean = false) = repeat(seconds) {
        RobolectricPump.advanceBy(1.seconds)
        feed(watts)
        val effective = if (paused) 0.0 else watts
        wBal = if (effective > criticalPower) {
            (wBal - (effective - criticalPower)).coerceAtLeast(0.0)
        } else {
            wBal + (criticalPower - effective) / CAPACITY * (CAPACITY - wBal)
        }
        awaitLatest(percent, wBal / CAPACITY * 100.0)
    }

    private fun awaitLatest(stream: StreamRecorder, expected: Double) = until("stream to show $expected, last ${stream.items.lastOrNull()}") {
        val last = (stream.items.lastOrNull() as? StreamState.Streaming)?.dataPoint?.singleValue
        last != null && abs(last - expected) < 1e-6
    }

    private fun waitFor(forMs: Long, condition: () -> Boolean): Boolean {
        val deadline = System.nanoTime() + forMs * 1_000_000
        while (!condition()) {
            if (System.nanoTime() >= deadline) return false
            LockSupport.parkNanos(1_000_000)
        }
        return true
    }

    private fun until(what: String, timeoutMs: Long = 10_000, condition: () -> Boolean) = check(waitFor(timeoutMs, condition)) { "Timed out waiting for $what" }

    private fun alerts(after: Int = 0) = karoo.system.effects.drop(after).filterIsInstance<InRideAlert>()

    // Same wait as karoo.awaitEffect without its looper pump, which would move the extension's clock.
    private fun awaitAlert(id: String): InRideAlert {
        until("alert $id") { alerts().any { it.id == id } }
        return alerts().first { it.id == id }
    }

    private fun assertNoNewAlert(after: Int) = assertFalse("Unexpected alerts ${alerts(after)}", waitFor(300) { alerts(after).isNotEmpty() })

    @Test
    fun `effort above critical power drains both W' streams`() {
        configure()
        startRide()
        // The streams open at full capacity.
        awaitLatest(percent, 100.0)

        ride(350.0, 30)

        awaitLatest(percent, 70.0)
        awaitLatest(kilojoules, 7.0)
    }

    @Test
    fun `critical power follows the Karoo FTP when configured`() {
        configure(source = CriticalPowerSource.KAROO_FTP)
        karoo.system.setUserProfile(FakeKarooSystem.metricProfile().copy(ftp = 300))
        criticalPower = 285.0
        startRide()

        ride(335.0, 30)

        awaitLatest(kilojoules, 8.5)
    }

    @Test
    fun `a drop alert beeps and shows once as W' falls through its threshold`() {
        configure(alerts = listOf(WPrimeAlert("low", 25, soundEnabled = true)))
        startRide()

        ride(400.0, 50)

        val alert = awaitAlert("wprime_alert_low")
        assertEquals("W' Low", alert.title)
        assertEquals("W' dropped to 25%", alert.detail)
        until("the beep") { karoo.system.effectsOf<PlayBeepPattern>().isNotEmpty() }
        val beep = karoo.system.effectsOf<PlayBeepPattern>().single()
        assertEquals(listOf(2400, null, 2400, null, 2600), beep.tones.map { it.frequency })

        // Staying below the threshold does not alert again.
        val mark = karoo.system.effects.size
        ride(400.0, 10)
        assertNoNewAlert(mark)
        assertEquals(1, alerts().size)
    }

    @Test
    fun `an alert with sound off shows but does not beep`() {
        configure(alerts = listOf(WPrimeAlert("quiet", 50, soundEnabled = false)))
        startRide()

        ride(350.0, 50)

        awaitAlert("wprime_alert_quiet")
        karoo.assertNoEffect<PlayBeepPattern>(forMs = 300)
    }

    @Test
    fun `no alert while W' stays high`() {
        configure(alerts = listOf(WPrimeAlert("half", 50, soundEnabled = true)))
        startRide()

        ride(300.0, 30)

        awaitLatest(percent, 85.0)
        karoo.assertNoEffect<InRideAlert>(forMs = 300)
        karoo.assertNoEffect<PlayBeepPattern>(forMs = 300)
    }

    @Test
    fun `a replenish alert fires when W' recovers through its threshold`() {
        configure(alerts = listOf(WPrimeAlert("back", 90, soundEnabled = false, alertType = AlertType.REPLENISH)))
        startRide()
        ride(450.0, 30)
        assertNoNewAlert(0)

        ride(0.0, 120)

        val alert = awaitAlert("wprime_alert_back")
        assertEquals("W' Recovered", alert.title)
        assertEquals("W' recovered to 90%", alert.detail)
    }

    @Test
    fun `an alert is not repeated inside its five minute cooldown but is after it`() {
        configure(alerts = listOf(WPrimeAlert("half", 50, soundEnabled = false)))
        startRide()
        ride(350.0, 50)
        awaitAlert("wprime_alert_half")

        // Recover above the threshold and cross it again 15 s later.
        ride(0.0, 5)
        val mark = karoo.system.effects.size
        ride(350.0, 10)
        assertNoNewAlert(mark)

        ride(0.0, 400)
        ride(350.0, 60)
        until("a second alert") { alerts().size == 2 }
    }

    @Test
    fun `paused ride recovers instead of draining, with no alert, and resuming counts effort again`() {
        configure(alerts = listOf(WPrimeAlert("seventy", 70, soundEnabled = false)))
        startRide()
        ride(350.0, 20)
        awaitLatest(percent, 80.0)

        karoo.system.setRideState(RideState.Paused(auto = false))
        // The pause is handled on the extension's own thread: wait for its session write so the
        // next sample is not older than the pause.
        until("the session write") { fit.effectsOf<WriteToSessionMesg>().isNotEmpty() }
        assertEquals(8000.0, fit.effectsOf<WriteToSessionMesg>().first().developerValues().getValue("WPrimeJ"), 0.0)
        ride(500.0, 60, paused = true)

        assertTrue(wBal > 9400.0)
        assertNoNewAlert(0)
        until("session writes while paused") { fit.effectsOf<WriteToSessionMesg>().size > 1 }

        val mark = fit.effectsOf<WriteToRecordMesg>().size
        karoo.system.setRideState(RideState.Recording)
        until("the resume record") { fit.effectsOf<WriteToRecordMesg>().size > mark }
        ride(500.0, 40)

        val alert = awaitAlert("wprime_alert_seventy")
        assertEquals("W' Alert", alert.title)
    }

    @Test
    fun `without a ride the power stream is not subscribed and ending a ride refills W'`() {
        configure()
        host = karoo.host<WPrimeExtension>()
        percent = host.startStream("wprime")
        kilojoules = host.startStream("wprime-kj")
        awaitLatest(kilojoules, CAPACITY / 1000.0)
        karoo.awaitConsumer(RideState.Params)
        assertFalse(karoo.system.hasStreamConsumer(DataType.Type.POWER))

        karoo.system.setRideState(RideState.Recording)
        karoo.awaitStreamConsumer(DataType.Type.POWER)
        feed(0.0)
        ride(350.0, 10)
        awaitLatest(percent, 90.0)

        karoo.system.setRideState(RideState.Idle)
        until("the power subscription to end") { !karoo.system.hasStreamConsumer(DataType.Type.POWER) }
        awaitLatest(percent, 100.0)
    }

    @Test
    fun `power lost for a minute does not count as effort`() {
        configure()
        startRide()
        ride(350.0, 10)

        karoo.system.setStreamState(DataType.Type.POWER, StreamState.NotAvailable)
        RobolectricPump.advanceBy(60.seconds)
        feed(350.0)
        // The first sample back only rebases the clock, then effort counts again.
        ride(350.0, 5)

        awaitLatest(kilojoules, 8.5)
    }

    @Test
    fun `fit records carry W' joules and percent while recording`() {
        configure()
        startRide()

        ride(350.0, 20)

        val values = fit.awaitRecord { it["WPrimeJ"] == 8000.0 }
        assertEquals(80.0, values.getValue("WPrimePct"), 0.0)
    }

    @Test
    fun `fit records stay empty when recording to FIT is off`() {
        configure(recordFit = false)
        startRide()

        ride(350.0, 20)

        awaitLatest(percent, 80.0)
        assertTrue(fit.effectsOf<WriteToRecordMesg>().isEmpty())
        assertTrue(fit.effectsOf<WriteToSessionMesg>().isEmpty())
    }

    @Test
    fun `a debug test-alert broadcast raises the alert and a quick repeat is dropped`() {
        configure()
        host = karoo.host<WPrimeExtension>()
        awaitReceiver("io.hammerhead.wprime.TEST_ALERT")

        sendTestAlert("t1")
        karoo.awaitEffect<InRideAlert> { it.id == "wprime_alert_t1" }
        val mark = karoo.system.effects.size
        sendTestAlert("t2")
        karoo.assertNoEffect<InRideAlert>(forMs = 300) { it.id == "wprime_alert_t2" }
        assertEquals(mark, karoo.system.effects.size)

        RobolectricPump.advanceBy(2.seconds)
        sendTestAlert("t3")
        karoo.awaitEffect<InRideAlert> { it.id == "wprime_alert_t3" }
    }

    @Test
    fun `a debug in-ride action broadcast dispatches the matching effect and ignores unknown ones`() {
        configure()
        host = karoo.host<WPrimeExtension>()
        awaitReceiver("io.hammerhead.wprime.IN_RIDE_ACTION")

        karoo.app.sendBroadcast(Intent("io.hammerhead.wprime.IN_RIDE_ACTION").putExtra("action", "java.lang.Runtime"))
        karoo.app.sendBroadcast(Intent("io.hammerhead.wprime.IN_RIDE_ACTION").putExtra("action", PauseRide::class.java.name))
        karoo.awaitEffect<PauseRide>()
        assertTrue(karoo.system.effects.none { it is MarkLap })
    }

    @Test
    fun `a paused ride keeps recovering with no power samples through the runtime ticker`() {
        configure()
        startRide()
        ride(450.0, 30)
        karoo.system.setRideState(RideState.Paused(auto = false))
        until("the session write") { fit.effectsOf<WriteToSessionMesg>().isNotEmpty() }
        val mark = percent.mark()

        // No samples at all: only the runtime's own ticker can move W'. Its 3 s delay is real time.
        RobolectricPump.advanceBy(3.seconds)
        val recovered = percent.await(20_000, after = mark) { it is StreamState.Streaming }
        assertTrue((recovered as StreamState.Streaming).dataPoint.singleValue!! > 40.0)
    }

    @Test
    fun `the field views render the percent and the kilojoules`() {
        configure()
        startRide()
        ride(350.0, 30)
        awaitLatest(kilojoules, 7.0)

        // Started after the ride: a later frame would race the extension's SystemClock render
        // throttle against the SDK's 900 ms real-time frame drop, and this clock was jumped.
        val config = ViewConfig(gridSize = 60 to 15, viewSize = 480 to 200, textSize = 30)
        val percentView = host.startView("wprime", config)
        val kilojouleView = host.startView("wprime-kj", config)

        fun texts(update: ViewUpdate) = (update as? ViewUpdate.Frame)?.views?.inflate(karoo.app)?.texts().orEmpty()
        percentView.await(10_000) { "70" in texts(it) }
        kilojouleView.await(10_000) { "7.0" in texts(it) }
    }

    private fun sendTestAlert(id: String) = karoo.app.sendBroadcast(
        Intent("io.hammerhead.wprime.TEST_ALERT")
            .putExtra("alertId", id)
            .putExtra("threshold", 50)
            .putExtra("soundEnabled", false),
    )

    // The extension registers its receivers on a background thread after onCreate.
    private fun awaitReceiver(action: String) = karoo.awaitValue {
        shadowOf(karoo.app).registeredReceivers.firstOrNull { it.intentFilter.hasAction(action) }
    }

    private companion object {
        const val CP = 250.0

        // Not the 12000 default, so applying it shows on the kJ stream and marks the settings as loaded.
        const val CAPACITY = 10000.0
    }
}
