package com.itl.wprimeextension.datatypes

import android.util.Log
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.StreamState

class PowerDataType : DataTypeImpl("wprime-id", "com.itl.wprimeextension.POWER") {

    companion object {
        private const val TAG = "PowerDataType"
    }

    private var onPowerUpdate: ((Double) -> Unit)? = null
    private var currentPowerValue: Double = 0.0
    private var streamEmitter: Emitter<StreamState>? = null

    override fun startStream(emitter: Emitter<StreamState>) {
        Log.d(TAG, "Starting power data stream")
        streamEmitter = emitter

        // Start with idle state
        emitter.onNext(StreamState.Idle)
    }

    fun handlePowerDataFromBike(powerValue: Double) {
        currentPowerValue = powerValue
        Log.d(TAG, "Power data received: ${powerValue}W")

        // Emit streaming state with power data
        streamEmitter?.onNext(
            StreamState.Streaming(
                DataPoint(
                    dataTypeId = typeId,
                    values = mapOf("value" to powerValue)
                )
            )
        )

        // Notify callback for W Prime calculation
        onPowerUpdate?.invoke(powerValue)
    }

    fun setOnPowerUpdate(callback: (Double) -> Unit) {
        onPowerUpdate = callback
    }
}
