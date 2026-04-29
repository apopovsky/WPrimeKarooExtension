/**
 * Copyright (c) 2025 SRAM LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.itl.wprimeext.extension

import com.itl.wprimeext.R
import com.itl.wprimeext.utils.WPrimeLogger
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.InRideAlert
import io.hammerhead.karooext.models.PlayBeepPattern

/**
 * Manages W' threshold alerts with cooldown logic to prevent alert spam.
 * Alerts fire when W' percentage crosses downward through configured thresholds.
 */
class WPrimeAlertManager(
    private val karooSystem: KarooSystemService,
) {
    private val lastAlertTimestamps = mutableMapOf<String, Long>()
    private var previousWPrimePercentage: Double? = null

    companion object {
        private const val ALERT_COOLDOWN_MS = 30_000L // 30 seconds
        private const val ALERT_AUTO_DISMISS_MS = 10_000L // 10 seconds
        // Higher, more pleasant frequencies
        private const val BEEP_FREQUENCY_HIGH = 2800 // Hz - Critical alerts
        private const val BEEP_FREQUENCY_MID_HIGH = 2600 // Hz
        private const val BEEP_FREQUENCY_MID = 2400 // Hz - Warning alerts
        private const val BEEP_FREQUENCY_LOW = 2200 // Hz - Standard alerts
        private const val BEEP_DURATION_SHORT = 200 // ms
        private const val BEEP_DURATION_LONG = 300 // ms
        private const val BEEP_GAP_SHORT = 100 // ms
        private const val BEEP_GAP_LONG = 150 // ms
    }

    /**
     * Check if any alerts should be triggered based on current W' percentage.
     *
     * Alert triggering logic:
     * - Alerts ONLY fire when crossing DOWNWARD through a threshold
     * - NO alerts when W' is recovering (going up)
     * - 30-second cooldown per alert between triggers
     *
     * Examples with threshold = 99%:
     * 1. W' goes 100% → 98%: ✅ Alert fires (crossed down through 99%)
     * 2. W' goes 98% → 97%: ❌ No alert (already below threshold)
     * 3. W' goes 97% → 100%: ❌ No alert (going up)
     * 4. W' goes 100% → 99%: ✅ Alert fires IF 30+ seconds since last alert
     * 5. W' goes 99% → 100%: ❌ No alert (going up)
     * 6. W' oscillates 98% ↔ 100% quickly: Only fires once per 30 seconds
     */
    fun checkAlerts(
        currentWPrimePercentage: Double,
        alerts: List<WPrimeAlert>,
    ) {
        val previousPct = previousWPrimePercentage
        previousWPrimePercentage = currentWPrimePercentage

        // Need previous value to detect downward crossing
        if (previousPct == null) return

        val now = System.currentTimeMillis()

        // Sort alerts by threshold (highest to lowest) to fire most critical first
        val sortedAlerts = alerts.sortedByDescending { it.thresholdPercentage }

        for (alert in sortedAlerts) {
            val threshold = alert.thresholdPercentage.toDouble()

            // Check if we crossed downward through this threshold
            // Example: previous=100, current=98, threshold=99 → TRUE (100 > 99 && 98 <= 99)
            // Example: previous=98, current=100, threshold=99 → FALSE (going up, ignored)
            val crossedDownward = previousPct > threshold && currentWPrimePercentage <= threshold

            if (crossedDownward) {
                val lastAlertTime = lastAlertTimestamps[alert.id] ?: 0L
                val timeSinceLastAlert = now - lastAlertTime

                if (timeSinceLastAlert >= ALERT_COOLDOWN_MS) {
                    dispatchAlert(alert, currentWPrimePercentage)
                    lastAlertTimestamps[alert.id] = now

                    // Only fire one alert per update to avoid overwhelming the rider
                    break
                } else {
                    WPrimeLogger.d(
                        WPrimeLogger.Module.DATA_TYPE,
                        "Alert ${alert.id} in cooldown (${(ALERT_COOLDOWN_MS - timeSinceLastAlert) / 1000}s remaining)"
                    )
                }
            }
        }
    }

    private fun dispatchAlert(alert: WPrimeAlert, currentPercentage: Double) {
        WPrimeLogger.i(
            WPrimeLogger.Module.DATA_TYPE,
            "Dispatching W' alert - Threshold: ${alert.thresholdPercentage}%, Current: ${"%.1f".format(currentPercentage)}%, Sound: ${alert.soundEnabled}"
        )

        // Determine alert severity based on threshold
        val backgroundColor = when {
            alert.thresholdPercentage <= 10 -> R.color.alert_critical_red
            alert.thresholdPercentage <= 25 -> R.color.alert_warning_orange
            else -> R.color.alert_warning_yellow
        }

        val title = when {
            alert.thresholdPercentage <= 10 -> "W' Critical!"
            alert.thresholdPercentage <= 25 -> "W' Low"
            else -> "W' Alert"
        }

        // Dispatch in-ride alert
        val inRideAlert = InRideAlert(
            id = "wprime_alert_${alert.id}",
            icon = R.drawable.ic_wprime_alert,
            title = title,
            detail = "W' at ${alert.thresholdPercentage}% (${currentPercentage.toInt()}%)",
            autoDismissMs = ALERT_AUTO_DISMISS_MS,
            backgroundColor = backgroundColor,
            textColor = R.color.alert_text,
        )
        karooSystem.dispatch(inRideAlert)

        // Dispatch beep pattern if enabled
        if (alert.soundEnabled) {
            val beepPattern = createBeepPattern(alert.thresholdPercentage)
            karooSystem.dispatch(beepPattern)
        }
    }

    private fun createBeepPattern(thresholdPercentage: Int): PlayBeepPattern {
        // Create different beep patterns based on severity with pleasant melodies
        return when {
            thresholdPercentage <= 10 -> {
                // Critical: High urgency pattern - descending tones (urgent but not harsh)
                PlayBeepPattern(
                    tones = listOf(
                        PlayBeepPattern.Tone(BEEP_FREQUENCY_HIGH, BEEP_DURATION_SHORT),
                        PlayBeepPattern.Tone(null, BEEP_GAP_SHORT),
                        PlayBeepPattern.Tone(BEEP_FREQUENCY_HIGH, BEEP_DURATION_SHORT),
                        PlayBeepPattern.Tone(null, BEEP_GAP_SHORT),
                        PlayBeepPattern.Tone(BEEP_FREQUENCY_MID_HIGH, BEEP_DURATION_LONG),
                    )
                )
            }
            thresholdPercentage <= 25 -> {
                // Warning: Medium urgency pattern - alternating tones
                PlayBeepPattern(
                    tones = listOf(
                        PlayBeepPattern.Tone(BEEP_FREQUENCY_MID, BEEP_DURATION_SHORT),
                        PlayBeepPattern.Tone(null, BEEP_GAP_SHORT),
                        PlayBeepPattern.Tone(BEEP_FREQUENCY_MID, BEEP_DURATION_SHORT),
                        PlayBeepPattern.Tone(null, BEEP_GAP_LONG),
                        PlayBeepPattern.Tone(BEEP_FREQUENCY_MID_HIGH, BEEP_DURATION_LONG),
                    )
                )
            }
            else -> {
                // Standard: Gentle notification - ascending tones (informative, not alarming)
                PlayBeepPattern(
                    tones = listOf(
                        PlayBeepPattern.Tone(BEEP_FREQUENCY_LOW, BEEP_DURATION_SHORT),
                        PlayBeepPattern.Tone(null, BEEP_GAP_SHORT),
                        PlayBeepPattern.Tone(BEEP_FREQUENCY_MID, BEEP_DURATION_LONG),
                    )
                )
            }
        }
    }

    /**
     * Reset alert state (e.g., when ride starts/ends)
     */
    fun reset() {
        lastAlertTimestamps.clear()
        previousWPrimePercentage = null
        WPrimeLogger.d(WPrimeLogger.Module.DATA_TYPE, "Alert manager reset")
    }

    /**
     * Manually trigger an alert for testing purposes.
     * This bypasses the cooldown and threshold checking.
     */
    fun testAlert(alert: WPrimeAlert, currentWPrimePercentage: Double = alert.thresholdPercentage.toDouble()) {
        WPrimeLogger.i(
            WPrimeLogger.Module.DATA_TYPE,
            "Testing alert - Threshold: ${alert.thresholdPercentage}%, Sound: ${alert.soundEnabled}"
        )
        dispatchAlert(alert, currentWPrimePercentage)
    }
}

