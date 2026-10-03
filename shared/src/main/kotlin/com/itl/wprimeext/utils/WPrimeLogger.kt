package com.itl.wprimeext.utils

import com.itl.wprimeext.shared.BuildConfig
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
        if (BuildConfig.DEBUG) Timber.tag("$APP_TAG:$module").d(message)
    }

    fun d(module: String, message: () -> String) {
        if (BuildConfig.DEBUG) d(module, message())
    }

    fun i(module: String, message: String) {
        if (BuildConfig.DEBUG) Timber.tag("$APP_TAG:$module").i(message)
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

    fun logPowerUpdate(module: String, power: Double, currentWPrime: Double, percentRemaining: Double) {
        d(module) { "Power: ${power}W -> W': ${currentWPrime.toInt()}J (${percentRemaining.toInt()}%)" }
    }
}
