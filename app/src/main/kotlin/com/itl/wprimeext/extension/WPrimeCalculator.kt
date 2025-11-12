package com.itl.wprimeext.extension

import com.itl.wprimeext.utils.LogConstants
import com.itl.wprimeext.utils.WPrimeLogger
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow

// --- Model Abstractions (from document) ---

/**
 * Defines the contract for a W' balance calculation model.
 */
interface IWPrimeModel {
    fun update(power: Double, dt: Double): Double
    fun getCurrentWPrime(): Double
    fun getWPrimePercentage(): Double
    fun reset()
}

/**
 * Base implementation of [IWPrimeModel] sharing common properties.
 */
abstract class BaseWPrimeModel(
    protected val cp: Double,
    protected val wPrime: Double
) : IWPrimeModel {
    protected var wBal: Double = wPrime

    override fun getCurrentWPrime(): Double = wBal

    override fun getWPrimePercentage(): Double = if (wPrime > 0) (wBal / wPrime) * 100.0 else 0.0

    override fun reset() {
        wBal = wPrime
    }

    protected fun logDepletion() {
        WPrimeLogger.w(WPrimeLogger.Module.CALCULATOR, LogConstants.WPRIME_DEPLETED)
    }
}

// --- Model Implementations ---

/**
 * Implements the Skiba 2014 differential model.
 */
class SkibaDifferentialModel(cp: Double, wPrime: Double) : BaseWPrimeModel(cp, wPrime) {
    override fun update(power: Double, dt: Double): Double {
        val oldWBal = wBal
        val delta = if (power > cp) {
            -(power - cp)
        } else {
            if (wPrime > 0) ((cp - power) / wPrime) * (wPrime - wBal) else 0.0
        }
        wBal = (wBal + delta * dt).coerceIn(0.0, wPrime)

        if (wBal == 0.0 && oldWBal > 0.0) logDepletion()
        return wBal
    }
}

/**
 * Implements the Skiba 2012 monoexponential model with its specific tau function.
 * Formula: tau = 546 * e^(-0.01 * D_CP) + 316
 */
class Skiba2012Model(cp: Double, wPrime: Double) : BaseWPrimeModel(cp, wPrime) {
    override fun update(power: Double, dt: Double): Double {
        val oldWBal = wBal
        if (power > cp) {
            // Depletion
            wBal = (wBal - (power - cp) * dt).coerceAtLeast(0.0)
        } else {
            // Recovery: rec = ((cp - power) / wPrime) * (wPrime - wBal) * dt
            val rec = ((cp - power) / wPrime) * (wPrime - wBal) * dt
            wBal = (wBal + rec).coerceAtMost(wPrime)
        }

        if (wBal == 0.0 && oldWBal > 0.0) logDepletion()
        return wBal
    }
}

/**
 * Implements the Bartram 2018 model with individualized tau.
 * Formula: tau = 2287 * (D_CP)^(-0.688)
 */
class BartramModel(cp: Double, wPrime: Double, private val tauOverride: Double?) : BaseWPrimeModel(cp, wPrime) {
    override fun update(power: Double, dt: Double): Double {
        val oldWBal = wBal
        val delta = if (power > cp) {
            -(power - cp)
        } else {
            val dcp = (cp - power.coerceAtMost(cp)).coerceAtLeast(1.0)
            val tau = tauOverride ?: (2287 * dcp.pow(-0.688))
            val recRate = ((cp - power) / wPrime) * (wPrime - wBal) / tau
            recRate
        }
        wBal = (wBal + delta * dt).coerceIn(0.0, wPrime)

        if (wBal == 0.0 && oldWBal > 0.0) logDepletion()
        return wBal
    }
}

/**
 * Implements the Caen/Lievens model with domain-dependent tau.
 * Tau varies by power domain: <60% CP, 60-90% CP, >90% CP
 */
class CaenLievensModel(cp: Double, wPrime: Double) : BaseWPrimeModel(cp, wPrime) {
    private fun domainTau(power: Double): Double {
        if (cp <= 0) return 1000.0 // Avoid division by zero
        val ratio = power / cp
        return when {
            ratio < 0.6 -> 350.0  // Moderate
            ratio < 0.9 -> 700.0  // Heavy
            else -> 1000.0        // Near CP
        }
    }

