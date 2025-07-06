package com.itl.wprimeextension.wprime

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
        this.criticalPower = criticalPower
        this.anaerobicCapacity = anaerobicCapacity
        this.tauRecovery = tauRecovery
        // Reset W Prime when configuration changes
        this.currentWPrime = anaerobicCapacity
    }

    fun updatePower(power: Double, timestamp: Long): Double {
        if (lastUpdateTime == 0L) {
            lastUpdateTime = timestamp
            return currentWPrime
        }

        val deltaTime = (timestamp - lastUpdateTime) / 1000.0 // Convert to seconds

        currentWPrime = when {
            power > criticalPower -> {
                // W Prime depletion when power is above critical power
                val depletion = (power - criticalPower) * deltaTime
                max(0.0, currentWPrime - depletion)
            }
            power < criticalPower -> {
                // W Prime recovery when power is below critical power
                val recovery = (anaerobicCapacity - currentWPrime) * (1 - exp(-deltaTime / tauRecovery))
                val newWPrime = currentWPrime + recovery
                // Cap at anaerobic capacity
                if (newWPrime > anaerobicCapacity) anaerobicCapacity else newWPrime
            }
            else -> {
                // No change when power equals critical power
                currentWPrime
            }
        }

        lastUpdateTime = timestamp
        return currentWPrime
    }

    fun getCurrentWPrime(): Double = currentWPrime

    fun getWPrimePercentage(): Double = (currentWPrime / anaerobicCapacity) * 100.0

    fun reset() {
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
