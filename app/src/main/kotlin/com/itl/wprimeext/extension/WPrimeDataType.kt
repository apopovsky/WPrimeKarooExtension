package com.itl.wprimeext.extension

import android.content.Context
import io.hammerhead.karooext.KarooSystemService

class WPrimeDataType(
    karooSystem: KarooSystemService,
    context: Context,
    extension: String,
    runtime: WPrimeRuntime? = null,
) : WPrimeDataTypeBase(karooSystem, context, extension, "wprime", runtime) {
    override fun getDisplayText(snapshot: WPrimeSnapshot): String = snapshot.percentage.toInt().toString()
    override fun getStreamValue(snapshot: WPrimeSnapshot): Double = snapshot.percentage
    override fun getFieldLabel(): String = "%W'"
    override fun getFixedCharCount(): Int = 3
}