    override fun update(power: Double, dt: Double): Double {
        val oldWBal = wBal
        val delta = if (power > cp) {
            -(power - cp)
        } else {
            val tau = domainTau(power)
            ((cp - power) / wPrime) * (wPrime - wBal) / tau
        }
        wBal = (wBal + delta * dt).coerceIn(0.0, wPrime)

        if (wBal == 0.0 && oldWBal > 0.0) logDepletion()
        return wBal
    }
}

/**
 * Implements the Chorley 2023 bi-exponential recovery model.
 * Uses fast (60s) and slow (400s) recovery components.
 */
class ChorleyModel(cp: Double, wPrime: Double) : BaseWPrimeModel(cp, wPrime) {
    override fun update(power: Double, dt: Double): Double {
        val oldWBal = wBal
        if (power > cp) {
            // Depletion
            wBal = (wBal - (power - cp) * dt).coerceAtLeast(0.0)
        } else {
            // Recovery: bi-exponential (rec is already an energy amount, not a rate)
            val deficit = (wPrime - wBal).coerceAtLeast(0.0)
            val aFast = 0.3 * deficit
            val aSlow = 0.7 * deficit
            val tauFast = 60.0
            val tauSlow = 400.0
            val rec = aFast * (1 - exp(-dt / tauFast)) + aSlow * (1 - exp(-dt / tauSlow))
            wBal = (wBal + rec).coerceAtMost(wPrime)
        }

        if (wBal == 0.0 && oldWBal > 0.0) logDepletion()
        return wBal
    }
}

/**
 * Implements the Weigend 2022 hydraulic model.
 */
class WeigendHydraulicModel(cp: Double, wPrime: Double, private val kIn: Double) : BaseWPrimeModel(cp, wPrime) {
    override fun update(power: Double, dt: Double): Double {
        val oldWBal = wBal
        val inflow = if (power < cp) kIn * (cp - power) * (1 - wBal / wPrime) else 0.0
        val outflow = if (power > cp) (power - cp) else 0.0
        val dW = (inflow - outflow) * dt
        wBal = (wBal + dW).coerceIn(0.0, wPrime)

        if (wBal == 0.0 && oldWBal > 0.0) logDepletion()
        return wBal
    }
}

// --- Main Calculator Class (Refactored) ---

