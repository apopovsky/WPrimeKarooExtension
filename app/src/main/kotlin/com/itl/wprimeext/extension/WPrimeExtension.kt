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
import io.hammerhead.karooext.models.WriteEventMesg
import io.hammerhead.karooext.models.WriteToRecordMesg
import io.hammerhead.karooext.models.WriteToSessionMesg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.absoluteValue
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
        )
    }

    private val doughnutsField by lazy {
        DeveloperField(
            fieldDefinitionNumber = 0,
            fitBaseTypeId = 136, // FitBaseType.Float32
            fieldName = "Doughnuts Earned",
            units = "doughnuts",
        )
    }

    override fun startFit(emitter: Emitter<FitEffect>) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            karooSystem.streamDataFlow(DataType.Type.ELAPSED_TIME)
                .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue?.div(1000) }
                .combine(karooSystem.consumerFlow<RideState>()) { seconds, rideState ->
                    Pair(seconds, rideState)
                }
                .collect { (seconds, rideState) ->
                    // One to start and another one earned every 20 minutes (rounded to 0.1)
                    val doughnuts = 1 + (seconds / 120.0).roundToInt() / 10.0
                    val doughnutsField = FieldValue(doughnutsField, doughnuts)
                    when (rideState) {
                        is RideState.Idle -> {}
                        // When paused, write to SessionMesg so it's committed infrequently
                        // Last set will be saved at end of activity
                        is RideState.Paused -> {
                            WPrimeLogger.logDataFlow(WPrimeLogger.Module.EXTENSION, "FIT session write", "doughnuts: $doughnuts")
                            emitter.onNext(WriteToSessionMesg(doughnutsField))
                        }
                        // When recording, write doughnuts and power to record messages
                        is RideState.Recording -> {
                            WPrimeLogger.logDataFlow(WPrimeLogger.Module.EXTENSION, "FIT record write", "doughnuts: $doughnuts")
                            emitter.onNext(WriteToRecordMesg(doughnutsField))

                            // Power: saw-tooth [100, 200]
                            val fakePower = 100 + seconds.mod(200.0).minus(100).absoluteValue
                            WPrimeLogger.logDataFlow(WPrimeLogger.Module.EXTENSION, "Fake power generated", "${fakePower}W at ${seconds}s")
                            emitter.onNext(
                                WriteToRecordMesg(
                                    /**
                                     * From FIT SDK:
                                     * public static final int PowerFieldNum = 7;
                                     */
                                    FieldValue(7, fakePower),
                                ),
                            )
                        }
                    }
                    if (seconds == 42.0) {
                        // Off-course marker at 42 seconds with doughnuts included
                        WPrimeLogger.i(WPrimeLogger.Module.EXTENSION, LogConstants.FIT_EVENT_WRITTEN + " - Off-course marker at 42s")
                        emitter.onNext(
                            WriteEventMesg(
                                event = 7, // OFF_COURSE((short)7),
                                eventType = 3, // MARKER((short)3),
                                values = listOf(doughnutsField),
                            ),
                        )
                    }
                }
        }
        emitter.setCancellable {
            job.cancel()
        }
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
