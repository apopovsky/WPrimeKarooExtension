package com.itl.wprimeext.extension

import dagger.hilt.android.testing.HiltAndroidTest
import fi.nikosavola.karooext.testing.ViewUpdate
import fi.nikosavola.karooext.testing.inflate
import fi.nikosavola.karooext.testing.robolectric.RobolectricPump
import fi.nikosavola.karooext.testing.texts
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile
import io.hammerhead.karooext.models.ViewConfig
import io.hammerhead.karooext.models.WriteToRecordMesg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
class WPrimeExtensionLifecycleTest : WPrimeExtensionTestBase() {

    @Test
    fun `a mid-ride restart re-subscribes once and resumes integration`() {
        configure()
        startRide()
        ride(350.0, 10)
        awaitLatest(percent, 90.0)
        // Park a 0 W point so the restarted consumer is not replayed the old hard effort.
        feed(0.0)

        host = karoo.restart<WPrimeExtension>()
        percent = host.startStream("wprime")
        kilojoules = host.startStream("wprime-kj")
        fit = host.startFit()

        // RideState is sticky, so the new runtime starts recording and must resubscribe power.
        karoo.awaitStreamConsumer(DataType.Type.POWER)
        assertEquals(1, karoo.system.consumerParams.count { it == OnStreamState.StartStreaming(DataType.Type.POWER) })
        assertEquals(1, karoo.system.consumerParams.count { it == RideState.Params })
        assertEquals(1, karoo.system.consumerParams.count { it == UserProfile.Params })

        // The engine restarts at full capacity; a fresh ride has to drain it again.
        repeat(15) {
            RobolectricPump.advanceBy(1.seconds)
            feed(400.0)
        }
        until("the restarted ride to drain") {
            val value = (percent.items.lastOrNull() as? StreamState.Streaming)?.dataPoint?.singleValue
            value != null && value < 80.0
        }
    }

    @Test
    fun `both fields and FIT share one raw power subscription`() {
        configure()
        startRide()
        ride(350.0, 5)

        assertEquals(1, karoo.system.consumerParams.count { it == OnStreamState.StartStreaming(DataType.Type.POWER) })
        assertEquals(1, karoo.system.consumerParams.count { it == RideState.Params })
        assertEquals(1, karoo.system.consumerParams.count { it == UserProfile.Params })
    }

    @Test
    fun `a preview field view renders its generator without a ride`() {
        configure()
        host = karoo.host<WPrimeExtension>()
        val view = host.startView(
            "wprime",
            ViewConfig(gridSize = 30 to 30, viewSize = 240 to 360, textSize = 40, preview = true),
        )

        // The preview generator starts at 400 W from full capacity, so the first frame is 100%.
        val frame = view.awaitOf<ViewUpdate.Frame>(10_000) { "100" in it.views.inflate(karoo.app).texts() }
        assertTrue("100" in frame.views.inflate(karoo.app).texts())
        assertTrue(karoo.system.consumerParams.none { it is OnStreamState.StartStreaming })
    }

    @Test
    fun `a field view renders full capacity before any ride`() {
        configure()
        host = karoo.host<WPrimeExtension>()
        val view = host.startView(
            "wprime-kj",
            ViewConfig(gridSize = 30 to 30, viewSize = 480 to 200, textSize = 30),
        )

        val frame = view.awaitOf<ViewUpdate.Frame>(10_000) { "10.0" in it.views.inflate(karoo.app).texts() }
        assertTrue("10.0" in frame.views.inflate(karoo.app).texts())
    }

    @Test
    fun `FIT records carry the declared developer field definitions`() {
        configure()
        startRide()
        ride(350.0, 10)

        val record = fit.awaitOf<WriteToRecordMesg>(10_000)
        val fields = record.values.mapNotNull { it.developerField }.associateBy { it.fieldName }
        val joules = fields.getValue("WPrimeJ")
        assertEquals(1, joules.fieldDefinitionNumber.toInt())
        assertEquals(134, joules.fitBaseTypeId.toInt())
        assertEquals("J", joules.units)
        val percentage = fields.getValue("WPrimePct")
        assertEquals(2, percentage.fieldDefinitionNumber.toInt())
        assertEquals(132, percentage.fitBaseTypeId.toInt())
        assertEquals("%", percentage.units)
    }
}
