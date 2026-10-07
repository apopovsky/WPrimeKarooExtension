package com.itl.wprimeext.extension

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.unit.ColorProvider
import com.itl.wprimeext.ui.WPrimeGlanceView
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
abstract class WPrimeDataTypeBase(
    extension: String,
    typeId: String,
    private val runtime: WPrimeRuntime,
) : DataTypeImpl(extension, typeId) {
    private val glance = GlanceRemoteViews()

    abstract fun getDisplayText(snapshot: WPrimeSnapshot): String

    abstract fun getFieldLabel(): String

    abstract fun getStreamValue(snapshot: WPrimeSnapshot): Double

    override fun startStream(emitter: Emitter<StreamState>) {
        val shared = runtime
        val job = CoroutineScope(Dispatchers.IO).launch {
            shared.state.map { snapshot ->
                // Map using this snapshot's capacity, not a separately changing configuration.
                getStreamValue(snapshot)
            }.distinctUntilChanged().collect { value ->
                emitter.onNext(StreamState.Streaming(DataPoint(dataTypeId, values = mapOf(DataType.Field.SINGLE to value))))
            }
        }
        emitter.setCancellable { job.cancel() }
    }

    @SuppressLint("RestrictedApi")
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            emitter.onNext(UpdateGraphicConfig(showHeader = false))
            val source = if (config.preview) previewDataFlow() else runtime.state
            var lastRenderMs: Long? = null
            source.map { snapshot ->

                RenderState(snapshot, getDisplayText(snapshot))
            }.distinctUntilChanged { old, new -> old.key() == new.key() }.conflate().collect { render ->
                val remaining = lastRenderMs?.let { it + 1000L - SystemClock.elapsedRealtime() } ?: 0L
                if (remaining > 0L) delay(remaining)
                val data = render.snapshot
                val presentation = data.presentation()
                withContext(Dispatchers.Main) {
                    val view = glance.compose(context, DpSize.Unspecified) {
                        WPrimeGlanceView(
                            value = render.text,
                            fieldLabel = getFieldLabel(),
                            backgroundColor = ColorProvider(presentation.backgroundColor),
                            textColor = ColorProvider(presentation.textColor),
                            currentPower = data.currentPower.toInt(), criticalPower = data.criticalPower.toInt(),
                            wPrimeJoules = data.wPrimeJoules, anaerobicCapacity = data.anaerobicCapacity,
                            textSize = config.textSize, alignment = config.alignment,
                            showArrow = presentation.showArrow, viewSize = config.viewSize,
                        )
                    }.remoteViews
                    emitter.updateView(view)
                    lastRenderMs = SystemClock.elapsedRealtime()
                }
            }
        }
        emitter.setCancellable { job.cancel() }
    }

    private data class RenderState(val snapshot: WPrimeSnapshot, val text: String) {
        fun key(): List<Any> {
            val presentation = snapshot.presentation()
            val powerDelta = snapshot.currentPower.toInt() - snapshot.criticalPower.toInt()
            val arrow = if (!presentation.showArrow || (snapshot.percentage >= 99.5 && powerDelta < 0)) {
                0
            } else {
                ((powerDelta / 150f).coerceIn(-1f, 1f) * 90f / 15f).roundToInt()
            }
            return listOf(text, presentation, arrow)
        }
    }
    private fun previewDataFlow(): Flow<WPrimeSnapshot> = flow {
        val engine = WPrimeEngine()
        engine.setRideState(WPrimeRideState.RECORDING, 0L)
        var time = 0L
        while (true) {
            emit(engine.updatePower(if (time % 60000L < 30000L) 400.0 else 100.0, time))
            time += 1000L
            delay(1000L)
        }
    }
}
