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

import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.ConnectionStatus
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.Device
import io.hammerhead.karooext.models.DeviceEvent
import io.hammerhead.karooext.models.OnConnectionStatus
import io.hammerhead.karooext.models.OnDataPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Test power data source that simulates realistic cycling power patterns.
 * Useful for testing W Prime calculations without needing a real power sensor.
 */
class TestPowerDataSource(extension: String) {
    val source by lazy {
        Device(
            extension,
            "test-power-sensor",
            listOf(DataType.Type.POWER),
            "Test Power Sensor",
        )
    }

    private var simulationTime = 0.0
    private var isConnected = false

    fun connect(emitter: Emitter<DeviceEvent>) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            emitter.onNext(OnConnectionStatus(ConnectionStatus.CONNECTED))
            isConnected = true

            // Simulate realistic cycling power patterns
            repeat(Int.MAX_VALUE) {
                val powerValue = generateRealisticPower()

                emitter.onNext(
                    OnDataPoint(
                        DataPoint(
                            DataType.Type.POWER,
                            values = mapOf(DataType.Field.SINGLE to powerValue),
                            sourceId = source.uid,
                        ),
                    ),
                )

                simulationTime += 1.0 // Increment by 1 second
                delay(1000) // Send data every second
            }
            awaitCancellation()
        }

        emitter.setCancellable {
            isConnected = false
            job.cancel()
        }
    }

    private fun generateRealisticPower(): Double {
        // Create realistic cycling power patterns
        val baseTime = simulationTime / 60.0 // Convert to minutes

        return when {
            // Warmup phase (0-5 minutes): gradually increasing power
            baseTime < 5.0 -> {
                val warmupProgress = baseTime / 5.0
                150.0 + (100.0 * warmupProgress) + Random.nextDouble(-20.0, 20.0)
            }

            // Interval training (5-15 minutes): alternating high/low power
            baseTime < 15.0 -> {
                val intervalPhase = ((baseTime - 5.0) % 2.0) // 2-minute intervals
                val basePower = if (intervalPhase < 1.0) 350.0 else 200.0 // High/low intervals
                basePower + Random.nextDouble(-30.0, 30.0)
            }

            // Steady state (15-25 minutes): around critical power
            baseTime < 25.0 -> {
                250.0 + Random.nextDouble(-40.0, 40.0)
            }

            // Sprint intervals (25-30 minutes): very high power bursts
            baseTime < 30.0 -> {
                val sprintPhase = ((baseTime - 25.0) % 1.5) // 1.5-minute cycles
                val basePower = if (sprintPhase < 0.3) 500.0 else 180.0 // Sprint/recovery
                basePower + Random.nextDouble(-50.0, 50.0)
            }

            // Cool down (30+ minutes): decreasing power
            else -> {
                val cooldownTime = baseTime - 30.0
                val cooldownPower = 200.0 - (cooldownTime * 5.0) // Gradual decrease
                (cooldownPower.coerceAtLeast(80.0)) + Random.nextDouble(-15.0, 15.0)
            }
        }.coerceAtLeast(0.0)
    }

    companion object {
        const val PREFIX = "test-power"
    }
}
