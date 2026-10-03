package com.itl.wprimeext.extension

import io.hammerhead.karooext.models.StreamState

data class WPrimePowerInputResult(val snapshot: WPrimeSnapshot?, val lostSamples: Long, val stale: Boolean)

/** Detect bounded queue gaps rather than inventing wattage for samples no longer available. */
class WPrimePowerInput(private val engine: WPrimeEngine) {
    private var previousSequence = 0L

    fun accept(sample: TimedStreamState): WPrimePowerInputResult {
        val lost = (sample.sequence - previousSequence - 1).coerceAtLeast(0)
        previousSequence = sample.sequence
        if (sample.timestampMs < engine.snapshot().timestampMs) return WPrimePowerInputResult(null, lost, true)
        val power = (sample.state as? StreamState.Streaming)?.dataPoint?.singleValue
        if (lost > 0) engine.setSensorAvailable(false, sample.timestampMs)
        val snapshot = if (power != null && power.isFinite() && power in 0.0..2000.0) {
            engine.updatePower(power, sample.timestampMs)
        } else {
            engine.setSensorAvailable(false, sample.timestampMs)
        }
        return WPrimePowerInputResult(snapshot, lost, false)
    }
}
