package com.itl.wprimeext.simulator

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.unit.DpSize
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.unit.ColorProvider
import com.itl.wprimeext.MainActivity
import com.itl.wprimeext.extension.WPrimeConfiguration
import com.itl.wprimeext.extension.WPrimeEngine
import com.itl.wprimeext.extension.WPrimeRideState
import com.itl.wprimeext.extension.WPrimeSettings
import com.itl.wprimeext.extension.WPrimeSnapshot
import com.itl.wprimeext.extension.presentation
import com.itl.wprimeext.ui.WPrimeGlanceView
import com.itl.wprimeext.utils.WPrimeLogger
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

/** Debug-only local host: real persisted settings, no Karoo service or real FIT writer. */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class WPrimeSimulatorActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var playback: Job? = null
    private var render: Job? = null
    private var configurationJob: Job? = null
    private var config = WPrimeConfiguration()
    private val engine by lazy { WPrimeEngine(config) }
    private lateinit var root: LinearLayout
    private lateinit var fields: FrameLayout
    private lateinit var status: TextView
    private lateinit var observations: TextView
    private lateinit var manual: EditText
    private var samples = PowerScenario.builtIn("Effort + recovery")
    private var index = 0
    private var time = 0L
    private var scenarioOffset = 0L
    private var speed = 1
    private var width = 480
    private var height = 240
    private var alignment = ViewConfig.Alignment.RIGHT
    private var manualMode = true
    private var recoveryOnly = false
    private var selectedField = "Percent"
    private var startupInitialized = false
    private var renders = 0
    private val events = ArrayDeque<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.HARDWARE !in listOf("ranchu", "goldfish") || (!android.os.Build.PRODUCT.startsWith("sdk") && !android.os.Build.MODEL.contains("sdk", ignoreCase = true) && !android.os.Build.MODEL.contains("Emulator"))) {
            Toast.makeText(this, "WPrime Simulator runs only on Android emulators", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        width = if (intent.getStringExtra("layout") == "Half-row") 240 else 480
        height = if (intent.getStringExtra("layout") == "Full-screen") 800 else 240
        selectedField = if (intent.getStringExtra("field") == "kJ") "kJ" else "Percent"
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
        }
        setContentView(ScrollView(this).apply { addView(root) })
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(12 + bars.left, 12 + bars.top, 12 + bars.right, 12 + bars.bottom)
            windowInsets
        }
        label("WPrime Lab")
        status = label("")
        val layoutNames = listOf("Full-width row", "Half-row", "Full-screen")
        val fieldNames = listOf("Percent", "kJ")
        row {
            choice(layoutNames, intent.getStringExtra("layout"), this) {
                this@WPrimeSimulatorActivity.width = if (it == "Half-row") 240 else 480
                this@WPrimeSimulatorActivity.height = if (it == "Full-screen") 800 else 240
                renderFields()
            }
            choice(fieldNames, selectedField, this) {
                selectedField = it
                renderFields()
            }
        }
        fields = FrameLayout(this).apply { setBackgroundColor(android.graphics.Color.rgb(38, 38, 38)) }
        root.addView(fields, LinearLayout.LayoutParams(dp(240), dp(400)))
        row {
            rowButton("Start") { start() }
            rowButton("Pause ride") { pauseRide() }
            rowButton("Step") {
                playback?.cancel()
                step()
                renderFields()
            }
            rowButton("Reset") { reset() }
        }
        row {
            manual = EditText(this@WPrimeSimulatorActivity).apply {
                hint = "Power W"
                setText("400")
                inputType = 2
            }
            addView(manual, LinearLayout.LayoutParams(0, -2, 1f))
            rowButton("Apply power") {
                val power = manual.text.toString().toDoubleOrNull()
                if (power != null && power.isFinite() && power in 0.0..2000.0) {
                    manualMode = true
                    observe(engine.updatePower(power, time))
                    renderFields()
                } else {
                    Toast.makeText(this@WPrimeSimulatorActivity, "Power must be 0–2000 W", Toast.LENGTH_SHORT).show()
                }
            }
        }
        row {
            rowButton("−10") { adjustPower(-10.0) }
            rowButton("−1") { adjustPower(-1.0) }
            rowButton("+1") { adjustPower(1.0) }
            rowButton("+10") { adjustPower(10.0) }
            rowButton("0") { setManualPower(0.0) }
            rowButton("CP") { setManualPower(engine.snapshot().criticalPower) }
        }
        row {
            rowButton("Settings · CP / model") { startActivity(Intent(this@WPrimeSimulatorActivity, MainActivity::class.java)) }
            rowButton("Freeze clock") { playback?.cancel() }
        }
        val scenarioNames = listOf("Manual", "Effort + recovery", "Intervals", "Exhaustion", "Sensor loss")
        val speedNames = listOf("1×", "5×", "20×", "60×")
        row {
            choice(scenarioNames, intent.getStringExtra("scenario"), this) { name ->
                manualMode = name == "Manual"
                samples = PowerScenario.builtIn(name)
                reset()
            }
            choice(speedNames, "${intent.getIntExtra("speed", 1)}×", this) { speed = it.removeSuffix("×").toInt() }
        }
        choice(listOf("RECORDING", "PAUSED", "IDLE")) {
            recoveryOnly = it == "PAUSED"
            observe(engine.setRideState(WPrimeRideState.valueOf(it), time))
            renderFields()
        }
        row {
            rowButton("Sensor lost") {
                observe(engine.setSensorAvailable(false, time))
                renderFields()
            }
            rowButton("Sensor found") {
                observe(engine.setSensorAvailable(true, time))
                renderFields()
            }
        }
        button("Import CSV") {
            startActivityForResult(
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                },
                10,
            )
        }
        button("Load sample CSV") {
            samples = PowerScenario.parse(assets.open("effort-recovery.csv").bufferedReader().use { it.readText() })
            manualMode = false
            reset()
        }
        label("CSV: time_s,power_w,event · starts at 0, increasing seconds · blank power = silence · events RECORDING/PAUSED/IDLE/LOST/FOUND")
        observations = label("No alerts yet. FIT below is a value preview; no file is written.")
        reset()
    }

    private fun reset() {
        playback?.cancel()
        index = 0
        time = 0
        scenarioOffset = 0
        events.clear()
        recoveryOnly = false
        engine.reset(0)
        engine.setSensorAvailable(true, 0)
        engine.setRideState(WPrimeRideState.RECORDING, 0)
        val first = samples.first()
        PowerScenario.rideState(first.event)?.let { observe(engine.setRideState(it, 0)) }
        if (first.event == "LOST") observe(engine.setSensorAvailable(false, 0))
        val initialPower = if (manualMode && ::manual.isInitialized) manual.text.toString().toDoubleOrNull()?.takeIf { it.isFinite() }?.coerceIn(0.0, 2000.0) ?: 0.0 else first.power
        observe(initialPower?.let { engine.updatePower(it, 0) } ?: engine.tick(0))
        renderFields()
    }

    private fun start() {
        recoveryOnly = false
        observe(engine.setRideState(WPrimeRideState.RECORDING, time))
        runPlayback()
    }

    private fun pauseRide() {
        recoveryOnly = true
        observe(engine.setRideState(WPrimeRideState.PAUSED, time))
        renderFields()
        runPlayback()
    }

    private fun runPlayback() {
        playback?.cancel()
        playback = scope.launch {
            while (isActive && (recoveryOnly || manualMode || index < samples.lastIndex)) {
                delay(1000)
                repeat(speed) { step() }
                renderFields()
            }
        }
    }

    private fun step() {
        if (recoveryOnly) {
            time += 1000
            if (!manualMode) scenarioOffset += 1000
            observe(engine.tick(time))
            return
        }
        if (manualMode) {
            time += 1000
            val state = engine.snapshot()
            observe(if (state.sensorAvailable) engine.updatePower(manual.text.toString().toDoubleOrNull()?.takeIf { it.isFinite() }?.coerceIn(0.0, 2000.0) ?: 0.0, time) else engine.tick(time))
            return
        }
        if (index >= samples.lastIndex) return
        val sample = samples[++index]
        val sampleTime = sample.timeMs + scenarioOffset
        // Reproduce silent recovery ticks through long gaps rather than integrating them as one sample.
        while (time + 3000 < sampleTime) {
            time += 3000
            observe(engine.tick(time))
        }
        time = sampleTime
        PowerScenario.rideState(sample.event)?.let { observe(engine.setRideState(it, time)) }
        if (sample.event == "LOST" || sample.event == "FOUND") observe(engine.setSensorAvailable(sample.event == "FOUND", time))
        observe(sample.power?.let { engine.updatePower(it, time) } ?: engine.tick(time))
    }

    private fun observe(snapshot: WPrimeSnapshot) {
        snapshot.alerts.forEach {
            if (events.size == 8) events.removeFirst()
            events.addLast("${time / 1000}s ${it.alertType} ${it.thresholdPercentage}% (${it.id})")
        }
    }

    private fun renderFields() {
        if (!::fields.isInitialized) return
        render?.cancel()
        render = scope.launch {
            val s = engine.snapshot()
            status.text = String.format(Locale.US, "t=%ds · %.0f W · %.1f%% · %.1f J\n%s · sensor=%s · render=%d\nCP %.0f W / W′ %.0f J · %s", time / 1000, s.currentPower, s.percentage, s.wPrimeJoules, s.rideState, s.sensorAvailable, ++renders, s.criticalPower, s.anaerobicCapacity, s.configuration.modelType)
            fields.removeAllViews()
            val presentation = s.presentation()
            if (height < 800) {
                for (rowIndex in 1..2) {
                    val placeholder = TextView(this@WPrimeSimulatorActivity).apply {
                        text = if (rowIndex == 1) "3S POWER\n${s.currentPower.toInt()} W" else "SIMULATED RIDE\n${time / 1000}s"
                        gravity = android.view.Gravity.CENTER
                        setTextColor(android.graphics.Color.WHITE)
                        textSize = 20f
                    }
                    fields.addView(placeholder, FrameLayout.LayoutParams(dp(240), dp(120)).apply { topMargin = dp(rowIndex * 130) })
                }
                if (width <= 400) {
                    fields.addView(
                        TextView(this@WPrimeSimulatorActivity).apply {
                            text = "OTHER FIELD"
                            gravity = android.view.Gravity.CENTER
                            setTextColor(android.graphics.Color.LTGRAY)
                        },
                        FrameLayout.LayoutParams(dp(120), dp(120)).apply { leftMargin = dp(120) },
                    )
                }
            }
            for (kj in listOf(selectedField == "kJ")) {
                val frame = FrameLayout(this@WPrimeSimulatorActivity)
                // Renderer uses the same project /2 pixel sizing convention, inflated at emulator density.
                val pixelWidth = (width / 2f * resources.displayMetrics.density).toInt()
                val pixelHeight = (height / 2f * resources.displayMetrics.density).toInt()
                fields.addView(frame, FrameLayout.LayoutParams(pixelWidth, pixelHeight))
                try {
                    val displayText = if (kj) String.format(Locale.getDefault(), "%.1f", s.wPrimeJoules / 1000) else s.percentage.toInt().toString()
                    val remote = GlanceRemoteViews().compose(this@WPrimeSimulatorActivity, DpSize.Unspecified) {
                        WPrimeGlanceView(
                            value = displayText,
                            fieldLabel = if (kj) "W' (kJ)" else "%W'",
                            backgroundColor = ColorProvider(presentation.backgroundColor),
                            textColor = ColorProvider(presentation.textColor),
                            currentPower = s.currentPower.toInt(), criticalPower = s.criticalPower.toInt(),
                            wPrimeJoules = s.wPrimeJoules, anaerobicCapacity = s.anaerobicCapacity,
                            textSize = 56, alignment = alignment, fixedCharCount = if (kj) 4 else 3,
                            showArrow = presentation.showArrow, viewSize = Pair(width, height),
                        )
                    }.remoteViews
                    frame.addView(remote.apply(this@WPrimeSimulatorActivity, frame))
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    WPrimeLogger.e(WPrimeLogger.Module.UI, "Simulator rendering failed: $e")
                    frame.addView(TextView(this@WPrimeSimulatorActivity).apply { text = "Render failed: ${e.message}" })
                }
            }
            val fitKind = when (s.rideState) {
                WPrimeRideState.IDLE -> "no message"
                WPrimeRideState.RECORDING -> "record"
                WPrimeRideState.PAUSED -> "session"
            }
            observations.text = "FIT value preview ($fitKind; no file written): WPrimeJ=${s.wPrimeJoules.roundToInt()} J, WPrimePct=${s.percentage.roundToInt()}%\nAlerts:\n${events.joinToString("\n")}"
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun adjustPower(delta: Double) = setManualPower((manual.text.toString().toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0) + delta)

    private fun setManualPower(power: Double) {
        val safePower = power.takeIf { it.isFinite() }?.coerceIn(0.0, 2000.0) ?: 0.0
        manual.setText(safePower.toInt().toString())
        manualMode = true
        observe(engine.updatePower(safePower, time))
        renderFields()
    }

    @Deprecated("Activity result callback retained for native debug host")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 10 || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        scope.launch {
            try {
                val imported = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                        val text = StringBuilder()
                        val buffer = CharArray(8192)
                        while (true) {
                            val count = reader.read(buffer)
                            if (count < 0) break
                            require(text.length + count <= 5_000_000) { "CSV exceeds 5 MB" }
                            text.append(buffer, 0, count)
                        }
                        PowerScenario.parse(text.toString())
                    } ?: error("Cannot open CSV")
                }
                samples = imported
                manualMode = false
                reset()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Toast.makeText(this@WPrimeSimulatorActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun label(text: String): TextView = TextView(this).apply {
        this.text = text
        setPadding(0, 6, 0, 6)
        root.addView(this)
    }
    private fun button(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        setOnClickListener { action() }
        root.addView(this)
    }
    private fun row(content: LinearLayout.() -> Unit) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(row)
        row.content()
    }
    private fun LinearLayout.rowButton(text: String, action: () -> Unit) {
        addView(
            Button(this@WPrimeSimulatorActivity).apply {
                this.text = text
                setOnClickListener { action() }
            },
            LinearLayout.LayoutParams(0, -2, 1f),
        )
    }
    private fun choice(values: List<String>, initial: String? = null, parent: LinearLayout = root, action: (String) -> Unit): Spinner {
        val spinner = Spinner(this).apply { adapter = ArrayAdapter(this@WPrimeSimulatorActivity, android.R.layout.simple_spinner_dropdown_item, values) }
        var previousPosition = values.indexOf(initial).coerceAtLeast(0)
        spinner.setSelection(previousPosition)
        if (parent == root) parent.addView(spinner) else parent.addView(spinner, LinearLayout.LayoutParams(0, -2, 1f))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != previousPosition) {
                    previousPosition = position
                    action(values[position])
                }
            }
        }
        return spinner
    }

    override fun onStart() {
        super.onStart()
        configurationJob = scope.launch {
            WPrimeSettings(this@WPrimeSimulatorActivity).configuration.collectLatest {
                config = it
                observe(engine.updateConfiguration(it))
                renderFields()
                if (!startupInitialized) {
                    startupInitialized = true
                    // Initial persisted parameters precede replay; controls' initial callbacks settle first.
                    root.post {
                        intent.getStringExtra("scenario")?.let { name ->
                            manualMode = name == "Manual"
                            samples = PowerScenario.builtIn(name)
                        }
                        speed = intent.getIntExtra("speed", speed).coerceIn(1, 60)
                        reset()
                        repeat(intent.getIntExtra("steps", 0).coerceIn(0, 10000)) { step() }
                        renderFields()
                        if (intent.getBooleanExtra("autoplay", false) || intent.getBooleanExtra("autoStart", false)) start()
                    }
                }
            }
        }
    }
    override fun onStop() {
        playback?.cancel()
        render?.cancel()
        configurationJob?.cancel()
        super.onStop()
    }
    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
