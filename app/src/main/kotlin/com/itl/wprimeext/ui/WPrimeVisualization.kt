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

import androidx.compose.ui.graphics.Color

/**
 * Color calculation for W' visualization based on power ratio relative to Critical Power.
 * Special case: Light blue when at max W' (100%) with power below CP (stable state)
 * Thresholds (powerRatio = currentPower / CP):
 *  < 0.90  -> recovery green (#109c77)
 *  < 1.00  -> light green (#59c496)
 *  < 1.10  -> yellow (#e6de26)
 *  < 1.25  -> mid orange (#e48f73)
 *  < 1.40  -> orange (#e5683c)
 *  < 1.60  -> red (#c7292a)
 *  >= 1.60 -> max violet (#af26a0)
 */

data class WPrimeColors(
    val backgroundColor: Color,
    val textColor: Color
)

fun calculateWPrimeColors(smoothedPower3s: Double, criticalPower: Double, wPrimePercentage: Double = -1.0): WPrimeColors {
    if (criticalPower <= 0.0) return WPrimeColors(Color(0xFF109C77), Color.White) // safe fallback

    // Special case: Light blue when at 100% W' with power below CP (stable/no change state)
    if (wPrimePercentage >= 0.99 && smoothedPower3s < criticalPower) {
        return WPrimeColors(Color(0xFF94D8E0), Color.Black) // Light blue with black text
    }

    val powerRatio = smoothedPower3s / criticalPower

    return when {
        powerRatio < 0.90 -> WPrimeColors(Color(0xFF109C77), Color.White) // recovery green with white text
        powerRatio < 1.00 -> WPrimeColors(Color(0xFF59C496), Color.Black) // light green with black text
        powerRatio < 1.10 -> WPrimeColors(Color(0xFFE6DE26), Color.Black) // yellow with black text
        powerRatio < 1.25 -> WPrimeColors(Color(0xFFE48F73), Color.Black) // mid orange with black text
        powerRatio < 1.40 -> WPrimeColors(Color(0xFFE5683C), Color.White) // orange with white text
        powerRatio < 1.60 -> WPrimeColors(Color(0xFFC7292A), Color.White) // red with white text
        else -> WPrimeColors(Color(0xFFAF26A0), Color.White) // max violet with white text
    }
}

