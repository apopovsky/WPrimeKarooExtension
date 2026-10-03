package com.itl.wprimeext.extension

import androidx.compose.ui.graphics.Color
import com.itl.wprimeext.ui.calculateWPrimeColors

data class WPrimePresentation(val backgroundColor: Color, val textColor: Color, val showArrow: Boolean)

/** Shared presentation policy for production RemoteViews and the debug laboratory. */
fun WPrimeSnapshot.presentation(): WPrimePresentation {
    val unknown = !sensorAvailable && rideState == WPrimeRideState.RECORDING
    val colors = calculateWPrimeColors(currentPower, criticalPower, percentage / 100.0)
    return WPrimePresentation(
        if (configuration.useColors && !unknown) colors.backgroundColor else Color.White,
        if (configuration.useColors && !unknown) colors.textColor else Color.Black,
        configuration.showArrow && !unknown,
    )
}
