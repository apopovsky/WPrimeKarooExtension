package com.itl.wprimeext.extension

import kotlin.math.floor
import kotlin.math.roundToInt

enum class WPrimeFitMessage { RECORD, SESSION }
data class WPrimeFitValues(val joules: Double, val percentage: Double, val message: WPrimeFitMessage)

/** Preserve each recording timestamp; paused session IPC only carries changed rounded values. */
class WPrimeFitProjection {
    private var previousSession: WPrimeFitValues? = null
    private var previousRecordTime: Long? = null
    private var previousRecord: WPrimeFitValues? = null

    fun next(snapshot: WPrimeSnapshot): WPrimeFitValues? {
        if (!snapshot.configuration.recordFit || snapshot.rideState == WPrimeRideState.IDLE) {
            previousSession = null
            previousRecordTime = null
            previousRecord = null
            return null
        }
        val message = if (snapshot.rideState == WPrimeRideState.RECORDING) WPrimeFitMessage.RECORD else WPrimeFitMessage.SESSION
        val values = WPrimeFitValues(
            floor(snapshot.wPrimeJoules + 0.5).coerceIn(0.0, 4294967295.0),
            snapshot.percentage.roundToInt().coerceIn(0, 100).toDouble(),
            message,
        )
        if (message == WPrimeFitMessage.RECORD) {
            previousSession = null
            if (snapshot.timestampMs == previousRecordTime && values == previousRecord) return null
            previousRecordTime = snapshot.timestampMs
            previousRecord = values
        } else {
            previousRecordTime = null
            previousRecord = null
            if (values == previousSession) return null
            previousSession = values
        }
        return values
    }
}
