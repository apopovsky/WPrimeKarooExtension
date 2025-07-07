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
package com.itl.wprimeext.extension

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.itl.wprimeext.utils.LogConstants
import com.itl.wprimeext.utils.WPrimeLogger
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.ShowCustomStreamState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateNumericConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

fun interpolateColor(
        color1: Int,
        color2: Int,
        lowerBound: Double,
        upperBound: Double,
        actualValue: Double
): Int {
    val factor =
            if (upperBound == lowerBound) 0.0
            else ((actualValue - lowerBound) / (upperBound - lowerBound)).coerceIn(0.0, 1.0)
    return ColorUtils.blendARGB(color1, color2, factor.toFloat())
}

fun calculatePowerZoneColor(powerValue: Double, criticalPower: Double, context: Context): Int {
    val powerPercentage = (powerValue / criticalPower) * 100.0

    return when {
        powerPercentage <= 95.0 -> {
            // Green: Below CP
            ContextCompat.getColor(context, android.R.color.holo_green_light)
        }
        powerPercentage <= 110.0 -> {
            // Yellow: 95-110% of CP (Zone 6)
            interpolateColor(
                    ContextCompat.getColor(context, android.R.color.holo_green_light),
                    ContextCompat.getColor(context, android.R.color.holo_orange_light),
                    95.0,
                    110.0,
                    powerPercentage,
            )
        }
        powerPercentage <= 130.0 -> {
            // Orange: 110-130% of CP (Zone 7)
            interpolateColor(
                    ContextCompat.getColor(context, android.R.color.holo_orange_light),
                    ContextCompat.getColor(context, android.R.color.holo_red_light),
                    110.0,
                    130.0,
                    powerPercentage,
            )
        }
        powerPercentage <= 150.0 -> {
            // Red: 130-150% of CP (Zone 8+)
            interpolateColor(
                    ContextCompat.getColor(context, android.R.color.holo_red_light),
                    ContextCompat.getColor(context, android.R.color.holo_purple),
                    130.0,
                    150.0,
                    powerPercentage,
            )
        }
        else -> {
            // Violet: 150%+ of CP (Neuromuscular power)
            ContextCompat.getColor(context, android.R.color.holo_purple)
        }
    }
}

