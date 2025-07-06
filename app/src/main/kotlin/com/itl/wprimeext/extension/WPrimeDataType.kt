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

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber

class WPrimeDataType(
    private val karooSystem: KarooSystemService,
    extension: String,
) : DataTypeImpl(extension, "wprime") {
    override fun startStream(emitter: Emitter<StreamState>) {
        Timber.d("start power stream")
        val job = CoroutineScope(Dispatchers.IO).launch {
            val powerFlow = karooSystem.streamDataFlow(DataType.Type.POWER)
            powerFlow.collect { power ->
                if (power is StreamState.Streaming) {
                    val powerValue = power.dataPoint.singleValue?.toDouble() ?: 0.0
                    emitter.onNext(
                        StreamState.Streaming(
                            DataPoint(
                                dataTypeId,
                                values = mapOf(DataType.Field.SINGLE to powerValue),
                            ),
                        )
                    )
                } else {
                    emitter.onNext(StreamState.NotAvailable)
                }
            }
        }
        emitter.setCancellable {
            Timber.d("stop power stream")
            job.cancel()
        }
    }
}
