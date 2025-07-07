/**
 * Copyright (c) 2024 SRAM LLC.
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

import android.content.Context
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import com.itl.wprimeext.utils.WPrimeLogger
import com.itl.wprimeext.utils.LogConstants

class WPrimeDataType(
    private val karooSystem: KarooSystemService,
    private val context: Context,
    extension: String,
) : DataTypeImpl(extension, "wprime") {

    private val wprimeSettings = WPrimeSettings(context)
    private val wprimeCalculator = WPrimeCalculator(
        criticalPower = 250.0,  // Default values, will be updated from settings
        anaerobicCapacity = 12000.0,
        tauRecovery = 300.0
    )

    // Test mode configuration
    private val enableTestMode = true // Set to false for production
    private var isUsingTestData = false

    // Power smoothing (5 second window)
    private val powerSamples = mutableListOf<Double>()
    private val maxSamples = 25 // Assuming ~5Hz data, 5 seconds = 25 samples

    private fun getSmoothedPower(currentPower: Double): Double {
        powerSamples.add(currentPower)
        if (powerSamples.size > maxSamples) {
            powerSamples.removeAt(0)
        }
        return powerSamples.average()
    }

    override fun startStream(emitter: Emitter<StreamState>) {
        WPrimeLog.dataType("Starting W Prime data stream...")
        val job = CoroutineScope(Dispatchers.IO).launch {
            // Launch a separate coroutine to continuously listen for configuration changes
            launch {
                wprimeSettings.configuration.collect { config ->
                    wprimeCalculator.updateConfiguration(
                        config.criticalPower,
                        config.anaerobicCapacity,
                        config.tauRecovery
                    )
                    WPrimeLog.settingsDebug("Configuration updated: CP=${config.criticalPower}W, W'=${config.anaerobicCapacity}J, Tau=${config.tauRecovery}s")
                }
            }

            val powerFlow = karooSystem.streamDataFlow(DataType.Type.POWER)
            powerFlow.collect { power ->
                when (power) {
                    is StreamState.Streaming -> {
                        isUsingTestData = false
                        processPowerData(power.dataPoint.singleValue?.toDouble() ?: 0.0, emitter)
                    }
                    is StreamState.NotAvailable, is StreamState.Searching -> {
                        if (enableTestMode && !isUsingTestData) {
                            android.util.Log.i("WPrimeDataType", "No real power data available, switching to test mode")
                            isUsingTestData = true
                            startTestDataGeneration(emitter)
                        } else {
                            emitter.onNext(StreamState.NotAvailable)
                        }
                    }
                    else -> {
                        emitter.onNext(power)
                    }
                }
            }
        }
        emitter.setCancellable {
            WPrimeLogger.d(WPrimeLogger.Module.DATA_TYPE, LogConstants.STREAM_STOPPED)
            job.cancel()
        }
    }

    private fun processPowerData(powerValue: Double, emitter: Emitter<StreamState>) {
        val smoothedPower = getSmoothedPower(powerValue)
        val timestamp = System.currentTimeMillis()

        // Calculate W Prime based on current power (using raw power for calculation)
        val currentWPrime = wprimeCalculator.updatePower(powerValue, timestamp)
        val wprimePercentage = wprimeCalculator.getWPrimePercentage()

        // Calculate time metrics using smoothed power
        val timeToExhaustion = wprimeCalculator.getTimeToExhaustion(smoothedPower)
        val timeToRecovery = wprimeCalculator.getTimeToFullRecovery(smoothedPower)

        // Log the values for debugging
        val dataSource = if (isUsingTestData) "[TEST]" else "[REAL]"
        android.util.Log.i("WPrimeDataType", "$dataSource Power: ${powerValue}W (smoothed: ${"%.1f".format(smoothedPower)}W), W': ${currentWPrime.toInt()}J, W'%: ${"%.1f".format(wprimePercentage)}%")
        if (timeToExhaustion != null) {
            android.util.Log.i("WPrimeDataType", "$dataSource Time to exhaustion: ${"%.0f".format(timeToExhaustion)}s")
        }
        if (timeToRecovery != null) {
            android.util.Log.i("WPrimeDataType", "$dataSource Time to recovery: ${"%.0f".format(timeToRecovery)}s")
        }

        // Send multiple data fields
        val dataValues = mutableMapOf<String, Double>().apply {
            put(DataType.Field.SINGLE, wprimePercentage) // Primary field for compatibility
            put("wprime_joules", currentWPrime)
            put("wprime_percentage", wprimePercentage)
            put("power_smoothed", smoothedPower)
            timeToExhaustion?.let { put("time_to_exhaustion", it) }
            timeToRecovery?.let { put("time_to_recovery", it) }
        }

        emitter.onNext(
            StreamState.Streaming(
                DataPoint(
                    dataTypeId,
                    values = dataValues,
                ),
            ),
        )
    }

    private fun startTestDataGeneration(emitter: Emitter<StreamState>) {
        val testJob = CoroutineScope(Dispatchers.IO).launch {
            var simulationTime = 0.0

            while (isUsingTestData) {
                val testPowerValue = generateTestPowerData(simulationTime)
                processPowerData(testPowerValue, emitter)

                simulationTime += 1.0
                delay(1000) // Send test data every second
            }
        }

        emitter.setCancellable {
            WPrimeLogger.d(WPrimeLogger.Module.DATA_TYPE, LogConstants.STREAM_STOPPED + " - Test data generation")
            testJob.cancel()
        }
    }

    private fun generateTestPowerData(simulationTime: Double): Double {
        val baseTime = simulationTime / 60.0 // Convert to minutes

        return when {
            // Warmup phase (0-5 minutes): gradually increasing power
            baseTime < 5.0 -> {
                val warmupProgress = baseTime / 5.0
                150.0 + (100.0 * warmupProgress) + kotlin.random.Random.nextDouble(-20.0, 20.0)
            }

            // Interval training (5-15 minutes): alternating high/low power
            baseTime < 15.0 -> {
                val intervalPhase = ((baseTime - 5.0) % 2.0) // 2-minute intervals
                val basePower = if (intervalPhase < 1.0) 350.0 else 200.0 // High/low intervals
                basePower + kotlin.random.Random.nextDouble(-30.0, 30.0)
            }

            // Steady state (15-25 minutes): around critical power
            baseTime < 25.0 -> {
                250.0 + kotlin.random.Random.nextDouble(-40.0, 40.0)
            }

            // Sprint intervals (25-30 minutes): very high power bursts
            baseTime < 30.0 -> {
                val sprintPhase = ((baseTime - 25.0) % 1.5) // 1.5-minute cycles
                val basePower = if (sprintPhase < 0.3) 500.0 else 180.0 // Sprint/recovery
                basePower + kotlin.random.Random.nextDouble(-50.0, 50.0)
            }

            // Cool down (30+ minutes): decreasing power
            else -> {
                val cooldownTime = baseTime - 30.0
                val cooldownPower = 200.0 - (cooldownTime * 5.0) // Gradual decrease
                (cooldownPower.coerceAtLeast(80.0)) + kotlin.random.Random.nextDouble(-15.0, 15.0)
            }
        }.coerceAtLeast(0.0)
    }
}
