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
 * Color calculation functions for W' visualization
 */
fun calculateWPrimeColor(smoothedPower3s: Double, criticalPower: Double): Color {
    val powerRatio = smoothedPower3s / criticalPower

    return when {
        // Recovering (power below CP) - Green tones
        powerRatio <= 0.50 -> Color(0xFF2E7D32) // Verde oscuro - recuperación muy rápida
        powerRatio <= 0.70 -> Color(0xFF388E3C) // Verde medio - recuperación rápida
        powerRatio <= 0.85 -> Color(0xFF4CAF50) // Verde - recuperación moderada
        powerRatio <= 1.00 -> Color(0xFF66BB6A) // Verde claro - recuperación lenta

        // Above CP (discharging W') - Yellow to Purple based on intensity
        powerRatio <= 1.10 -> Color(0xFFFFEB3B) // Amarillo - ligeramente sobre CP
        powerRatio <= 1.25 -> Color(0xFFFF9800) // Naranja - moderadamente sobre CP
        powerRatio <= 1.50 -> Color(0xFFFF5722) // Rojo naranja - esfuerzo alto
        powerRatio <= 2.00 -> Color(0xFFE91E63) // Rojo - esfuerzo muy alto
        else -> Color(0xFF9C27B0) // Violeta - esfuerzo extremo que agota W' muy rápido
    }
}
