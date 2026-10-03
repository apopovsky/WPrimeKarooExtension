package com.itl.wprimeext.extension

class WPrimeDataType(
    extension: String,
    runtime: WPrimeRuntime,
) : WPrimeDataTypeBase(extension, "wprime", runtime) {
    override fun getDisplayText(snapshot: WPrimeSnapshot): String = snapshot.percentage.toInt().toString()
    override fun getStreamValue(snapshot: WPrimeSnapshot): Double = snapshot.percentage
    override fun getFieldLabel(): String = "%W'"
    override fun getFixedCharCount(): Int = 3
}
