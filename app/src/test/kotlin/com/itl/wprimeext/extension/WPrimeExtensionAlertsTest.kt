package com.itl.wprimeext.extension

import dagger.hilt.android.testing.HiltAndroidTest
import fi.nikosavola.karooext.testing.robolectric.RobolectricPump
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.InRideAlert
import io.hammerhead.karooext.models.PlayBeepPattern
import io.hammerhead.karooext.models.RideState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
class WPrimeExtensionAlertsTest : WPrimeExtensionTestBase() {

    @Test
    fun `a critical drop uses the critical title and its beep pattern`() {
        configure(alerts = listOf(WPrimeAlert("crit", 10, soundEnabled = true)))
        startRide()

        ride(400.0, 62)

        val alert = awaitAlert("wprime_alert_crit")
        assertEquals("W' Critical!", alert.title)
        assertEquals("W' dropped to 10%", alert.detail)
        val beep = beepPatterns().single()
        assertEquals(listOf(2800, null, 2800, null, 2600), beep.tones.map { it.frequency })
    }

    @Test
    fun `a single large effort alerts only the most critical crossed threshold`() {
        configure(
            alerts = listOf(
                WPrimeAlert("high", 75, false),
                WPrimeAlert("mid", 25, false),
                WPrimeAlert("low", 10, false),
            ),
        )
        startRide()
        ride(350.0, 5)

        // Below full, so the sample integrates rather than rebasing, and drains across every
        // threshold in one step; the engine picks the lowest one.
        RobolectricPump.advanceBy(40.seconds)
        feed(2000.0)

        val alert = awaitAlert("wprime_alert_low")
        assertEquals("W' Critical!", alert.title)
        assertEquals(1, alerts().size)
    }

    @Test
    fun `a new ride clears alert cooldowns so the same threshold fires again`() {
        configure(alerts = listOf(WPrimeAlert("half", 50, false)))
        startRide()
        ride(400.0, 50)
        awaitAlert("wprime_alert_half")

        karoo.system.setRideState(RideState.Idle)
        until("the power subscription to end") { !karoo.system.hasStreamConsumer(DataType.Type.POWER) }
        awaitLatest(percent, 100.0)

        resumeRide()
        ride(400.0, 50)

        until("a second alert") { alerts().size == 2 }
        assertEquals("wprime_alert_half", alerts().last().id)
    }

    @Test
    fun `a debug test-alert broadcast raises a replenish alert and ignores an unknown type`() {
        configure()
        host = karoo.host<WPrimeExtension>()
        awaitReceiver("io.hammerhead.wprime.TEST_ALERT")

        sendTestAlert("r1", threshold = 60, type = "REPLENISH")
        val alert = karoo.awaitEffect<InRideAlert> { it.id == "wprime_alert_r1" }
        assertEquals("W' Recovered", alert.title)
        assertEquals("W' recovered to 60%", alert.detail)

        // Past the receiver's 1 s rate limit, so an invalid type is what suppresses the next one.
        RobolectricPump.advanceBy(2.seconds)
        val mark = karoo.system.effects.size
        sendTestAlert("bad", threshold = 60, type = "NOPE")
        karoo.assertNoEffect<InRideAlert>(forMs = 300) { it.id == "wprime_alert_bad" }
        assertEquals(mark, karoo.system.effects.size)
    }

    @Test
    fun `an alert added while riding starts firing`() {
        configure()
        startRide()
        ride(300.0, 20)
        awaitLatest(percent, 90.0)

        updateSettings { addAlert(WPrimeAlert("late", 80, soundEnabled = false, alertType = AlertType.DROP)) }
        // Two easy steps keep W' above the new threshold while the configuration lands.
        ride(300.0, 2)
        ride(400.0, 30)

        val alert = awaitAlert("wprime_alert_late")
        assertEquals("W' Alert", alert.title)
        assertTrue(beepPatterns().isEmpty())
    }
}
