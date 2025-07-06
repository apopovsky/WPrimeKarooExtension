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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

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

    override fun startStream(emitter: Emitter<StreamState>) {
        Timber.d("start W Prime stream")
        val job = CoroutineScope(Dispatchers.IO).launch {
            // Load configuration from DataStore at startup
            try {
                val config = wprimeSettings.configuration.first()
                wprimeCalculator.updateConfiguration(
                    config.criticalPower,
                    config.anaerobicCapacity,
                    config.tauRecovery
                )
                Timber.d("W Prime configuration loaded: CP=${config.criticalPower}W, W'=${config.anaerobicCapacity}J, Tau=${config.tauRecovery}s")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load W Prime configuration, using defaults")
            }

            val powerFlow = karooSystem.streamDataFlow(DataType.Type.POWER)
            powerFlow.collect { power ->
                if (power is StreamState.Streaming) {
                    val powerValue = power.dataPoint.singleValue?.toDouble() ?: 0.0
                    val timestamp = System.currentTimeMillis()

                    // Calculate W Prime based on current power
                    val currentWPrime = wprimeCalculator.updatePower(powerValue, timestamp)

                    emitter.onNext(
                        StreamState.Streaming(
                            DataPoint(
                                dataTypeId,
                                values = mapOf(DataType.Field.SINGLE to currentWPrime),
                            ),
                        ),
                    )
                } else {
                    emitter.onNext(StreamState.NotAvailable)
                }
            }
        }
        emitter.setCancellable {
            Timber.d("stop W Prime stream")
            job.cancel()
        }
    }
}
