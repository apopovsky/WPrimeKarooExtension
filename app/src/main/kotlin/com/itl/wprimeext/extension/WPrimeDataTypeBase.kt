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
            ContextCompat.getColor(context, android.R.color.holo_green_light)
        }
        powerPercentage <= 110.0 -> {
            interpolateColor(
                    ContextCompat.getColor(context, android.R.color.holo_green_light),
                    ContextCompat.getColor(context, android.R.color.holo_orange_light),
                    95.0,
                    110.0,
                    powerPercentage,
            )
        }
        powerPercentage <= 130.0 -> {
            interpolateColor(
                    ContextCompat.getColor(context, android.R.color.holo_orange_light),
                    ContextCompat.getColor(context, android.R.color.holo_red_light),
                    110.0,
                    130.0,
                    powerPercentage,
            )
        }
        powerPercentage <= 150.0 -> {
            interpolateColor(
                    ContextCompat.getColor(context, android.R.color.holo_red_light),
                    ContextCompat.getColor(context, android.R.color.holo_purple),
                    130.0,
                    150.0,
                    powerPercentage,
            )
        }
        else -> {
            ContextCompat.getColor(context, android.R.color.holo_purple)
        }
    }
}

abstract class WPrimeDataTypeBase(
        private val karooSystem: KarooSystemService,
        private val context: Context,
        extension: String,
        typeId: String
) : DataTypeImpl(extension, typeId) {

    private val wprimeSettings = WPrimeSettings(context)
    private val wprimeCalculator =
            WPrimeCalculator(
                    criticalPower = 250.0,
                    anaerobicCapacity = 12000.0,
                    tauRecovery = 300.0,
            )

    // Power smoothing (5 second window)
    private val powerSamples = mutableListOf<Double>()
    private val maxSamples = 25

    private fun getSmoothedPower(currentPower: Double): Double {
        powerSamples.add(currentPower)
        if (powerSamples.size > maxSamples) {
            powerSamples.removeAt(0)
        }
        return powerSamples.average()
    }

    // Abstract methods to be implemented by subclasses
    abstract fun getDisplayValue(): Double
    abstract fun getInitialValue(): Double
    abstract fun getFormatDataTypeId(): String

    override fun startStream(emitter: Emitter<StreamState>) {
        WPrimeLogger.d(WPrimeLogger.Module.DATA_TYPE, "Starting W Prime data stream for $typeId...")
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
                                    "Setting Calculator Configuration for $typeId - CP: ${config.criticalPower}W, W': ${config.anaerobicCapacity}J, Tau: ${config.tauRecovery}s"
                            )
                        }
                    }

                    emitter.onNext(
                            StreamState.Streaming(
                                    DataPoint(
                                            dataTypeId,
                                            values = mapOf(DataType.Field.SINGLE to getInitialValue()),
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
                                val displayValue = getDisplayValue()

                                WPrimeLogger.logDataTypeUpdate(
                                        WPrimeLogger.Module.DATA_TYPE,
                                        "stream-$typeId",
                                        powerValue,
                                        powerValue,
                                        currentWPrime,
                                        displayValue
                                )

                                emitter.onNext(
                                        StreamState.Streaming(
                                                DataPoint(
                                                        dataTypeId,
                                                        values = mapOf(DataType.Field.SINGLE to displayValue),
                                                ),
                                        ),
                                )
                            }
                            is StreamState.NotAvailable, is StreamState.Searching -> {
                                WPrimeLogger.d(
                                        WPrimeLogger.Module.DATA_TYPE,
                                        "Case NotAvailable/Searching Power data for $typeId: $power"
                                )
                                emitter.onNext(power)
                            }
                            else -> {
                                WPrimeLogger.d(
                                        WPrimeLogger.Module.DATA_TYPE,
                                        "Case Other Power data for $typeId: $power"
                                )
                                emitter.onNext(power)
                            }
                        }
                    }
                }
        emitter.setCancellable {
            WPrimeLogger.d(WPrimeLogger.Module.DATA_TYPE, LogConstants.STREAM_STOPPED + " for $typeId")
            job.cancel()
        }
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        WPrimeLogger.d(
                WPrimeLogger.Module.DATA_TYPE,
                "Starting W Prime view for $typeId... Preview mode: ${config.preview}"
        )

        val configJob =
                CoroutineScope(Dispatchers.IO).launch {
                    WPrimeLogger.d(
                            WPrimeLogger.Module.DATA_TYPE,
                            "Configuring W Prime view formatDataTypeId to ${getFormatDataTypeId()} for $typeId"
                    )
                    emitter.onNext(
                            UpdateNumericConfig(formatDataTypeId = getFormatDataTypeId())
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
                                        "Using preview power flow for $typeId"
                                )
                                previewPowerFlow(configuration.criticalPower)
                            } else {
                                WPrimeLogger.d(
                                        WPrimeLogger.Module.DATA_TYPE,
                                        "Using real power flow for $typeId"
                                )
                                streamRealPowerData()
                            }

                    powerFlow.collect { power ->
                        when (power) {
                            is StreamState.Streaming -> {
                                val powerValue = power.dataPoint.singleValue?.toDouble() ?: 0.0
                                WPrimeLogger.d(WPrimeLogger.Module.DATA_TYPE, "View received power for $typeId: ${powerValue}W")
                                updateBackgroundColor(powerValue, configuration.criticalPower, context, emitter)
                            }
                            else -> {
                                WPrimeLogger.d(WPrimeLogger.Module.DATA_TYPE, "View power data not available for $typeId: $power")
                            }
                        }
                    }
                }

        emitter.setCancellable {
            WPrimeLogger.d(WPrimeLogger.Module.DATA_TYPE, "Stopping W Prime view for $typeId...")
            configJob.cancel()
            viewJob.cancel()
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

            simulationTime += 2.0
            delay(2000)
        }
    }

    private fun streamRealPowerData(): Flow<StreamState> = flow {
        val powerFlow = karooSystem.streamDataFlow(DataType.Type.POWER)
        powerFlow.collect { power ->
            when (power) {
                is StreamState.Streaming -> {
                    emit(power)
                }
                is StreamState.NotAvailable, is StreamState.Searching -> {
                    WPrimeLogger.d(
                            WPrimeLogger.Module.DATA_TYPE,
                            "Power data not available for $typeId: $power"
                    )
                    emit(power)
                }
                else -> {
                    emit(power)
                }
            }
        }
    }

    private fun generatePreviewPowerData(simulationTime: Double, criticalPower: Double): Double {
        val cycleTime = 30.0
        val timeInCycle = (simulationTime % cycleTime) / cycleTime
        val sineValue = (sin(timeInCycle * 2 * PI) + 1) / 2
        val minPowerPercentage = 0.0
        val maxPowerPercentage = 1.6
        val powerPercentage = minPowerPercentage + (sineValue * (maxPowerPercentage - minPowerPercentage))
        val powerValue = criticalPower * powerPercentage
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
            emitter.onNext(ShowCustomStreamState(message = null, color = null))
            return
        }

        val smoothedPower = getSmoothedPower(powerValue)

        WPrimeLogger.d(
                WPrimeLogger.Module.DATA_TYPE,
                "Background color update for $typeId - Power: ${powerValue.toInt()}W (smoothed: ${smoothedPower.toInt()}W), CP: ${criticalPower.toInt()}W"
        )

        val backgroundColor = calculatePowerZoneColor(smoothedPower, criticalPower, context)
        emitter.onNext(ShowCustomStreamState(message = null, color = backgroundColor))
    }

    // Protected getter for calculator (for subclasses)
    protected fun getCalculator(): WPrimeCalculator = wprimeCalculator
}
