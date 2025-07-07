package com.itl.wprimeext.extension

import com.itl.wprimeext.utils.WPrimeLogger
import com.itl.wprimeext.utils.LogConstants
import kotlin.math.exp
import kotlin.math.max

class WPrimeCalculator(
    private var criticalPower: Double,
    private var anaerobicCapacity: Double,
    private var tauRecovery: Double,
) {
    private var currentWPrime: Double = anaerobicCapacity
    private var lastUpdateTime: Long = 0

    fun updateConfiguration(criticalPower: Double, anaerobicCapacity: Double, tauRecovery: Double) {
        WPrimeLogger.logConfiguration(
            WPrimeLogger.Module.CALCULATOR,
            criticalPower,
            anaerobicCapacity,
            tauRecovery
        )
        this.criticalPower = criticalPower
        this.anaerobicCapacity = anaerobicCapacity
        this.tauRecovery = tauRecovery
        // Reset W Prime when configuration changes
        this.currentWPrime = anaerobicCapacity
        WPrimeLogger.i(WPrimeLogger.Module.CALCULATOR, LogConstants.WPRIME_CONFIG_UPDATED)
    }

    fun updatePower(power: Double, timestamp: Long): Double {
        if (lastUpdateTime == 0L) {
            lastUpdateTime = timestamp
            WPrimeLogger.i(WPrimeLogger.Module.CALCULATOR, LogConstants.WPRIME_INITIALIZED)
            return currentWPrime
        }

        val deltaTime = (timestamp - lastUpdateTime) / 1000.0 // Convert to seconds
        val oldWPrime = currentWPrime

        currentWPrime = when {
            power > criticalPower -> {
                // W Prime depletion when power is above critical power
                val depletion = (power - criticalPower) * deltaTime
                val newWPrime = max(0.0, currentWPrime - depletion)

                if (newWPrime == 0.0 && oldWPrime > 0.0) {
                    WPrimeLogger.w(WPrimeLogger.Module.CALCULATOR, LogConstants.WPRIME_DEPLETED)
                }

                newWPrime
            }
            power < criticalPower -> {
                // W Prime recovery when power is below critical power
                val recovery = (anaerobicCapacity - currentWPrime) * (1 - exp(-deltaTime / tauRecovery))
                val newWPrime = currentWPrime + recovery

                if (oldWPrime < anaerobicCapacity * 0.5 && newWPrime >= anaerobicCapacity * 0.5) {
                    WPrimeLogger.i(WPrimeLogger.Module.CALCULATOR, LogConstants.WPRIME_RECOVERING + " - 50% recovered")
                }

                // Cap at anaerobic capacity
                if (newWPrime > anaerobicCapacity) anaerobicCapacity else newWPrime
            }
            else -> {
                // No change when power equals critical power
                currentWPrime
            }
        }

        // Log detailed power updates only for significant changes or every 30 seconds
        val significantChange = kotlin.math.abs(oldWPrime - currentWPrime) > (anaerobicCapacity * 0.05) // 5% change
        val periodicLog = (timestamp / 1000) % 30 == 0L // Every 30 seconds

        if (significantChange || periodicLog) {
            WPrimeLogger.logPowerUpdate(
                WPrimeLogger.Module.CALCULATOR,
                power,
                currentWPrime,
                getWPrimePercentage()
            )
        }

        lastUpdateTime = timestamp
        return currentWPrime
    }

    fun getCurrentWPrime(): Double = currentWPrime

    fun getWPrimePercentage(): Double = (currentWPrime / anaerobicCapacity) * 100.0

    fun reset() {
        WPrimeLogger.i(WPrimeLogger.Module.CALCULATOR, "W Prime calculator reset - W': ${anaerobicCapacity}J")
        currentWPrime = anaerobicCapacity
        lastUpdateTime = 0
    }

    fun getTimeToExhaustion(currentPower: Double): Double? {
        return if (currentPower > criticalPower && currentWPrime > 0) {
            currentWPrime / (currentPower - criticalPower)
        } else {
            null
        }
    }

    fun getTimeToFullRecovery(currentPower: Double): Double? {
        return if (currentPower < criticalPower && currentWPrime < anaerobicCapacity) {
            -tauRecovery * kotlin.math.ln(currentWPrime / anaerobicCapacity)
        } else {
            null
        }
    }
}
