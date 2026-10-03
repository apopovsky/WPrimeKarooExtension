package com.itl.wprimeext.extension

import io.hammerhead.karooext.models.KarooEffect
import io.hammerhead.karooext.models.MarkLap
import io.hammerhead.karooext.models.PauseRide
import io.hammerhead.karooext.models.ResumeRide
import io.hammerhead.karooext.models.ShowMapPage
import io.hammerhead.karooext.models.TurnScreenOff
import io.hammerhead.karooext.models.TurnScreenOn
import io.hammerhead.karooext.models.ZoomPage

/** A closed debug command list. Never instantiate a class named by an untrusted broadcast. */
fun debugRideEffect(action: String?): KarooEffect? = when (action) {
    "io.hammerhead.karooext.models.MarkLap" -> MarkLap
    "io.hammerhead.karooext.models.PauseRide" -> PauseRide
    "io.hammerhead.karooext.models.ResumeRide" -> ResumeRide
    "io.hammerhead.karooext.models.ShowMapPage" -> ShowMapPage()
    "io.hammerhead.karooext.models.ZoomPage" -> ZoomPage()
    "io.hammerhead.karooext.models.TurnScreenOff" -> TurnScreenOff
    "io.hammerhead.karooext.models.TurnScreenOn" -> TurnScreenOn
    else -> null
}

fun testAlertCommand(id: String?, threshold: Int, sound: Boolean, type: String?): WPrimeAlert? {
    if (id.isNullOrBlank() || id.length > 128 || threshold !in 0..100) return null
    val alertType = if (type == null) AlertType.DROP else runCatching { AlertType.valueOf(type) }.getOrNull() ?: return null
    return WPrimeAlert(id, threshold, sound, alertType)
}
