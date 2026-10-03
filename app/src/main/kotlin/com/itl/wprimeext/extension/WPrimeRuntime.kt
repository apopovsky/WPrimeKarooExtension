package com.itl.wprimeext.extension

import android.content.Context
import android.os.SystemClock
import com.itl.wprimeext.utils.WPrimeLogger
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.RideState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** One input subscription and one serialized calculation for numeric fields, views, alerts and FIT. */
class WPrimeRuntime(context: Context, private val karooSystem: KarooSystemService) {
    private val settings = WPrimeSettings(context.applicationContext)
    private val engine = WPrimeEngine()
    private val alerts = WPrimeAlertManager(karooSystem)
    private val mutableState = MutableStateFlow(engine.snapshot())
    val state = mutableState.asStateFlow()
    private var scope: CoroutineScope? = null

    @Synchronized fun start() {
        if (scope != null) return
        val owner = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = owner
        owner.launch {
            settings.configuration.combine(karooSystem.userProfileFlow()) { config, profile -> config to profile?.ftp }
                .collect { (config, ftp) -> command { engine.updateConfiguration(config, ftp) } }
        }
        owner.launch {
            karooSystem.consumerFlow<RideState>().collect { state ->
                command {
                    engine.setRideState(
                        when (state) {
                            is RideState.Idle -> WPrimeRideState.IDLE
                            is RideState.Paused -> WPrimeRideState.PAUSED
                            is RideState.Recording -> WPrimeRideState.RECORDING
                        },
                        SystemClock.elapsedRealtime(),
                    )
                }
            }
        }
        owner.launch {
            state.map { it.rideState != WPrimeRideState.IDLE }.distinctUntilChanged().collectLatest { riding ->
                if (riding) {
                    val input = WPrimePowerInput(engine)
                    karooSystem.timedStreamDataFlow(DataType.Type.POWER).collect { sample ->
                        command {
                            val result = input.accept(sample)
                            if (result.lostSamples > 0) {
                                WPrimeLogger.w(WPrimeLogger.Module.EXTENSION, "Power queue lost ${result.lostSamples} samples; excluding unknown interval")
                            }
                            result.snapshot ?: engine.snapshot()
                        }
                    }
                }
            }
        }
        owner.launch {
            state.map {
                it.rideState != WPrimeRideState.IDLE && it.wPrimeJoules < it.anaerobicCapacity &&
                    (it.sensorAvailable || it.rideState == WPrimeRideState.PAUSED)
            }
                .distinctUntilChanged().collectLatest { recovering ->
                    if (recovering) {
                        while (true) {
                            delay(3000L)
                            command { engine.tick(SystemClock.elapsedRealtime()) }
                        }
                    }
                }
        }
    }

    // Lock includes publication and dispatch: state cannot be published out of command order.
    @Synchronized private fun command(update: () -> WPrimeSnapshot) {
        if (scope == null) return
        try {
            val snapshot = update()
            mutableState.value = snapshot.copy(alerts = emptyList())
            snapshot.alerts.forEach { alerts.dispatchAlert(it, snapshot.percentage) }
        } catch (e: IllegalArgumentException) {
            WPrimeLogger.w(WPrimeLogger.Module.EXTENSION, e, "Rejected invalid runtime input")
        }
    }

    @Synchronized fun stop() {
        scope?.cancel()
        scope = null
        engine.reset()
        mutableState.value = engine.setRideState(WPrimeRideState.IDLE, SystemClock.elapsedRealtime())
    }
}
