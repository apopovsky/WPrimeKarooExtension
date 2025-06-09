package com.itl.wprimeextension

import android.util.Log
import com.itl.wprimeextension.data.WPrimeSettings
import com.itl.wprimeextension.datatypes.WPrimeDataType
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.extension.KarooExtension

class WPrimeExtension : KarooExtension("wprime-extension", "1.0") {

    companion object {
        private const val TAG = "WPrimeExtension"
    }    private lateinit var wPrimeSettings: WPrimeSettings
    private lateinit var wPrimeDataType: WPrimeDataType

    override val types: List<DataTypeImpl>
        get() = listOf(wPrimeDataType)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "WPrimeExtension onCreate")

        // Initialize settings
        wPrimeSettings = WPrimeSettings(this)

        // Initialize data type with KarooSystemService and configuration flow
        wPrimeDataType = WPrimeDataType(
            karooSystem = KarooSystemService(this),
            configurationFlow = wPrimeSettings.configuration,
            extension = "wprime-extension"
        )

        Log.d(TAG, "WPrimeExtension initialized successfully")
    }

    override fun onDestroy() {
        Log.d(TAG, "WPrimeExtension onDestroy")
        super.onDestroy()
    }
}
