package com.itl.wprimeext.extension

class WPrimeKjDataType(
    extension: String,
    runtime: WPrimeRuntime,
) : WPrimeDataTypeBase(extension, "wprime-kj", runtime) {
    override fun getDisplayText(snapshot: WPrimeSnapshot): String = "%.1f".format(snapshot.wPrimeJoules / 1000.0)
    override fun getStreamValue(snapshot: WPrimeSnapshot): Double = snapshot.wPrimeJoules / 1000.0
    override fun getFieldLabel(): String = "W' (kJ)"
    override fun getFixedCharCount(): Int = 4
}
