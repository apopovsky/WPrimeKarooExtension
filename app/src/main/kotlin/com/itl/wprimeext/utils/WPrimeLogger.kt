package com.itl.wprimeext.utils

import timber.log.Timber

object WPrimeLogger {
    private const val APP_TAG = "WPrime"

    object Module {
        const val EXTENSION = "Extension"
        const val DATA_TYPE = "DataType"
        const val CALCULATOR = "Calculator"
        const val SETTINGS = "Settings"
        const val UI = "UI"
        const val VIEWMODEL = "ViewModel"
    }

    fun d(module: String, message: String) {
        Timber.tag("$APP_TAG:$module").d(message)
    }

    fun i(module: String, message: String) {
        Timber.tag("$APP_TAG:$module").i(message)
    }

    fun w(module: String, message: String) {
        Timber.tag("$APP_TAG:$module").w(message)
    }

    fun w(module: String, throwable: Throwable, message: String) {
        Timber.tag("$APP_TAG:$module").w(throwable, message)
    }

    fun e(module: String, message: String) {
        Timber.tag("$APP_TAG:$module").e(message)
    }

    fun e(module: String, throwable: Throwable, message: String) {
        Timber.tag("$APP_TAG:$module").e(throwable, message)
    }

    // Specific logging methods for common debugging scenarios
    fun logConfiguration(module: String, cp: Double, wprime: Double, tau: Double) {
        d(module, "Configuration - CP: ${cp}W, W': ${wprime}J, Tau: ${tau}s")
    }

    fun logPowerUpdate(module: String, power: Double, currentWPrime: Double, percentRemaining: Double) {
        d(module, "Power: ${power}W -> W': ${currentWPrime.toInt()}J (${percentRemaining.toInt()}%)")
    }

    fun logStateChange(module: String, from: String, to: String) {
        i(module, "State change: $from -> $to")
    }

    fun logDataFlow(module: String, operation: String, value: Any? = null) {
        val valueStr = value?.let { " - Value: $it" } ?: ""
        d(module, "Data flow: $operation$valueStr")
    }

    fun logError(module: String, operation: String, error: Throwable) {
        e(module, error, "Error during $operation: ${error.message}")
    }
}
