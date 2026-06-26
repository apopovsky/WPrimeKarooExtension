package com.itl.wprimeext.extension

fun WPrimeConfiguration.resolveCriticalPower(karooFtp: Int?): Double {
    val calculatedCriticalPower = karooFtp
        ?.takeIf { it > 0 }
        ?.let { it * KAROO_FTP_TO_CRITICAL_POWER_FACTOR }

    return if (criticalPowerSource == CriticalPowerSource.KAROO_FTP && calculatedCriticalPower != null) {
        calculatedCriticalPower
    } else {
        criticalPower
    }
}
