package com.itl.wprimeext.extension

import android.widget.ImageView
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.testing.HiltAndroidTest
import fi.nikosavola.karooext.testing.ViewRecorder
import fi.nikosavola.karooext.testing.ViewUpdate
import fi.nikosavola.karooext.testing.descendants
import fi.nikosavola.karooext.testing.inflate
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class WPrimeExtensionViewTest : WPrimeExtensionTestBase() {
    private val config = ViewConfig(gridSize = 30 to 30, viewSize = 480 to 200, textSize = 30)

    private fun descriptions(view: ViewRecorder): List<String> = (view.frames.lastOrNull() ?: return emptyList())
        .inflate(karoo.app)
        .descendants()
        .filterIsInstance<ImageView>()
        .mapNotNull { it.contentDescription?.toString() }
        .toList()

    @Test
    fun `the trend arrow shows while riding and hides on explicit sensor loss`() {
        configure()
        runBlocking { WPrimeSettings(karoo.app).updateShowArrow(true) }
        startRide()
        ride(350.0, 5)

        val view = host.startView("wprime", config)
        view.awaitOf<ViewUpdate.Frame>(10_000)
        assertTrue(descriptions(view).contains("W' Trend"))

        karoo.system.setStreamState(DataType.Type.POWER, StreamState.NotAvailable)
        val mark = view.mark()
        view.awaitOf<ViewUpdate.Frame>(10_000, after = mark)

        // Explicit loss while recording hides the trend arrow but keeps the header icon.
        assertFalse(descriptions(view).contains("W' Trend"))
        assertTrue(descriptions(view).contains("W' Icon"))
    }

    @Test
    fun `the trend arrow is hidden when the setting is off`() {
        configure()
        runBlocking { WPrimeSettings(karoo.app).updateShowArrow(false) }
        startRide()
        ride(350.0, 5)

        val view = host.startView("wprime", config)
        view.awaitOf<ViewUpdate.Frame>(10_000)

        assertFalse(descriptions(view).contains("W' Trend"))
        assertTrue(descriptions(view).contains("W' Icon"))
    }

    @Test
    fun `explicit sensor loss uses neutral colors and hides the arrow`() {
        val snapshot = WPrimeSnapshot(
            wPrimeJoules = 8000.0,
            percentage = 80.0,
            currentPower = 350.0,
            criticalPower = 250.0,
            anaerobicCapacity = 10000.0,
            configuration = WPrimeConfiguration(useColors = true, showArrow = true),
            rideState = WPrimeRideState.RECORDING,
            sensorAvailable = false,
            timestampMs = 0L,
        )

        val presentation = snapshot.presentation()
        assertEquals(Color.Black, presentation.textColor)
        assertEquals(Color.White, presentation.backgroundColor)
        assertFalse(presentation.showArrow)
        assertTrue(snapshot.copy(sensorAvailable = true).presentation().showArrow)
    }
}
