package com.itl.wprimeext.extension

import com.itl.wprimeext.utils.WPrimeLogger
import com.itl.wprimeext.utils.LogConstants
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.ln
import kotlin.math.abs

class WPrimeCalculator(
    private var criticalPower: Double,
    private var anaerobicCapacity: Double,
    private var tauRecovery: Double,
) {
    private var currentWPrime: Double = anaerobicCapacity
    private var lastUpdateTime: Long = 0

    companion object {
        private const val MILLISECONDS_TO_SECONDS = 1000.0
        private const val MAX_DELTA_TIME_SECONDS = 3600.0 // 1 hour max jump
        private const val MIN_POWER = 0.0
        private const val MAX_POWER = 2000.0 // Reasonable upper limit
        private const val EPSILON = 1e-6
    }

    fun updateConfiguration(criticalPower: Double, anaerobicCapacity: Double, tauRecovery: Double) {
        require(criticalPower > 0) { "Critical Power must be positive" }
        require(anaerobicCapacity > 0) { "Anaerobic Capacity must be positive" }
        require(tauRecovery > 0) { "Tau Recovery must be positive" }

        WPrimeLogger.logConfiguration(
            WPrimeLogger.Module.CALCULATOR,
            criticalPower,
            anaerobicCapacity,
            tauRecovery
        )

        this.criticalPower = criticalPower
        this.anaerobicCapacity = anaerobicCapacity
        this.tauRecovery = tauRecovery
        this.currentWPrime = anaerobicCapacity

        WPrimeLogger.i(WPrimeLogger.Module.CALCULATOR, LogConstants.WPRIME_CONFIG_UPDATED)
    }    fun updatePower(power: Double, timestamp: Long): Double {
        // Input validation
        if (power < MIN_POWER || power > MAX_POWER) {
            WPrimeLogger.w(WPrimeLogger.Module.CALCULATOR, "Invalid power value: $power. Using 0W")
            return updatePowerInternal(0.0, timestamp)
        }

        return updatePowerInternal(power, timestamp)
    }

    private fun updatePowerInternal(power: Double, timestamp: Long): Double {
        if (lastUpdateTime == 0L) {
            lastUpdateTime = timestamp
            WPrimeLogger.i(WPrimeLogger.Module.CALCULATOR, LogConstants.WPRIME_INITIALIZED)
            return currentWPrime
        }

        val deltaTime = validateDeltaTime(timestamp)
        if (deltaTime <= EPSILON) {
            return currentWPrime // No time passed
        }

        val oldWPrime = currentWPrime

        currentWPrime = when {
            power > criticalPower -> {
                calculateDepletion(power, deltaTime, oldWPrime)
            }
            power < criticalPower -> {
                calculateRecovery(power, deltaTime, oldWPrime)
            }
            else -> {
                // Power exactly equals CP - no change in W'
                currentWPrime
            }
        }

        logSignificantChanges(power, oldWPrime, timestamp)
        lastUpdateTime = timestamp
        return currentWPrime
    }

    private fun validateDeltaTime(timestamp: Long): Double {
        val deltaTime = (timestamp - lastUpdateTime) / MILLISECONDS_TO_SECONDS

        return when {
            deltaTime < 0 -> {
                WPrimeLogger.w(WPrimeLogger.Module.CALCULATOR, "Negative deltaTime detected: $deltaTime. Ignoring update.")
                0.0
            }
            deltaTime > MAX_DELTA_TIME_SECONDS -> {
                WPrimeLogger.w(WPrimeLogger.Module.CALCULATOR, "Excessive deltaTime: $deltaTime. Capping to $MAX_DELTA_TIME_SECONDS")
                MAX_DELTA_TIME_SECONDS
            }
            else -> deltaTime
        }
    }

    private fun calculateDepletion(power: Double, deltaTime: Double, oldWPrime: Double): Double {
        val depletion = (power - criticalPower) * deltaTime
        val newWPrime = max(0.0, currentWPrime - depletion)

        if (newWPrime == 0.0 && oldWPrime > 0.0) {
            WPrimeLogger.w(WPrimeLogger.Module.CALCULATOR, LogConstants.WPRIME_DEPLETED)
        }

        return newWPrime
    }

    private fun calculateRecovery(power: Double, deltaTime: Double, oldWPrime: Double): Double {
        // Improved recovery model considering power deficit intensity
        val powerDeficit = criticalPower - power
        val recoveryIntensity = min(1.0, powerDeficit / criticalPower) // Normalized [0,1]

        // Modified tau based on recovery intensity (faster recovery with larger deficit)
        val effectiveTau = tauRecovery / (1.0 + recoveryIntensity * 0.5)

        val recovery = (anaerobicCapacity - currentWPrime) * (1 - exp(-deltaTime / effectiveTau))
        val newWPrime = min(anaerobicCapacity, currentWPrime + recovery)

        // Log recovery milestones
        if (oldWPrime < anaerobicCapacity * 0.5 && newWPrime >= anaerobicCapacity * 0.5) {
            WPrimeLogger.i(WPrimeLogger.Module.CALCULATOR, LogConstants.WPRIME_RECOVERING + " - 50% recovered")
        }
        if (oldWPrime < anaerobicCapacity * 0.9 && newWPrime >= anaerobicCapacity * 0.9) {
            WPrimeLogger.i(WPrimeLogger.Module.CALCULATOR, LogConstants.WPRIME_RECOVERING + " - 90% recovered")
        }

        return newWPrime
    }

    private fun logSignificantChanges(power: Double, oldWPrime: Double, timestamp: Long) {
        val significantChange = abs(oldWPrime - currentWPrime) > (anaerobicCapacity * 0.05) // 5% change
        val periodicLog = (timestamp / 1000) % 30 == 0L // Every 30 seconds

        if (significantChange || periodicLog) {
            WPrimeLogger.logPowerUpdate(
                WPrimeLogger.Module.CALCULATOR,
                power,
                currentWPrime,
                getWPrimePercentage()
            )
        }
    }

    fun getCurrentWPrime(): Double = currentWPrime

    fun getWPrimePercentage(): Double = (currentWPrime / anaerobicCapacity) * 100.0

    fun reset() {
        WPrimeLogger.i(WPrimeLogger.Module.CALCULATOR, "W Prime calculator reset - W': ${anaerobicCapacity}J")
        currentWPrime = anaerobicCapacity
        lastUpdateTime = 0
    }

    fun getTimeToExhaustion(currentPower: Double): Double? {
        return if (currentPower > criticalPower && currentWPrime > EPSILON) {
            max(0.0, currentWPrime / (currentPower - criticalPower))
        } else {
            null
        }
    }

    fun getTimeToFullRecovery(currentPower: Double): Double? {
        return if (currentPower < criticalPower && currentWPrime < anaerobicCapacity - EPSILON) {
            val powerDeficit = criticalPower - currentPower
            val recoveryIntensity = min(1.0, powerDeficit / criticalPower)
            val effectiveTau = tauRecovery / (1.0 + recoveryIntensity * 0.5)

            // Time to reach 99% recovery
            -effectiveTau * ln((anaerobicCapacity - currentWPrime) / anaerobicCapacity * 0.01)
        } else {
            null
        }
    }

    // Additional utility functions
    fun getWPrimeJoules(): Double = currentWPrime

    fun getWPrimeKilojoules(): Double = currentWPrime / 1000.0

    fun isFullyDepleted(): Boolean = currentWPrime < EPSILON

    fun isFullyRecovered(): Boolean = abs(currentWPrime - anaerobicCapacity) < EPSILON

    fun getDepletion(): Double = anaerobicCapacity - currentWPrime

    fun getDepletionPercentage(): Double = (getDepletion() / anaerobicCapacity) * 100.0
}