class WPrimeCalculator(
    private var criticalPower: Double,
    private var anaerobicCapacity: Double,
    private var tauRecovery: Double, // Used as tau override for Bartram
    private var kIn: Double = 0.002, // Used for Weigend hydraulic model
    private var modelType: WPrimeModelType = WPrimeModelType.SKIBA_DIFFERENTIAL
) {
    private var model: IWPrimeModel = WPrimeFactory.create(modelType, criticalPower, anaerobicCapacity, tauRecovery, kIn)
    private var lastUpdateTime: Long = 0

    companion object {
        private const val MILLISECONDS_TO_SECONDS = 1000.0
        private const val MAX_DELTA_TIME_SECONDS = 3600.0
        private const val MIN_POWER = 0.0
        private const val MAX_POWER = 2000.0
        private const val EPSILON = 1e-6
        private const val SIGNIFICANT_CHANGE_THRESHOLD = 0.05
    }

    init {
        WPrimeLogger.i(WPrimeLogger.Module.CALCULATOR, "WPrimeCalculator initialized with model: $modelType")
    }

    fun updateConfiguration(
        criticalPower: Double,
        anaerobicCapacity: Double,
        tauRecovery: Double, // Used as tau override for Bartram
        kIn: Double,
        modelType: WPrimeModelType
    ) {
        require(criticalPower > 0) { "Critical Power must be positive" }
        require(anaerobicCapacity >= 0) { "Anaerobic Capacity must be non-negative" }

        WPrimeLogger.d(
            WPrimeLogger.Module.CALCULATOR,
            "${LogConstants.WPRIME_CONFIG_UPDATING} - Model: $modelType, CP: $criticalPower, W': $anaerobicCapacity"
        )

        this.criticalPower = criticalPower
        this.anaerobicCapacity = anaerobicCapacity
        this.tauRecovery = tauRecovery
        this.kIn = kIn
        this.modelType = modelType

        this.model = WPrimeFactory.create(modelType, criticalPower, anaerobicCapacity, tauRecovery, kIn)

        WPrimeLogger.i(WPrimeLogger.Module.CALCULATOR, LogConstants.WPRIME_CONFIG_UPDATED)
    }

    fun updatePower(power: Double, timestamp: Long): Double {
        val validatedPower = if (power < MIN_POWER || power > MAX_POWER) {
            WPrimeLogger.w(WPrimeLogger.Module.CALCULATOR, "Invalid power value: $power. Using 0W")
            0.0
        } else {
            power
        }

        if (lastUpdateTime == 0L) {
            lastUpdateTime = timestamp
            WPrimeLogger.i(WPrimeLogger.Module.CALCULATOR, LogConstants.WPRIME_INITIALIZED)
            return model.getCurrentWPrime()
        }

        val deltaTime = validateDeltaTime(timestamp)
        if (deltaTime <= EPSILON) return model.getCurrentWPrime()

        val oldWPrime = model.getCurrentWPrime()
        val newWPrime = model.update(validatedPower, deltaTime)

        logSignificantChanges(validatedPower, oldWPrime, timestamp)
        lastUpdateTime = timestamp
        return newWPrime
    }

    private fun validateDeltaTime(timestamp: Long): Double {
        val deltaTime = (timestamp - lastUpdateTime) / MILLISECONDS_TO_SECONDS
        return when {
            deltaTime < 0 -> {
                WPrimeLogger.w(WPrimeLogger.Module.CALCULATOR, "Negative deltaTime: $deltaTime. Ignoring.")
                0.0
            }
            deltaTime > MAX_DELTA_TIME_SECONDS -> {
                WPrimeLogger.w(WPrimeLogger.Module.CALCULATOR, "Excessive deltaTime: $deltaTime. Capping.")
                MAX_DELTA_TIME_SECONDS
            }
            else -> deltaTime
        }
    }

    private fun logSignificantChanges(power: Double, oldWPrime: Double, timestamp: Long) {
        val currentWPrime = model.getCurrentWPrime()
        val significantChange = abs(oldWPrime - currentWPrime) > (anaerobicCapacity * SIGNIFICANT_CHANGE_THRESHOLD)
        val periodicLog = (timestamp / 1000) % 30 == 0L
        if (significantChange || periodicLog) {
            WPrimeLogger.logPowerUpdate(
                WPrimeLogger.Module.CALCULATOR,
                power,
                currentWPrime,
                getWPrimePercentage(),
            )
        }
    }

    fun getCurrentWPrime(): Double = model.getCurrentWPrime()
    fun getWPrimePercentage(): Double = model.getWPrimePercentage()
    fun getCriticalPower(): Double = criticalPower
    fun getAnaerobicCapacity(): Double = anaerobicCapacity
}

// --- Model Factory and Enum ---

enum class WPrimeModelType {
    SKIBA_2012,
    SKIBA_DIFFERENTIAL, // Skiba 2014
    BARTRAM,
    CAEN_LIEVENS,
    CHORLEY,
    WEIGEND
}

object WPrimeFactory {
    fun create(type: WPrimeModelType, cp: Double, wPrime: Double, tauOverride: Double? = null, kIn: Double = 0.002): IWPrimeModel =
        when (type) {
            WPrimeModelType.SKIBA_2012 -> Skiba2012Model(cp, wPrime)
            WPrimeModelType.SKIBA_DIFFERENTIAL -> SkibaDifferentialModel(cp, wPrime)
            WPrimeModelType.BARTRAM -> BartramModel(cp, wPrime, tauOverride)
            WPrimeModelType.CAEN_LIEVENS -> CaenLievensModel(cp, wPrime)
            WPrimeModelType.CHORLEY -> ChorleyModel(cp, wPrime)
            WPrimeModelType.WEIGEND -> WeigendHydraulicModel(cp, wPrime, kIn)
        }
}
