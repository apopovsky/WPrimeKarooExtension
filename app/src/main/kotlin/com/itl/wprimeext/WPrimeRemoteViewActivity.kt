package com.itl.wprimeext

import android.app.Activity
import android.os.Bundle
import android.os.IBinder
import android.widget.FrameLayout
import android.widget.RemoteViews
import com.itl.wprimeext.extension.WPrimeDataType
import com.itl.wprimeext.extension.WPrimeKjDataType
import com.itl.wprimeext.utils.WPrimeLogger
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.aidl.IHandler
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.ViewConfig

class WPrimeRemoteViewActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val frameLayout = FrameLayout(this)
        setContentView(frameLayout)

        val karooSystem = KarooSystemService(this)
        val typeId = intent.getStringExtra("typeId") ?: "wprime-kj"
        val dataType = when (typeId) {
            "wprime" -> WPrimeDataType(karooSystem, this, "demo")
            else -> WPrimeKjDataType(karooSystem, this, "demo")
        }

        // Create a mock IHandler that renders RemoteViews directly in our UI
        val mockHandler = object : IHandler {
            override fun onNext(bundle: Bundle) {
                val remoteView = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    bundle.getParcelable("view", RemoteViews::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    bundle.getParcelable<RemoteViews>("view")
                }
                remoteView?.let { view ->
                    runOnUiThread {
                        frameLayout.removeAllViews()
                        val inflatedView = view.apply(this@WPrimeRemoteViewActivity, frameLayout)
                        frameLayout.addView(inflatedView)
                    }
                }
            }

            override fun onError(msg: String?) {
                // Handle errors in the emitter (optional logging)
                WPrimeLogger.e(WPrimeLogger.Module.DATA_TYPE, "ViewEmitter error: $msg")
            }

            override fun onComplete() {
                // Handle completion of the emitter stream
                WPrimeLogger.d(WPrimeLogger.Module.DATA_TYPE, "ViewEmitter completed")
            }

            override fun asBinder(): IBinder? = null
        }

        val config = ViewConfig(
            gridSize = Pair(60, 15),
            viewSize = Pair(800, 200),
            textSize = 48,
            preview = true,
        )

        // Create a real ViewEmitter that will call our mock handler
        val emitter = ViewEmitter(packageName, mockHandler)

        // Start the view - this will begin the W' calculation and UI updates
        dataType.startView(this, config, emitter)
    }
}
