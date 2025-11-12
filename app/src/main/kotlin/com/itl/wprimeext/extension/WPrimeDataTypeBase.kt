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

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.unit.ColorProvider
import com.itl.wprimeext.ui.WPrimeGlanceView
import com.itl.wprimeext.ui.WPrimeNotAvailableGlanceView
import com.itl.wprimeext.ui.calculateWPrimeColors
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
    context: Context,
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

    /**
     * Internal data passed to Glance composition. Always uses current W' (Joules) plus configuration
     * so the UI (arrows, colors, thresholds) can be computed consistently regardless of how each
     * DataType chooses to display/format the number (percent or Joules).
     */
    data class WPrimeDisplayData(
        val wPrimeJoules: Double,
        val backgroundColor: Color,
        val textColor: Color,
        val currentPower: Int,
        val criticalPower: Int,
        val anaerobicCapacity: Double,
    )

    abstract fun getFormatDataTypeId(): String
    abstract fun getDisplayText(joulesValue: Double): String
    abstract fun getUnitText(): String
    abstract fun getFieldLabel(): String
    open fun getNumberVerticalOffset(): Int = 0
    open fun getTargetHeightFraction(): Float = 0.5f
    open fun getValueBottomPaddingExtra(): Int = 0
    open fun getFixedCharCount(): Int? = null
    open fun getSizeScale(): Float = 1f
    // NEW: numeric value provided to Karoo stream (could be percent or Joules)
    abstract fun getInitialStreamValue(): Double
    abstract fun mapJoulesToStreamValue(joules: Double): Double

    protected fun getAnaerobicCapacity(): Double = wprimeCalculator.getAnaerobicCapacity()

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
                            config.kIn,
                            config.modelType,
                        )
                        WPrimeLogger.d(
                            WPrimeLogger.Module.DATA_TYPE,
                            "Setting Calculator Configuration for $typeId - Model: ${config.modelType}, CP: ${config.criticalPower}W, W': ${config.anaerobicCapacity}J, Tau: ${config.tauRecovery}s, kIn: ${config.kIn}"
                        )
                    }
                }

                // Emit initial (full) W' in stream units (percent or Joules)
                emitter.onNext(
                    StreamState.Streaming(
                        DataPoint(
                            dataTypeId,
                            values = mapOf(DataType.Field.SINGLE to getInitialStreamValue()),
                        ),
                    ),
                )

                // Stream 3s smoothed power data for W' calculation
                val powerFlow = karooSystem.streamDataFlow(DataType.Type.SMOOTHED_3S_AVERAGE_POWER)
                powerFlow.collect { power ->
                    when (power) {
                        is StreamState.Streaming -> {
                            val powerValue = power.dataPoint.singleValue ?: 0.0
                            val currentWPrimeJ =
                                wprimeCalculator.updatePower(
                                    powerValue,
                                    System.currentTimeMillis(),
                                ) // returns current W' in Joules

                            val streamValue = mapJoulesToStreamValue(currentWPrimeJ)
                            emitter.onNext(
                                StreamState.Streaming(
                                    DataPoint(
                                        dataTypeId,
                                        values = mapOf(DataType.Field.SINGLE to streamValue),
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

    @SuppressLint("RestrictedApi")
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
                            val joulesValue = data.wPrimeJoules
                            val displayText = getDisplayText(joulesValue)
                            val newView = kotlinx.coroutines.withContext(Dispatchers.Main) {
                                glance.compose(context, DpSize.Unspecified) {
                                    WPrimeGlanceView(
                                        value = displayText,
                                        fieldLabel = getFieldLabel(),
                                        backgroundColor = ColorProvider(data.backgroundColor),
                                        textColor = ColorProvider(data.textColor),
                                        currentPower = data.currentPower,
                                        criticalPower = data.criticalPower,
                                        wPrimeJoules = joulesValue,
                                        anaerobicCapacity = data.anaerobicCapacity,
                                        textSize = config.textSize,
                                        alignment = config.alignment,
                                        numberVerticalOffset = getNumberVerticalOffset(),
                                        targetHeightFraction = getTargetHeightFraction(),
                                        valueBottomExtraPadding = getValueBottomPaddingExtra(),
                                        fixedCharCount = getFixedCharCount(),
                                        sizeScale = getSizeScale(),
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

            // Simulate W' calculation for preview (updates internal Joules)
            wprimeCalculator.updatePower(previewPower, System.currentTimeMillis())
            val wPrimeJ = wprimeCalculator.getCurrentWPrime()
            val (backgroundColor, textColor) = calculateDisplayColors(previewPower)

            emit(
                WPrimeDisplayData(
                    wPrimeJoules = wPrimeJ,
                    backgroundColor = backgroundColor,
                    textColor = textColor,
                    currentPower = previewPower.toInt(),
                    criticalPower = configuration.criticalPower.toInt(),
                    anaerobicCapacity = configuration.anaerobicCapacity,
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
                    val powerValue = power.dataPoint.singleValue ?: 0.0
                    wprimeCalculator.updatePower(powerValue, System.currentTimeMillis())
                    val wPrimeJ = wprimeCalculator.getCurrentWPrime()
                    val (backgroundColor, textColor) = calculateDisplayColors(powerValue)
                    val criticalPower = wprimeCalculator.getCriticalPower()
                    val anaerobicCapacity = wprimeCalculator.getAnaerobicCapacity()

                    emit(
                        WPrimeDisplayData(
                            wPrimeJoules = wPrimeJ,
                            backgroundColor = backgroundColor,
                            textColor = textColor,
                            currentPower = powerValue.toInt(),
                            criticalPower = criticalPower.toInt(),
                            anaerobicCapacity = anaerobicCapacity,
                        ),
                    )
                }
                is StreamState.NotAvailable, is StreamState.Searching -> {
                    val (backgroundColor, textColor) = calculateDisplayColors(0.0)
                    val criticalPower = wprimeCalculator.getCriticalPower()
                    val anaerobicCapacity = wprimeCalculator.getAnaerobicCapacity()
                    val wPrimeJ = wprimeCalculator.getCurrentWPrime()

                    emit(
                        WPrimeDisplayData(
                            wPrimeJoules = wPrimeJ,
                            backgroundColor = backgroundColor,
                            textColor = textColor,
                            currentPower = 0,
                            criticalPower = criticalPower.toInt(),
                            anaerobicCapacity = anaerobicCapacity,
                        ),
                    )
                }
                else -> {
                    val (backgroundColor, textColor) = calculateDisplayColors(0.0)
                    val criticalPower = wprimeCalculator.getCriticalPower()
                    val anaerobicCapacity = wprimeCalculator.getAnaerobicCapacity()
                    val wPrimeJ = wprimeCalculator.getCurrentWPrime()

                    emit(
                        WPrimeDisplayData(
                            wPrimeJoules = wPrimeJ,
                            backgroundColor = backgroundColor,
                            textColor = textColor,
                            currentPower = 0,
                            criticalPower = criticalPower.toInt(),
                            anaerobicCapacity = anaerobicCapacity,
                        ),
                    )
                }
            }
        }
    }

    private fun calculateDisplayColors(currentPower: Double = 0.0): Pair<Color, Color> {
        val criticalPower = wprimeCalculator.getCriticalPower()
        val wPrimePercentage = wprimeCalculator.getWPrimePercentage() / 100.0 // 0-1 range

        WPrimeLogger.d(
            WPrimeLogger.Module.DATA_TYPE,
            "Color calculation - Power: ${currentPower}W, CP: ${criticalPower}W, W': ${(wPrimePercentage * 100).toInt()}%"
        )

        val colors = calculateWPrimeColors(currentPower, criticalPower, wPrimePercentage)

        WPrimeLogger.d(
            WPrimeLogger.Module.DATA_TYPE,
            "Determined colors: BG=#${colors.backgroundColor.value.toString(16).uppercase().padStart(8, '0')}, Text=#${colors.textColor.value.toString(16).uppercase().padStart(8, '0')} for power=${currentPower}W, CP=${criticalPower}W, W'=${(wPrimePercentage * 100).toInt()}%"
        )

        return Pair(colors.backgroundColor, colors.textColor)
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
}
