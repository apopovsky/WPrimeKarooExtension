/**
 * Copyright (c) 2024 SRAM LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.itl.wprimeext.ui

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.itl.wprimeext.R

/**
 * Simple W' display using RemoteViews for colored background display
 */
fun createWPrimeRemoteView(
    context: Context,
    value: String,
    unit: String,
    backgroundColor: Color,
    showUnit: Boolean = true
): RemoteViews {
    val remoteViews = RemoteViews(context.packageName, R.layout.wprime_display_layout)

    // Set the main value text
    remoteViews.setTextViewText(R.id.wprimeValue, value)

    // Set the unit text (if shown)
    if (showUnit) {
        remoteViews.setTextViewText(R.id.wprimeUnit, unit)
    } else {
        remoteViews.setTextViewText(R.id.wprimeUnit, "")
    }

    // Set the background color
    remoteViews.setInt(R.id.wprimeContainer, "setBackgroundColor", backgroundColor.toArgb())

    return remoteViews
}

/**
 * Color calculation functions for W' visualization
 */
fun calculateWPrimeColor(wprimePercentage: Double): Color {
    return when {
        wprimePercentage >= 80.0 -> Color(0xFF4CAF50) // Verde brillante
        wprimePercentage >= 60.0 -> Color(0xFF8BC34A) // Verde claro
        wprimePercentage >= 40.0 -> Color(0xFFFFEB3B) // Amarillo
        wprimePercentage >= 20.0 -> Color(0xFFFF9800) // Naranja
        wprimePercentage >= 10.0 -> Color(0xFFFF5722) // Rojo naranja
        else -> Color(0xFFF44336) // Rojo intenso
    }
}

fun calculateWPrimeColorForKj(wprimeKj: Double, maxWPrimeKj: Double): Color {
    val percentage = (wprimeKj / maxWPrimeKj) * 100.0
    return calculateWPrimeColor(percentage)
}
