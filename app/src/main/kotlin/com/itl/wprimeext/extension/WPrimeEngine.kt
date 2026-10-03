package com.itl.wprimeext.extension

enum class WPrimeRideState { IDLE, RECORDING, PAUSED }

data class WPrimeSnapshot(
    val wPrimeJoules: Double,
    val percentage: Double,
    val currentPower: Double,
    val criticalPower: Double,
    val anaerobicCapacity: Double,
    val configuration: WPrimeConfiguration,
    val rideState: WPrimeRideState,
    val sensorAvailable: Boolean,
    val timestampMs: Long,
    val alerts: List<WPrimeAlert> = emptyList(),
)

/** Deterministic state owner shared by the host runtime and debug simulator. Times are monotonic. */
class WPrimeEngine(configuration: WPrimeConfiguration = WPrimeConfiguration()) {
    private var config = configuration.copy(alerts = configuration.alerts.toList())
    private val calculator = WPrimeCalculator(config.criticalPower, config.anaerobicCapacity, config.tauRecovery, config.kIn, config.modelType)
    private var rideState = WPrimeRideState.IDLE
    private var available = true
    private var power = 0.0
    private var time = 0L
    private var lastSample: Long? = null
    private val cooldowns = mutableMapOf<String, Long>()
    private var dropAlerts = config.alerts.filter { it.alertType == AlertType.DROP }.sortedBy { it.thresholdPercentage }
    private var replenishAlerts = config.alerts.filter { it.alertType == AlertType.REPLENISH }.sortedByDescending { it.thresholdPercentage }

    @Synchronized fun snapshot(): WPrimeSnapshot = state()

    @Synchronized fun updateConfiguration(config: WPrimeConfiguration, ftp: Int? = null): WPrimeSnapshot {
        calculator.updateConfiguration(config.resolveCriticalPower(ftp), config.anaerobicCapacity, config.tauRecovery, config.kIn, config.modelType)
        this.config = config.copy(alerts = config.alerts.toList())
        dropAlerts = config.alerts.filter { it.alertType == AlertType.DROP }.sortedBy { it.thresholdPercentage }
        replenishAlerts = config.alerts.filter { it.alertType == AlertType.REPLENISH }.sortedByDescending { it.thresholdPercentage }
        cooldowns.keys.retainAll(config.alerts.map { it.id }.toSet())
        return state()
    }

    @Synchronized fun reset(timestampMs: Long? = null): WPrimeSnapshot {
        calculator.reset()
        lastSample = null
        available = true
        power = 0.0
        cooldowns.clear()
        if (timestampMs != null) time = timestampMs
        return state()
    }

    @Synchronized fun setRideState(state: WPrimeRideState, timestampMs: Long): WPrimeSnapshot {
        require(timestampMs >= time) { "Time must be monotonic" }
        if (state == rideState) return state()
        val fired = if (rideState == WPrimeRideState.PAUSED && state != WPrimeRideState.IDLE) {
            advance(0.0, timestampMs).alerts
        } else {
            emptyList()
        }
        // A transition starts a new integration interval; paused wall time is recovery, never effort.
        if (state == WPrimeRideState.IDLE) {
            reset(timestampMs)
        } else {
            calculator.rebaseTime(timestampMs)
        }
        rideState = state
        time = timestampMs
        power = 0.0
        lastSample = null
        return state(fired)
    }

    @Synchronized fun setSensorAvailable(available: Boolean, timestampMs: Long): WPrimeSnapshot {
        require(timestampMs >= time) { "Time must be monotonic" }
        if (rideState == WPrimeRideState.RECORDING && !this.available && available) calculator.rebaseTime(timestampMs)
        this.available = available
        time = timestampMs
        if (!available) {
            power = 0.0
            if (rideState == WPrimeRideState.RECORDING) calculator.rebaseTime(timestampMs)
        }
        return state()
    }

    @Synchronized fun updatePower(power: Double, timestampMs: Long): WPrimeSnapshot {
        require(power.isFinite() && power in 0.0..2000.0) { "Power must be finite and in 0..2000 W" }
        require(timestampMs >= time) { "Time must be monotonic" }
        time = timestampMs
        if (rideState == WPrimeRideState.RECORDING && (
                !available ||
                    (
                        calculator.getCurrentWPrime() == calculator.getAnaerobicCapacity() &&
                            lastSample?.let { timestampMs - it > 5000L } == true
                        )
                )
        ) {
            calculator.rebaseTime(timestampMs)
        }
        available = true
        lastSample = timestampMs
        if (rideState == WPrimeRideState.IDLE) return state()
        this.power = if (rideState == WPrimeRideState.PAUSED) 0.0 else power
        return advance(this.power, timestampMs)
    }

    @Synchronized fun tick(timestampMs: Long): WPrimeSnapshot {
        require(timestampMs >= time) { "Time must be monotonic" }
        time = timestampMs
        if (rideState == WPrimeRideState.IDLE) return state()
        if (rideState == WPrimeRideState.PAUSED || (available && lastSample?.let { timestampMs - it > 5000L } == true)) {
            power = 0.0
            return advance(0.0, timestampMs)
        }
        // Explicit sensor loss holds the balance and excludes the unknown interval on reconnection.
        if (rideState == WPrimeRideState.RECORDING && (
                !available ||
                    (
                        calculator.getCurrentWPrime() == calculator.getAnaerobicCapacity() &&
                            lastSample?.let { timestampMs - it > 5000L } == true
                        )
                )
        ) {
            calculator.rebaseTime(timestampMs)
        }
        return state()
    }

    private fun advance(power: Double, timestampMs: Long): WPrimeSnapshot {
        val previous = calculator.getWPrimePercentage()
        calculator.updatePower(power, timestampMs)
        val current = calculator.getWPrimePercentage()
        val candidates = if (current < previous) dropAlerts else replenishAlerts
        val fired = candidates.firstOrNull { alert ->
            val threshold = alert.thresholdPercentage
            val crossed = when (alert.alertType) {
                AlertType.DROP -> previous > threshold && current <= threshold
                AlertType.REPLENISH -> previous < threshold && current >= threshold
            }
            crossed && (cooldowns[alert.id]?.let { timestampMs - it >= 300000L } ?: true)
        }
        if (fired != null) cooldowns[fired.id] = timestampMs
        return state(if (fired == null) emptyList() else listOf(fired))
    }

    private fun state(alerts: List<WPrimeAlert> = emptyList()) = WPrimeSnapshot(
        calculator.getCurrentWPrime(), calculator.getWPrimePercentage(), power,
        calculator.getCriticalPower(), calculator.getAnaerobicCapacity(), config,
        rideState, available, time, alerts,
    )
}
