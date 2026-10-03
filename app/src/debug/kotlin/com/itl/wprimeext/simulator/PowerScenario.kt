package com.itl.wprimeext.simulator

import com.itl.wprimeext.extension.WPrimeRideState

data class PowerSample(val timeMs: Long, val power: Double?, val event: String = "")

/** Strict, bounded CSV input; times are seconds from the start, blanks mean sensor silence. */
object PowerScenario {
    const val MAX_SAMPLES = 100_000
    fun parse(csv: String): List<PowerSample> {
        val lines = csv.lineSequence().filter { it.isNotBlank() && !it.trimStart().startsWith("#") }.iterator()
        require(lines.hasNext() && lines.next().trim().removePrefix("\uFEFF") == "time_s,power_w,event") {
            "Expected header: time_s,power_w,event"
        }
        val result = mutableListOf<PowerSample>()
        var previous = -1L
        lines.forEach { line ->
            require(result.size < MAX_SAMPLES) { "Too many samples" }
            val cells = line.split(',').map(String::trim)
            require(cells.size == 3) { "Expected three columns at sample ${result.size + 1}" }
            val seconds = cells[0].toDoubleOrNull()
            require(seconds != null && seconds.isFinite() && seconds >= 0 && seconds <= 604800) { "Invalid time" }
            val time = (seconds * 1000).toLong()
            require(time > previous) { "Times must increase strictly" }
            val power = cells[1].takeIf(String::isNotEmpty)?.let {
                it.toDoubleOrNull().also { value -> require(value != null && value.isFinite() && value in 0.0..2000.0) { "Invalid power" } }
            }
            val event = cells[2].uppercase()
            require(event in listOf("", "RECORDING", "PAUSED", "IDLE", "LOST", "FOUND")) { "Unknown event: $event" }
            result += PowerSample(time, power, event)
            previous = time
        }
        require(result.isNotEmpty()) { "No samples" }
        require(result.first().timeMs == 0L) { "First sample must start at time 0" }
        return result
    }

    fun builtIn(name: String): List<PowerSample> = (0..420).map { second ->
        val power = when (name) {
            "Intervals" -> if (second % 120 < 60) 400.0 else 100.0

            "Exhaustion" -> if (second < 180) 450.0 else 0.0

            "Sensor loss" -> if (second in 90..150) {
                null
            } else if (second < 90) {
                400.0
            } else {
                100.0
            }

            else -> if (second < 90) 400.0 else 0.0
        }
        val event = if (name == "Sensor loss") {
            when (second) {
                90 -> "LOST"
                151 -> "FOUND"
                else -> ""
            }
        } else {
            ""
        }
        PowerSample(second * 1000L, power, event)
    }

    fun rideState(event: String): WPrimeRideState? = when (event) {
        "RECORDING" -> WPrimeRideState.RECORDING
        "PAUSED" -> WPrimeRideState.PAUSED
        "IDLE" -> WPrimeRideState.IDLE
        else -> null
    }
}
