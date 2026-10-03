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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.itl.wprimeext.BuildConfig
import com.itl.wprimeext.utils.LogConstants
import com.itl.wprimeext.utils.WPrimeLogger
import dagger.hilt.android.AndroidEntryPoint
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DeveloperField
import io.hammerhead.karooext.models.FieldValue
import io.hammerhead.karooext.models.FitEffect
import io.hammerhead.karooext.models.WriteToRecordMesg
import io.hammerhead.karooext.models.WriteToSessionMesg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WPrimeExtension : KarooExtension("wprime-id", BuildConfig.VERSION_NAME) {
    @Inject
    lateinit var karooSystem: KarooSystemService

    private var serviceJob: Job? = null
    private val runtime by lazy { WPrimeRuntime(this, karooSystem) }

    override val types by lazy {
        listOf(
            WPrimeDataType(extension, runtime),
            WPrimeKjDataType(extension, runtime),
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
            val projection = WPrimeFitProjection()
            runtime.state.collect { snapshot ->
                val values = projection.next(snapshot) ?: return@collect
                val fields = listOf(FieldValue(wPrimeJField, values.joules), FieldValue(wPrimePctField, values.percentage))
                when (values.message) {
                    WPrimeFitMessage.RECORD -> emitter.onNext(WriteToRecordMesg(fields))
                    WPrimeFitMessage.SESSION -> emitter.onNext(WriteToSessionMesg(fields))
                }
            }
        }
        emitter.setCancellable { job.cancel() }
    }
    override fun onCreate() {
        super.onCreate()
        WPrimeLogger.i(
            WPrimeLogger.Module.EXTENSION,
            LogConstants.EXTENSION_STARTED + " - Version ${BuildConfig.VERSION_NAME}",
        )

        runtime.start()
        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            karooSystem.connect { connected ->
                if (connected) {
                    WPrimeLogger.i(WPrimeLogger.Module.EXTENSION, LogConstants.SERVICE_CONNECTED)
                } else {
                    WPrimeLogger.w(WPrimeLogger.Module.EXTENSION, "Failed to connect to Karoo service")
                }
            }
            launch {
                val manager = WPrimeAlertManager(karooSystem)
                var previousTest: Long? = null
                broadcastFlow("io.hammerhead.wprime.TEST_ALERT", exported = BuildConfig.DEBUG).collect { intent ->
                    val alert = runCatching {
                        testAlertCommand(
                            intent.getStringExtra("alertId"),
                            intent.getIntExtra("threshold", -1),
                            intent.getBooleanExtra("soundEnabled", false),
                            intent.getStringExtra("alertType"),
                        )
                    }.getOrNull() ?: return@collect
                    val now = SystemClock.elapsedRealtime()
                    if (previousTest?.let { now - it < 1000L } == true) return@collect
                    previousTest = now
                    manager.testAlert(alert, alert.thresholdPercentage.toDouble())
                }
            }
            if (BuildConfig.DEBUG) {
                launch {
                    broadcastFlow("io.hammerhead.wprime.IN_RIDE_ACTION", exported = true)
                        .mapNotNull { intent -> runCatching { debugRideEffect(intent.getStringExtra("action")) }.getOrNull() }
                        .collect { effect -> karooSystem.dispatch(effect) }
                }
            }
        }
    }

    private fun broadcastFlow(action: String, exported: Boolean) = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(intent)
            }
        }
        ContextCompat.registerReceiver(
            this@WPrimeExtension,
            receiver,
            IntentFilter(action),
            if (exported) ContextCompat.RECEIVER_EXPORTED else ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        awaitClose { unregisterReceiver(receiver) }
    }
    override fun onDestroy() {
        WPrimeLogger.i(WPrimeLogger.Module.EXTENSION, LogConstants.EXTENSION_STOPPED)
        serviceJob?.cancel()
        serviceJob = null
        runtime.stop()
        karooSystem.disconnect()
        WPrimeLogger.i(WPrimeLogger.Module.EXTENSION, LogConstants.SERVICE_DISCONNECTED)
        super.onDestroy()
    }
}
