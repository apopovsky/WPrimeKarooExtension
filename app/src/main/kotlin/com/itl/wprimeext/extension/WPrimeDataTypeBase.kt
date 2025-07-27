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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.unit.ColorProvider
import com.itl.wprimeext.ui.WPrimeGlanceView
import com.itl.wprimeext.ui.WPrimeNotAvailableGlanceView
import com.itl.wprimeext.ui.calculateWPrimeColor
import com.itl.wprimeext.utils.LogConstants
import com.itl.wprimeext.utils.WPrimeLogger
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class WPrimeDataTypeBase(
    private val karooSystem: KarooSystemService,
    private val context: Context,
    extension: String,
    typeId: String,
) : DataTypeImpl(extension, typeId) {

    private val glance = GlanceRemoteViews()

    private val wprimeSettings = WPrimeSettings(context)
    private val wprimeCalculator =
        WPrimeCalculator(
            criticalPower = 250.0,
            anaerobicCapacity = 12000.0,
            tauRecovery = 300.0,
        )

    // Power smoothing (5 second window) - keeping for potential future use
    private val powerSamples = mutableListOf<Double>()
    private val maxSamples = 25

    @Suppress("unused")
    private fun getSmoothedPower(currentPower: Double): Double {
        powerSamples.add(currentPower)
        if (powerSamples.size > maxSamples) {
            powerSamples.removeAt(0)
        }
        return powerSamples.average()
    }

    data class WPrimeDisplayData(
        val displayValue: Double,
        val backgroundColor: Color,
    )

    // Abstract methods to be implemented by subclasses
    abstract fun getDisplayValue(): Double
    abstract fun getInitialValue(): Double
    abstract fun getFormatDataTypeId(): String
    abstract fun getDisplayText(value: Double): String
    abstract fun getUnitText(): String
    abstract fun getFieldLabel(wideMode: Boolean = true): String

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
                            "Setting Calculator Configuration for $typeId - CP: ${config.criticalPower}W, W': ${config.anaerobicCapacity}J, Tau: ${config.tauRecovery}s",
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
                                    System.currentTimeMillis(),
                                )
                            val displayValue = getDisplayValue()

                            WPrimeLogger.logDataTypeUpdate(
                                WPrimeLogger.Module.DATA_TYPE,
                                "stream-$typeId",
                                powerValue,
                                powerValue,
                                currentWPrime,
                                displayValue,
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
                                "Case NotAvailable/Searching Power data for $typeId: $power",
                            )
                            emitter.onNext(power)
                        }
                        else -> {
                            WPrimeLogger.d(
                                WPrimeLogger.Module.DATA_TYPE,
                                "Case Other Power data for $typeId: $power",
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
            "Starting W Prime view for $typeId... Preview mode: ${config.preview}",
        )

        // Detect wide mode based on grid size (like karoo-headwind example)
        val wideMode = config.gridSize.first == 60

        val configJob =
            CoroutineScope(Dispatchers.IO).launch {
                WPrimeLogger.d(
                    WPrimeLogger.Module.DATA_TYPE,
                    "Configuring W Prime view as graphical for $typeId (wideMode: $wideMode, textSize: ${config.textSize}, gridSize: ${config.gridSize})",
                )
                emitter.onNext(UpdateGraphicConfig(showHeader = false))
                awaitCancellation()
            }

        val viewJob =
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Show initial searching state
                    if (!config.preview) {
                        val initialRemoteViews = kotlinx.coroutines.withContext(Dispatchers.Main) {
                            glance.compose(context, DpSize.Unspecified) {
                                WPrimeNotAvailableGlanceView(
                                    message = "Searching...",
                                    isKaroo3 = karooSystem.hardwareType == io.hammerhead.karooext.models.HardwareType.KAROO,
                                )
                            }.remoteViews
                        }
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            emitter.updateView(initialRemoteViews)
                        }
                        delay(400L)
                    }

                    val configuration = wprimeSettings.configuration.first()

                    val dataFlow =
                        if (config.preview) {
                            WPrimeLogger.d(
                                WPrimeLogger.Module.DATA_TYPE,
                                "Using preview data flow for $typeId",
                            )
                            previewDataFlow(configuration)
                        } else {
                            WPrimeLogger.d(
                                WPrimeLogger.Module.DATA_TYPE,
                                "Using real data flow for $typeId",
                            )
                            streamRealWPrimeData()
                        }

                    dataFlow.collect { data ->
                        try {
                            val newView = kotlinx.coroutines.withContext(Dispatchers.Main) {
                                glance.compose(context, DpSize.Unspecified) {
                                    WPrimeGlanceView(
                                        value = getDisplayText(data.displayValue),
                                        fieldLabel = getFieldLabel(wideMode),
                                        backgroundColor = ColorProvider(data.backgroundColor),
                                        textSize = config.textSize,
                                        alignment = config.alignment,
                                    )
                                }.remoteViews
                            }

                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                emitter.updateView(newView)
                            }

                            // Add refresh delay to avoid overwhelming the system
                            delay(500L)
                        } catch (e: Exception) {
                            WPrimeLogger.d(
                                WPrimeLogger.Module.DATA_TYPE,
                                "Error updating W Prime view for $typeId: ${e.message}",
                            )
                        }
                    }
                } catch (e: Exception) {
                    WPrimeLogger.d(
                        WPrimeLogger.Module.DATA_TYPE,
                        "Error in W Prime view job for $typeId: ${e.message}",
                    )
                }
            }

        emitter.setCancellable {
            WPrimeLogger.d(WPrimeLogger.Module.DATA_TYPE, LogConstants.STREAM_STOPPED + " for $typeId view")
            configJob.cancel()
            viewJob.cancel()
        }
    }

    private fun previewDataFlow(configuration: WPrimeConfiguration): Flow<WPrimeDisplayData> = flow {
        var simulationTime = 0.0

        while (true) {
            val previewPower = generatePreviewPowerData(simulationTime, configuration.criticalPower)

            // Simulate W' calculation for preview
            wprimeCalculator.updatePower(previewPower, System.currentTimeMillis())
            val displayValue = getDisplayValue()
            val backgroundColor = calculateDisplayColor(displayValue, previewPower)

            emit(
                WPrimeDisplayData(
                    displayValue = displayValue,
                    backgroundColor = backgroundColor,
                ),
            )

            simulationTime += 2.0
            delay(2000)
        }
    }

    private fun streamRealWPrimeData(): Flow<WPrimeDisplayData> = flow {
        val powerFlow = karooSystem.streamDataFlow(DataType.Type.POWER)
        powerFlow.collect { power ->
            when (power) {
                is StreamState.Streaming -> {
                    val powerValue = power.dataPoint.singleValue?.toDouble() ?: 0.0
                    wprimeCalculator.updatePower(powerValue, System.currentTimeMillis())
                    val displayValue = getDisplayValue()
                    val backgroundColor = calculateDisplayColor(displayValue, powerValue)

                    emit(WPrimeDisplayData(displayValue, backgroundColor))
                }
                is StreamState.NotAvailable, is StreamState.Searching -> {
                    // Use default/initial values when power data not available
                    val displayValue = getInitialValue()
                    val backgroundColor = calculateDisplayColor(displayValue, 0.0)

                    emit(
                        WPrimeDisplayData(
                            displayValue = displayValue,
                            backgroundColor = backgroundColor,
                        ),
                    )
                }
                else -> {
                    // Handle other cases with safe defaults
                    val displayValue = getInitialValue()
                    val backgroundColor = calculateDisplayColor(displayValue, 0.0)

                    emit(
                        WPrimeDisplayData(
                            displayValue = displayValue,
                            backgroundColor = backgroundColor,
                        ),
                    )
                }
            }
        }
    }

    private fun calculateDisplayColor(displayValue: Double, currentPower: Double = 0.0): Color {
        // Get current configuration for critical power
        val criticalPower = wprimeCalculator.getCriticalPower()

        // Use current power (3s smoothed would be ideal, but we'll use current power for now)
        return calculateWPrimeColor(currentPower, criticalPower)
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

    // Protected getter for calculator (for subclasses)
    protected fun getCalculator(): WPrimeCalculator = wprimeCalculator
}
