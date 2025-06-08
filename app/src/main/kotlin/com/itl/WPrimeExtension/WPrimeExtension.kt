package com.itl.wprimeextension

import android.util.Log
import com.itl.wprimeextension.data.WPrimeSettings
import com.itl.wprimeextension.datatypes.PowerDataType
import com.itl.wprimeextension.datatypes.WPrimeDataType
import com.itl.wprimeextension.wprime.WPrimeCalculator
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.extension.KarooExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WPrimeExtension : KarooExtension("wprime-id", "1.0") {

    companion object {
        private const val TAG = "WPrimeExtension"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var wPrimeSettings: WPrimeSettings
    private lateinit var wPrimeCalculator: WPrimeCalculator
    private var isInitialized = false

    private lateinit var powerDataType: PowerDataType
    private lateinit var wPrimeDataType: WPrimeDataType

    override val types: List<DataTypeImpl>
        get() = listOf(powerDataType, wPrimeDataType)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "WPrimeExtension onCreate")

        wPrimeSettings = WPrimeSettings(this)

        // Initialize data types
        powerDataType = PowerDataType()
        wPrimeDataType = WPrimeDataType()

        initializeWPrimeCalculator()
        setupDataTypeConnections()
    }

    private fun setupDataTypeConnections() {
        // Connect power updates to W Prime calculation
        powerDataType.setOnPowerUpdate { powerValue ->
            if (isInitialized) {
                val currentWPrime = wPrimeCalculator.updatePower(powerValue, System.currentTimeMillis())
                wPrimeDataType.updateWPrime(currentWPrime)
                Log.d(TAG, "W Prime calculated: ${currentWPrime.toInt()}J (Power: ${powerValue}W)")
            }
        }
    }

    private fun initializeWPrimeCalculator() {
        scope.launch {
            try {
                val config = wPrimeSettings.configuration.first()
                wPrimeCalculator = WPrimeCalculator(
                    criticalPower = config.criticalPower,
                    anaerobicCapacity = config.anaerobicCapacity,
                    tauRecovery = config.tauRecovery,                )
                isInitialized = true
                Log.d(TAG, "W Prime Calculator initialized with CP: ${config.criticalPower}W, W': ${config.anaerobicCapacity}J, Tau: ${config.tauRecovery}s")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize W Prime Calculator", e)
            }
        }

        // Listen for configuration changes
        scope.launch {
            wPrimeSettings.configuration.collect { config: com.itl.wprimeextension.data.WPrimeConfiguration ->
                if (isInitialized) {
                    wPrimeCalculator.updateConfiguration(
                        config.criticalPower,
                        config.anaerobicCapacity,
                        config.tauRecovery,
                    )
                    Log.d(TAG, "W Prime Calculator configuration updated")
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "WPrimeExtension onDestroy")
        super.onDestroy()
    }
}