class WPrimeDataType(
        private val karooSystem: KarooSystemService,
        private val context: Context,
        extension: String,
) : DataTypeImpl(extension, "wprime") {

    private val wprimeSettings = WPrimeSettings(context)
    private val wprimeCalculator =
            WPrimeCalculator(
                    criticalPower = 250.0, // Default values, will be updated from settings
                    anaerobicCapacity = 12000.0,
                    tauRecovery = 300.0,
            )

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
        WPrimeLogger.d(WPrimeLogger.Module.DATA_TYPE, "Starting W Prime data stream...")
        val job =
                CoroutineScope(Dispatchers.IO).launch {
                    // Configure the calculator with persistent settings
                    launch {
                        wprimeSettings.configuration.collect { config ->
                            wprimeCalculator.updateConfiguration(
                                    config.criticalPower,
                                    config.anaerobicCapacity,
                                    config.tauRecovery,
                            )
                            WPrimeLogger.d(
                                    WPrimeLogger.Module.DATA_TYPE,
                                    "Setting Calculator Configuration - CP: ${config.criticalPower}W, W': ${config.anaerobicCapacity}J, Tau: ${config.tauRecovery}s"
                            )
                        }
                    }

                    emitter.onNext(
                            StreamState.Streaming(
                                    DataPoint(
                                            dataTypeId,
                                            values =
                                                    mapOf(
                                                            DataType.Field.SINGLE to 100.0
                                                    ), // Initial value for W Prime percentage
                                    ),
                            ),
                    )

                    // Stream real power data for W Prime calculation
                    val powerFlow = karooSystem.streamDataFlow(DataType.Type.POWER)
                    powerFlow.collect { power ->
                        when (power) {
                            is StreamState.Streaming -> {
                                val powerValue = power.dataPoint.singleValue?.toDouble() ?: 0.0
                                val currentWPrime =
                                        wprimeCalculator.updatePower(
                                                powerValue,
                                                System.currentTimeMillis()
                                        )
                                val wprimePercentage = wprimeCalculator.getWPrimePercentage()

                                WPrimeLogger.logDataTypeUpdate(
                                        WPrimeLogger.Module.DATA_TYPE,
                                        "stream",
                                        powerValue,
                                        powerValue,
                                        currentWPrime,
                                        wprimePercentage
                                )

                                emitter.onNext(
                                        StreamState.Streaming(
                                                DataPoint(
                                                        dataTypeId,
                                                        values =
                                                                mapOf(
                                                                        DataType.Field.SINGLE to
                                                                                wprimePercentage
                                                                ),
                                                ),
                                        ),
                                )
                            }
                            is StreamState.NotAvailable, is StreamState.Searching -> {
                                WPrimeLogger.d(
                                        WPrimeLogger.Module.DATA_TYPE,
                                        "Case NotAvailable/Searching Power data: $power"
                                )
                                emitter.onNext(power)
                            }
                            else -> {
                                WPrimeLogger.d(
                                        WPrimeLogger.Module.DATA_TYPE,
                                        "Case Other Power data: $power"
                                )
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

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        WPrimeLogger.d(
                WPrimeLogger.Module.DATA_TYPE,
                "Starting W Prime view... Preview mode: ${config.preview}"
        )

        val configJob =
                CoroutineScope(Dispatchers.IO).launch {
                    WPrimeLogger.d(
                            WPrimeLogger.Module.DATA_TYPE,
                            "Configuring W Prime view formatDataTypeId to PERCENT_MAX_FTP"
                    )
                    emitter.onNext(
                            UpdateNumericConfig(formatDataTypeId = DataType.Type.PERCENT_MAX_FTP)
                    )
                    awaitCancellation()
                }

        val viewJob =
                CoroutineScope(Dispatchers.IO).launch {
                    val configuration = wprimeSettings.configuration.first()

                    val powerFlow =
                            if (config.preview) {
                                WPrimeLogger.d(
                                        WPrimeLogger.Module.DATA_TYPE,
                                        "Using preview power flow"
                                )
                                previewPowerFlow(configuration.criticalPower)
                            } else {
                                WPrimeLogger.d(
                                        WPrimeLogger.Module.DATA_TYPE,
                                        "Using real power flow"
                                )
                                streamRealPowerData()
                            }

                    powerFlow.collect { power ->
                        when (power) {
                            is StreamState.Streaming -> {
                                val powerValue = power.dataPoint.singleValue?.toDouble() ?: 0.0
                                WPrimeLogger.d(WPrimeLogger.Module.DATA_TYPE, "View received power: ${powerValue}W")
                                updateBackgroundColor(powerValue, configuration.criticalPower, context, emitter)
                            }
                            else -> {
                                WPrimeLogger.d(WPrimeLogger.Module.DATA_TYPE, "View power data not available: $power")
                            }
                        }
                    }
                }

        emitter.setCancellable {
            WPrimeLogger.d(WPrimeLogger.Module.DATA_TYPE, "Stopping W Prime view...")
            configJob.cancel()
            viewJob.cancel()
        }
    }

    private fun previewFlow(): Flow<StreamState> = flow {
        while (true) {
            emit(
                    StreamState.Streaming(
                            DataPoint(
                                    dataTypeId,
                                    mapOf(DataType.Field.SINGLE to (0..100).random().toDouble()),
                                    extension
                            )
                    )
            )
            delay(2000)
        }
    }

    private fun previewPowerFlow(criticalPower: Double): Flow<StreamState> = flow {
        var simulationTime = 0.0

        while (true) {
            val previewPower = generatePreviewPowerData(simulationTime, criticalPower)
            emit(
                    StreamState.Streaming(
                            DataPoint(
                                    DataType.Type.POWER,
                                    mapOf(DataType.Field.SINGLE to previewPower),
                                    "preview"
                            )
                    )
            )

            simulationTime += 2.0 // Increment by 2 seconds
            delay(2000) // Update every 2 seconds
        }
    }

    private fun streamRealPowerData(): Flow<StreamState> = flow {
        val powerFlow = karooSystem.streamDataFlow(DataType.Type.POWER)
        powerFlow.collect { power ->
            when (power) {
                is StreamState.Streaming -> {
                    emit(power) // Forward the power data as-is
                }
                is StreamState.NotAvailable, is StreamState.Searching -> {
                    WPrimeLogger.d(
                            WPrimeLogger.Module.DATA_TYPE,
                            "Power data not available: $power"
                    )
                    emit(power) // Forward the state as-is
                }
                else -> {
                    emit(power) // Forward other states as-is
                }
            }
        }
    }

    private fun generatePreviewPowerData(simulationTime: Double, criticalPower: Double): Double {
        // Use a sine wave that varies from 0% to 160% of Critical Power over 30 seconds
        // This creates a smooth, predictable pattern that shows all color zones
        val cycleTime = 30.0 // seconds for a complete cycle
        val timeInCycle = (simulationTime % cycleTime) / cycleTime // 0.0 to 1.0

        // Sine wave that goes from 0 to 1, then map to power range
        val sineValue = (sin(timeInCycle * 2 * PI) + 1) / 2 // 0.0 to 1.0

        // Map to power range: 0% to 160% of CP to show all color zones
        // Green: 0-95%, Yellow: 95-110%, Orange: 110-130%, Red: 130-150%, Violet: 150%+
        val minPowerPercentage = 0.0
        val maxPowerPercentage = 1.6 // 160% of CP

        val powerPercentage =
                minPowerPercentage + (sineValue * (maxPowerPercentage - minPowerPercentage))
        val powerValue = criticalPower * powerPercentage

        // Add some random noise (±5% of CP) to make it more realistic
        val noise = criticalPower * 0.05 * (kotlin.random.Random.nextDouble() - 0.5) * 2

        return (powerValue + noise).coerceAtLeast(0.0)
    }

    private fun updateBackgroundColor(
            powerValue: Double,
            criticalPower: Double,
            context: Context,
            emitter: ViewEmitter
    ) {
        if (powerValue <= 0.0) {
            // Clear background color when no power data
            emitter.onNext(ShowCustomStreamState(message = null, color = null))
            return
        }

        val smoothedPower = getSmoothedPower(powerValue)

        WPrimeLogger.d(
                WPrimeLogger.Module.DATA_TYPE,
                "Background color update - Power: ${powerValue.toInt()}W (smoothed: ${smoothedPower.toInt()}W), CP: ${criticalPower.toInt()}W"
        )

        val backgroundColor = calculatePowerZoneColor(smoothedPower, criticalPower, context)
        emitter.onNext(ShowCustomStreamState(message = null, color = backgroundColor))
    }
}
