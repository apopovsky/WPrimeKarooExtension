package com.itl.wprimeextension.datatypes

import android.util.Log
import com.itl.wprimeextension.data.WPrimeConfiguration
import com.itl.wprimeextension.streamDataFlow
import com.itl.wprimeextension.wprime.WPrimeCalculator
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class WPrimeDataType(
    private val karooSystem: KarooSystemService,
    private val configurationFlow: Flow<WPrimeConfiguration>,
    extension: String,
) : DataTypeImpl(extension, "wprime") {    companion object {
        private const val TAG = "WPrimeDataType"
    }

    private var wPrimeCalculator: WPrimeCalculator? = null
    private var streamJob: Job? = null
    private var configJob: Job? = null

    override fun startStream(emitter: Emitter<StreamState>) {
        Log.d(TAG, "startStream called - Beginning W Prime data stream initialization")

        try {
            Log.d(TAG, "Creating coroutine scopes for configuration and power data streaming")

            // Initialize the calculator with current configuration
        configJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Starting configuration monitoring coroutine")
                configurationFlow.collect { config ->
                    wPrimeCalculator = WPrimeCalculator(
                        criticalPower = config.criticalPower,
                        anaerobicCapacity = config.anaerobicCapacity,
                        tauRecovery = config.tauRecovery
                    )
                    Log.d(TAG, "W Prime Calculator initialized with CP: ${config.criticalPower}W, W': ${config.anaerobicCapacity}J, Tau: ${config.tauRecovery}s")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize W Prime Calculator", e)
            }
        }        // Start streaming power data and calculating W Prime
        streamJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Starting power data streaming coroutine")
                karooSystem.streamDataFlow(DataType.Type.POWER).collect { powerState ->
                    when (powerState) {
                        is StreamState.Streaming -> {
                            val powerValue = powerState.dataPoint.singleValue?.toDouble()
                            if (powerValue != null) {
                                Log.d(TAG, "Power data received: ${powerValue}W")

                                // Calculate W Prime if calculator is ready
                                wPrimeCalculator?.let { calculator ->
                                    val currentWPrime = calculator.updatePower(powerValue, System.currentTimeMillis())

                                    // Emit the calculated W Prime value
                                    emitter.onNext(
                                        StreamState.Streaming(
                                            DataPoint(
                                                dataTypeId = dataTypeId,
                                                values = mapOf(DataType.Field.SINGLE to currentWPrime)
                                            )
                                        )
                                    )

                                    Log.d(TAG, "W Prime calculated: ${currentWPrime.toInt()}J (Power: ${powerValue}W)")
                                } ?: run {
                                    // Calculator not ready yet, emit default value
                                    emitter.onNext(
                                        StreamState.Streaming(
                                            DataPoint(
                                                dataTypeId = dataTypeId,
                                                values = mapOf(DataType.Field.SINGLE to 22000.0)
                                            )
                                        )
                                    )
                                }
                            }
                        }
                        is StreamState.NotAvailable -> {
                            Log.d(TAG, "Power data not available")
                            emitter.onNext(StreamState.NotAvailable)
                        }
                        else -> {
                            Log.d(TAG, "Power data state: $powerState")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in power data streaming", e)
                emitter.onNext(StreamState.NotAvailable)
            }
        }        // Listen for configuration changes and update calculator
        val configUpdateJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                configurationFlow.collect { config ->
                    wPrimeCalculator?.updateConfiguration(
                        config.criticalPower,
                        config.anaerobicCapacity,
                        config.tauRecovery
                    )
                    Log.d(TAG, "W Prime Calculator configuration updated")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating configuration", e)
            }
        }

        emitter.setCancellable {
            Log.d(TAG, "Stopping W Prime data stream")
            streamJob?.cancel()
            configJob?.cancel()
            configUpdateJob.cancel()
        }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start W Prime data stream", e)
            emitter.onNext(StreamState.NotAvailable)
        }
    }
}
