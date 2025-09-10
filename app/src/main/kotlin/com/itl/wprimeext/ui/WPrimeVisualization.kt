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
 * Thresholds (powerRatio = currentPower / CP):
 *  < 0.90  -> recovery green (#109c77)
 *  < 1.00  -> light green (#59c496)
 *  < 1.10  -> yellow (#e6de26)
 *  < 1.25  -> mid orange (#e48f73)
 *  < 1.40  -> orange (#e5683c)
 *  < 1.60  -> red (#c7292a)
 *  >= 1.60 -> max violet (#af26a0)
 */
fun calculateWPrimeColor(smoothedPower3s: Double, criticalPower: Double): Color {
    if (criticalPower <= 0.0) return Color(0xFF109C77) // safe fallback
    val powerRatio = smoothedPower3s / criticalPower

    return when {
        powerRatio < 0.90 -> Color(0xFF109C77) // recovery green
        powerRatio < 1.00 -> Color(0xFF59C496) // light green near CP
        powerRatio < 1.10 -> Color(0xFFE6DE26) // yellow
        powerRatio < 1.25 -> Color(0xFFE48F73) // mid orange
        powerRatio < 1.40 -> Color(0xFFE5683C) // orange
        powerRatio < 1.60 -> Color(0xFFC7292A) // red
        else -> Color(0xFFAF26A0) // max violet
    }
}
