package com.itl.wprimeext.extension

import android.content.Context
import io.hammerhead.karooext.KarooSystemService

class WPrimeKjDataType(
    karooSystem: KarooSystemService,
    context: Context,
    extension: String,
    runtime: WPrimeRuntime? = null,
) : WPrimeDataTypeBase(karooSystem, context, extension, "wprime-kj", runtime) {
    override fun getDisplayText(snapshot: WPrimeSnapshot): String = "%.1f".format(snapshot.wPrimeJoules / 1000.0)
    override fun getStreamValue(snapshot: WPrimeSnapshot): Double = snapshot.wPrimeJoules / 1000.0
    override fun getFieldLabel(): String = "W' (kJ)"
    override fun getFixedCharCount(): Int = 4
}
