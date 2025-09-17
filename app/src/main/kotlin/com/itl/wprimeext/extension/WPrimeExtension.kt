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

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.itl.wprimeext.utils.LogConstants
import com.itl.wprimeext.utils.WPrimeLogger
import dagger.hilt.android.AndroidEntryPoint
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.DeveloperField
import io.hammerhead.karooext.models.FieldValue
import io.hammerhead.karooext.models.FitEffect
import io.hammerhead.karooext.models.KarooEffect
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.SystemNotification
import io.hammerhead.karooext.models.WriteToRecordMesg
import io.hammerhead.karooext.models.WriteToSessionMesg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.reflect.full.createInstance
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@AndroidEntryPoint
class WPrimeExtension : KarooExtension("wprime-id", "1.0") {
    @Inject
    lateinit var karooSystem: KarooSystemService

    private var serviceJob: Job? = null

    override val types by lazy {
        listOf(
            WPrimeDataType(karooSystem, this, extension),
            WPrimeKjDataType(karooSystem, this, extension),
        )
    }

    private val wPrimeJField by lazy {
        DeveloperField(
            fieldDefinitionNumber = 1,
            fitBaseTypeId = 134, // FitBaseType.UInt32 (W' stored in Joules, fits in positive int range)
            fieldName = "WPrimeJ",
            units = "J",
        )
    }
    private val wPrimePctField by lazy {
        DeveloperField(
            fieldDefinitionNumber = 2,
            fitBaseTypeId = 132, // FitBaseType.UInt16 (percentage 0-100)
            fieldName = "WPrimePct",
            units = "%",
        )
    }

    override fun startFit(emitter: Emitter<FitEffect>) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            // Initialize settings & calculator (mirror logic from data types)
            val settings = WPrimeSettings(this@WPrimeExtension)
            val initialConfig = settings.configuration.first()
            val calculator = WPrimeCalculator(
                criticalPower = initialConfig.criticalPower,
                anaerobicCapacity = initialConfig.anaerobicCapacity,
                tauRecovery = initialConfig.tauRecovery,
            )
            // Keep calculator updated with config changes
            launch {
                settings.configuration.collect { cfg ->
                    calculator.updateConfiguration(
                        cfg.criticalPower,
                        cfg.anaerobicCapacity,
                        cfg.tauRecovery,
                    )
                }
            }

            karooSystem.streamDataFlow(DataType.Type.SMOOTHED_3S_AVERAGE_POWER)
                .combine(karooSystem.consumerFlow<RideState>()) { powerState, rideState ->
                    Pair(powerState, rideState)
                }
                .collectLatest { (powerState, rideState) ->
                    val streaming = powerState as? StreamState.Streaming
                    val power = streaming?.dataPoint?.singleValue ?: 0.0
                    // Update calculator with 3s smoothed power for more stable W' calculations
                    calculator.updatePower(power, System.currentTimeMillis())
                    val wPrimeJ = calculator.getCurrentWPrime().roundToInt().coerceAtLeast(0)
                    val wPrimePct = calculator.getWPrimePercentage().roundToInt().coerceIn(0, 100)
                    val fieldJ = FieldValue(wPrimeJField, wPrimeJ.toDouble())
                    val fieldPct = FieldValue(wPrimePctField, wPrimePct.toDouble())
                    when (rideState) {
                        is RideState.Idle -> { /* no write */ }
                        is RideState.Paused -> {
                            // Session-level write (infrequent)
                            emitter.onNext(WriteToSessionMesg(listOf(fieldJ, fieldPct)))
                        }
                        is RideState.Recording -> {
                            emitter.onNext(WriteToRecordMesg(listOf(fieldJ, fieldPct)))
                        }
                    }
                }
        }
        emitter.setCancellable { job.cancel() }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        WPrimeLogger.i(WPrimeLogger.Module.EXTENSION, LogConstants.EXTENSION_STARTED + " - Version 1.0")
        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            karooSystem.connect { connected ->
                if (connected) {
                    WPrimeLogger.i(WPrimeLogger.Module.EXTENSION, LogConstants.SERVICE_CONNECTED)
                    karooSystem.dispatch(
                        SystemNotification(
                            "wprime-started",
                            "W Prime Extension Started",
                            action = "Configure",
                            actionIntent = "com.itl.wprimeext.MAIN",
                        ),
                    )
                    WPrimeLogger.i(WPrimeLogger.Module.EXTENSION, LogConstants.NOTIFICATION_SENT + " - Extension started notification")
                } else {
                    WPrimeLogger.w(WPrimeLogger.Module.EXTENSION, "Failed to connect to Karoo service")
                }
            }
            launch {
                // Handle actions that can't be shown in MainActivity because
                // they are for in-ride scenarios. Receiving these intents is like
                // if an extension got a command from a sensor or API that maps to the in-ride actions.
                //
                // Test with: adb shell am broadcast -a io.hammerhead.wprime.IN_RIDE_ACTION --es action io.hammerhead.karooext.models.MarkLap
                // Works with any KarooEffect that has no required parameters:
                //  - MarkLap, PauseRide, ResumeRide, ShowMapPage, ZoomPage, TurnScreenOff, TurnScreenOn, and PerformHardwareActions
                callbackFlow {
                    val intentFilter = IntentFilter("io.hammerhead.wprime.IN_RIDE_ACTION")
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            trySend(intent)
                        }
                    }
                    registerReceiver(receiver, intentFilter)
                    awaitClose { unregisterReceiver(receiver) }
                }
                    .mapNotNull {
                        it.extras?.getString("action")?.let { action ->
                            WPrimeLogger.d(WPrimeLogger.Module.EXTENSION, LogConstants.INTENT_RECEIVED + " - Action: $action")
                            try {
                                val clazz = Class.forName(action).kotlin
                                (clazz.objectInstance ?: clazz.createInstance()) as? KarooEffect
                            } catch (e: Exception) {
                                WPrimeLogger.w(WPrimeLogger.Module.EXTENSION, e, "Unknown action $action")
                                null
                            }
                        }
                    }
                    .collect { effect ->
                        WPrimeLogger.d(WPrimeLogger.Module.EXTENSION, "Dispatching KarooEffect: ${effect::class.simpleName}")
                        karooSystem.dispatch(effect)
                    }
            }
        }
    }

    override fun onDestroy() {
        WPrimeLogger.i(WPrimeLogger.Module.EXTENSION, LogConstants.EXTENSION_STOPPED)
        serviceJob?.cancel()
        serviceJob = null
        karooSystem.disconnect()
        WPrimeLogger.i(WPrimeLogger.Module.EXTENSION, LogConstants.SERVICE_DISCONNECTED)
        super.onDestroy()
    }
}
