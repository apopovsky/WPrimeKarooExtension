package com.itl.wprimeextension.datatypes

import android.util.Log
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.StreamState

class WPrimeDataType : DataTypeImpl("wprime-id", "com.itl.wprimeextension.WPRIME") {

    companion object {
        private const val TAG = "WPrimeDataType"
    }

    private var currentWPrimeValue: Double = 0.0
    private var streamEmitter: Emitter<StreamState>? = null

    override fun startStream(emitter: Emitter<StreamState>) {
        Log.d(TAG, "Starting W Prime data stream")
        streamEmitter = emitter

        // Start with idle state
        emitter.onNext(StreamState.Idle)
    }

    fun updateWPrime(wPrimeValue: Double) {
        currentWPrimeValue = wPrimeValue
        Log.d(TAG, "W Prime value updated: ${wPrimeValue.toInt()}J")

        // Emit streaming state with W Prime data
        streamEmitter?.onNext(
            StreamState.Streaming(
                DataPoint(
                    dataTypeId = typeId,
                    values = mapOf("value" to wPrimeValue)
                )
            )
        )
    }
}
