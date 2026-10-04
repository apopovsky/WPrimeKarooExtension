package com.itl.wprimeext.ui

import android.annotation.SuppressLint
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.absolutePadding
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.itl.wprimeext.shared.R
import io.hammerhead.karooext.models.ViewConfig
import kotlin.math.roundToInt
import androidx.glance.unit.ColorProvider as UnitColorProvider

/**
 * Glance composable for W' display - follows CustomDoubleTypeView pattern
 */
@SuppressLint("RestrictedApi")
@Composable
fun WPrimeGlanceView(
    value: String,
    fieldLabel: String,
    backgroundColor: UnitColorProvider,
    textColor: UnitColorProvider = UnitColorProvider(Color.White),
    currentPower: Int,
    criticalPower: Int,
    wPrimeJoules: Double,
    anaerobicCapacity: Double,
    textSize: Int = 56,
    alignment: ViewConfig.Alignment = ViewConfig.Alignment.RIGHT,
    maxPowerDeltaForFullRotation: Int = 150,
    fixedCharCount: Int? = null,
    sizeScale: Float = 1f,
    showArrow: Boolean = true,
    viewSize: Pair<Int, Int> = Pair(480, 240), // Size in pixels from ViewConfig
) {
    val (textAlign, horizontalAlignment) = when (alignment) {
        ViewConfig.Alignment.LEFT -> TextAlign.Start to Alignment.Start
        ViewConfig.Alignment.CENTER -> TextAlign.Center to Alignment.CenterHorizontally
        ViewConfig.Alignment.RIGHT -> TextAlign.End to Alignment.End
    }

    val safeCapacity = anaerobicCapacity.takeIf { it > 0 } ?: 1.0
    val wPrimeFraction = (wPrimeJoules / safeCapacity).toFloat().coerceIn(0f, 1f)

    val powerDelta = currentPower - criticalPower
    val wPrimeIsFull = wPrimeFraction >= 0.995f
    val isAtMaxWithLowPower = currentPower < criticalPower && wPrimeIsFull

    val rotationDegrees = if (isAtMaxWithLowPower) {
        0f
    } else {
        val rotationRatio = (powerDelta.toFloat() / maxPowerDeltaForFullRotation).coerceIn(-1f, 1f)
        ((if (powerDelta == 0) 0f else rotationRatio * 90f) / 15f).roundToInt() * 15f
    }

    // Convert pixel dimensions to dp (Karoo density = 2.0)
    val density = 2.0f
    val widgetWidthDp = (viewSize.first / density).dp
    val widgetHeightDp = (viewSize.second / density).dp

    val fieldArea = widgetWidthDp.value * widgetHeightDp.value
    val isWide = widgetWidthDp.value > 200
    val isTall = widgetHeightDp.value > 90

    // Field size classification: LARGE / MEDIUM_WIDE / MEDIUM / SMALL
    val fieldSize: String = when {
        fieldArea > 20000 || (isWide && isTall) -> "LARGE"

        isWide -> "MEDIUM_WIDE"

        // wide but short (e.g. 239×71 dp)
        fieldArea > 12000 -> "MEDIUM"

        else -> "SMALL"
    }

    val iconSizeDp = when (fieldSize) {
        "LARGE" -> 48.dp
        "MEDIUM_WIDE" -> 32.dp
        "MEDIUM" -> 36.dp
        else -> 28.dp // SMALL
    }
    // Ancho de columnas: solo el ícono sin padding adicional
    val arrowColWidthDp = iconSizeDp

    // Reservar espacio para el cálculo de texto - reducido para dar más espacio al texto
    val showValueArrow = showArrow && isWide
    val sizingReservedHorizontal = if (showValueArrow) {
        if (alignment == ViewConfig.Alignment.CENTER) iconSizeDp * 2 else iconSizeDp
    } else {
        0.dp
    }

    // Escalar maxSp según el tamaño del campo
    val scaledMaxSp = when (fieldSize) {
        "LARGE" -> (textSize * 3.0f).toInt()

        // 3.0x
        "MEDIUM_WIDE" -> (textSize * 2.2f).toInt()

        // 2.2x para campos anchos pero bajos (NUEVO)
        "MEDIUM" -> (textSize * 1.8f).toInt()

        // 1.8x
        else -> (textSize * 1.5f).toInt() // SMALL: 1.5x
    }

    val baseAutoSp = pickTextSizeSp(
        value = value,
        widgetWidth = widgetWidthDp,
        widgetHeight = widgetHeightDp,
        reservedHorizontal = sizingReservedHorizontal,
        maxSp = scaledMaxSp,
        minSp = 24,
        targetHeightFraction = when (fieldSize) {
            "LARGE" -> 0.95f
            "MEDIUM_WIDE" -> 0.95f
            "MEDIUM" -> 0.85f
            else -> 0.90f // SMALL
        },
        fixedCharCount = fixedCharCount,
    )
    val autoTextSp = (baseAutoSp * sizeScale).toInt().coerceAtLeast(8)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(12.dp)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.Top,
            modifier = GlanceModifier.fillMaxSize(),
        ) {
            // Match native Karoo header glyph heights and leave the value more room.
            val isWidePx = viewSize.first > 400
            val titleIconSize = if (isWidePx) 22.5.dp else 19.5.dp
            val titleRowHeight = 26.dp
            val titleTextSize = if (isWidePx) 18 else 17

            Box(
                modifier = GlanceModifier.fillMaxWidth().height(titleRowHeight),
                contentAlignment = Alignment.Center,
            ) {
                TitleRow(fieldLabel, textAlign, horizontalAlignment, textColor, titleRowHeight, titleIconSize, titleTextSize)
                // A narrow value needs the entire row for "100" and "12.0".
                // Keep the trend visible beside the header instead of shrinking digits.
                if (showArrow && !isWidePx) {
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = if (alignment == ViewConfig.Alignment.LEFT) Alignment.CenterEnd else Alignment.CenterStart,
                    ) {
                        ArrowColumn(rotationDegrees, 20.dp, 22.dp, textColor)
                    }
                }
            }

            // Value area occupies only the space left below the fixed title.
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // LEFT ARROW COLUMN (for RIGHT and CENTER alignment)
                if ((alignment == ViewConfig.Alignment.RIGHT || alignment == ViewConfig.Alignment.CENTER) && showValueArrow) {
                    ArrowColumn(rotationDegrees, iconSizeDp, arrowColWidthDp, textColor)
                }

                // Center the visible digits in the remaining value area.
                Box(
                    modifier = GlanceModifier
                        .fillMaxHeight()
                        .defaultWeight()
                        .padding(vertical = 2.dp),
                    contentAlignment = when (alignment) {
                        ViewConfig.Alignment.LEFT -> Alignment.CenterStart
                        ViewConfig.Alignment.RIGHT -> Alignment.CenterEnd
                        else -> Alignment.Center
                    },
                ) {
                    val context = LocalContext.current
                    val bodyHeightPx = viewSize.second - (titleRowHeight.value + 4f) * context.resources.displayMetrics.density
                    val referenceBounds = Rect().also { valuePaint.getTextBounds("0123456789", 0, 10, it) }
                    val requestedFontPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        autoTextSp.toFloat(),
                        context.resources.displayMetrics,
                    )
                    val requestedGlyphHeight = referenceBounds.height() * requestedFontPx / valuePaint.textSize
                    val fittedTextSp = if (requestedGlyphHeight > bodyHeightPx - 2f) {
                        autoTextSp * ((bodyHeightPx - 2f).coerceAtLeast(1f) / requestedGlyphHeight)
                    } else {
                        autoTextSp.toFloat()
                    }
                    val numberPaint = Paint(valuePaint).apply {
                        this.textSize = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_SP,
                            fittedTextSp,
                            context.resources.displayMetrics,
                        )
                    }
                    val glyphBounds = Rect().also { numberPaint.getTextBounds(value, 0, value.length, it) }
                    val metrics = numberPaint.fontMetrics
                    val lineHeightPx = kotlin.math.ceil(metrics.descent - metrics.ascent)
                    val naturalBaseline = -metrics.ascent
                    val centeredBaseline = (bodyHeightPx - glyphBounds.height()) / 2f - glyphBounds.top
                    AndroidRemoteViews(
                        remoteViews = RemoteViews(context.packageName, R.layout.wprime_value).apply {
                            setTextViewText(R.id.wprime_value, value)
                            setTextViewTextSize(R.id.wprime_value, TypedValue.COMPLEX_UNIT_SP, fittedTextSp)
                            setTextColor(R.id.wprime_value, textColor.getColor(context).toArgb())
                            if (android.os.Build.VERSION.SDK_INT >= 31) {
                                setViewLayoutHeight(R.id.wprime_value, lineHeightPx, TypedValue.COMPLEX_UNIT_PX)
                            }
                            // Android clamps vertical gravity when the line box is taller
                            // than the field. Center visible digits rather than that box.
                            setFloat(R.id.wprime_value, "setTranslationY", centeredBaseline - naturalBaseline)
                            setInt(
                                R.id.wprime_value,
                                "setGravity",
                                Gravity.CENTER_VERTICAL or when (alignment) {
                                    ViewConfig.Alignment.LEFT -> Gravity.START
                                    ViewConfig.Alignment.RIGHT -> Gravity.END
                                    else -> Gravity.CENTER_HORIZONTAL
                                },
                            )
                        },
                        modifier = GlanceModifier.fillMaxSize(),
                    )
                }

                // RIGHT ARROW COLUMN (for LEFT alignment only)
                if (alignment == ViewConfig.Alignment.LEFT && showValueArrow) {
                    ArrowColumn(rotationDegrees, iconSizeDp, arrowColWidthDp, textColor)
                }

                // RIGHT SPACER for CENTER alignment (balances left arrow to keep text centered)
                if (alignment == ViewConfig.Alignment.CENTER && showValueArrow) {
                    SpacerColumn(arrowColWidthDp)
                }
            }
        }
    }
}

/**
 * Renders an arrow column showing W' trend direction
 */
@SuppressLint("RestrictedApi")
@Composable
private fun ArrowColumn(
    rotationDegrees: Float,
    iconSizeDp: Dp,
    arrowColWidthDp: Dp,
    textColor: UnitColorProvider,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxHeight()
            .width(arrowColWidthDp),
        contentAlignment = Alignment.Center,
    ) {
        val arrowDrawableRes = when (rotationDegrees.roundToInt()) {
            -90 -> R.drawable.ic_direction_arrow_n90
            -75 -> R.drawable.ic_direction_arrow_n75
            -60 -> R.drawable.ic_direction_arrow_n60
            -45 -> R.drawable.ic_direction_arrow_n45
            -30 -> R.drawable.ic_direction_arrow_n30
            -15 -> R.drawable.ic_direction_arrow_n15
            0 -> R.drawable.ic_direction_arrow
            15 -> R.drawable.ic_direction_arrow_p15
            30 -> R.drawable.ic_direction_arrow_p30
            45 -> R.drawable.ic_direction_arrow_p45
            60 -> R.drawable.ic_direction_arrow_p60
            75 -> R.drawable.ic_direction_arrow_p75
            90 -> R.drawable.ic_direction_arrow_p90
            else -> R.drawable.ic_direction_arrow
        }

        Image(
            provider = ImageProvider(arrowDrawableRes),
            contentDescription = "W' Trend",
            modifier = GlanceModifier.size(iconSizeDp),
            colorFilter = ColorFilter.tint(textColor),
        )
    }
}

/**
 * Renders an empty spacer column for CENTER alignment balance
 */
@SuppressLint("RestrictedApi")
@Composable
private fun SpacerColumn(arrowColWidthDp: Dp) {
    Box(
        modifier = GlanceModifier
            .fillMaxHeight()
            .width(arrowColWidthDp),
    ) {
        // Empty spacer to balance arrow on opposite side
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun TitleRow(
    text: String,
    textAlign: TextAlign,
    horizontalAlignment: Alignment.Horizontal,
    textColor: UnitColorProvider,
    heightDp: Dp = 26.dp,
    iconSizeDp: Dp = 19.5.dp,
    textSizeSp: Int = 18,
) {
    Row(
        horizontalAlignment = horizontalAlignment,
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier
            .padding(0.dp)
            .height(heightDp),
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_wprime_battery),
            contentDescription = "W' Icon",
            modifier = GlanceModifier.size(iconSizeDp).absolutePadding(top = 3.dp),
        )
        Text(
            text = text,
            style = TextStyle(
                color = textColor,
                fontSize = textSizeSp.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
            ),
            modifier = GlanceModifier.padding(top = 6.dp),
        )
    }
}

@SuppressLint("RestrictedApi")
@Composable
fun WPrimeNotAvailableGlanceView(
    message: String = "N/A",
    isKaroo3: Boolean = true,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .let { if (isKaroo3) it.cornerRadius(12.dp) else it.cornerRadius(0.dp) }
            .padding(1.dp),
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.Gray)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = message,
                style = TextStyle(
                    color = UnitColorProvider(Color.White),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

// Helper functions for dynamic text sizing
private val valuePaint = Paint().apply {
    // Karoo maps weight 300 to IBM Plex Sans Regular; 400 resolves to Medium.
    typeface = Typeface.create("sans-serif-condensed-light", Typeface.NORMAL)
    textSize = 100f
}

private fun pickTextSizeSp(
    value: String,
    widgetWidth: Dp,
    widgetHeight: Dp,
    reservedHorizontal: Dp,
    maxSp: Int,
    minSp: Int = 24,
    targetHeightFraction: Float = 0.5f,
    fixedCharCount: Int? = null,
): Int {
    val safeMax = if (maxSp < minSp) minSp else maxSp
    if (widgetWidth == Dp.Unspecified ||
        widgetHeight == Dp.Unspecified ||
        widgetWidth.value <= 0f ||
        widgetHeight.value <= 0f
    ) {
        return safeMax
    }

    // Subtract reserved space (arrow) only once
    val availW = (widgetWidth - reservedHorizontal - 4.dp).coerceAtLeast(0.dp).value

    val isWide = widgetWidth.value > 200

    // Title row height and vertical padding mirror WPrimeGlanceView.
    // Keep in sync with the titleRowHeight block above or text sizing will be off.
    val titleRowHeight = 26.dp
    val verticalMargins = 4.dp
    val availH = (widgetHeight - titleRowHeight - verticalMargins).coerceAtLeast(0.dp).value
    if (availW <= 0f || availH <= 0f) {
        return safeMax
    }

    // Measure the actual font, including the narrower decimal point, against
    // both arrow columns for centered fields. The displayed string is authoritative.
    val widthText = if (isWide && value.length <= 4) "8888" else value
    val textWidthFactor = valuePaint.measureText(widthText) / valuePaint.textSize
    val fromWidth = if (textWidthFactor > 0) availW / textWidthFactor else safeMax.toFloat()
    // Increase height usage factor for better vertical space utilization
    val adjustedFraction = targetHeightFraction.coerceIn(0.5f, 0.95f) // Aumentado de 0.9 a 0.95
    // Measured against native 3S POWER (wide) and SPEED (half-width) on Karoo 3.
    val lineHeightFactor = if (isWide) 0.61f else 0.72f
    val fromHeight = (availH * adjustedFraction) / lineHeightFactor
    val raw = fromWidth.coerceAtMost(fromHeight)
    val clamped = raw.coerceIn(minSp.toFloat(), safeMax.toFloat())

    if (fixedCharCount != null) return clamped.toInt()

    // Expandir steps para incluir tamaños más grandes para campos grandes
    val steps = listOf(90, 84, 78, 72, 64, 56, 50, 46, 42, 38, 34, 32, 30, 28, 26, 24)
    val stepped = steps.firstOrNull { clamped >= it && it <= safeMax } ?: steps.last { it <= safeMax }
    return stepped
}

@Suppress("unused")
@SuppressLint("RestrictedApi")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 420, heightDp = 150)
@Composable
fun WPrimeGlanceViewPreview() {
    WPrimeGlanceView(
        value = "12.3",
        fieldLabel = "W' (kJ)",
        backgroundColor = UnitColorProvider(Color.DarkGray),
        currentPower = 250,
        criticalPower = 200,
        wPrimeJoules = 8000.0,
        anaerobicCapacity = 12000.0,
        textSize = 50,
        alignment = ViewConfig.Alignment.CENTER,
        maxPowerDeltaForFullRotation = 150,
        viewSize = Pair(840, 300), // 420dp * 2.0 density
    )
}

@Suppress("unused")
@SuppressLint("RestrictedApi")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun WPrimeGlanceViewPreview_Recovering() {
    WPrimeGlanceView(
        value = "18.5",
        fieldLabel = "W' (kJ)",
        backgroundColor = UnitColorProvider(Color.hsl(100f, 0.3f, 0.4f)),
        currentPower = 150,
        criticalPower = 200,
        wPrimeJoules = 10800.0,
        anaerobicCapacity = 12000.0,
        textSize = 50,
        alignment = ViewConfig.Alignment.CENTER,
        maxPowerDeltaForFullRotation = 150,
        viewSize = Pair(400, 300),
    )
}

@Suppress("unused")
@SuppressLint("RestrictedApi")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun WPrimeGlanceViewPreview_FullNoArrow() {
    WPrimeGlanceView(
        value = "11438",
        fieldLabel = "W' (kJ)",
        backgroundColor = UnitColorProvider(Color.hsl(120f, 0.5f, 0.5f)),
        currentPower = 100,
        criticalPower = 200,
        wPrimeJoules = 12000.0,
        anaerobicCapacity = 12000.0,
        textSize = 50,
        alignment = ViewConfig.Alignment.CENTER,
        maxPowerDeltaForFullRotation = 150,
        viewSize = Pair(400, 300),
    )
}

@Suppress("unused")
@SuppressLint("RestrictedApi")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun WPrimeGlanceViewPreview_MaxEffort() {
    WPrimeGlanceView(
        value = "3789",
        fieldLabel = "W' (kJ)",
        backgroundColor = UnitColorProvider(Color.Red),
        currentPower = 380,
        criticalPower = 200,
        wPrimeJoules = 2000.0,
        anaerobicCapacity = 12000.0,
        textSize = 50,
        alignment = ViewConfig.Alignment.LEFT,
        maxPowerDeltaForFullRotation = 150,
        viewSize = Pair(400, 300),
    )
}

@Suppress("unused")
@SuppressLint("RestrictedApi")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun WPrimeGlanceViewPreview_Neutral() {
    WPrimeGlanceView(
        value = "1580",
        fieldLabel = "W' (kJ)",
        backgroundColor = UnitColorProvider(Color.Gray),
        currentPower = 200,
        criticalPower = 200,
        wPrimeJoules = 9000.0,
        anaerobicCapacity = 12000.0,
        textSize = 50,
        alignment = ViewConfig.Alignment.RIGHT,
        maxPowerDeltaForFullRotation = 150,
        viewSize = Pair(400, 300),
    )
}

@Suppress("unused")
@SuppressLint("RestrictedApi")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun WPrimeGlanceViewPreview_NoArrow_NoColors() {
    WPrimeGlanceView(
        value = "1580",
        fieldLabel = "W' (kJ)",
        backgroundColor = UnitColorProvider(Color.White),
        textColor = UnitColorProvider(Color.Black),
        currentPower = 200,
        criticalPower = 200,
        wPrimeJoules = 9000.0,
        anaerobicCapacity = 12000.0,
        textSize = 50,
        alignment = ViewConfig.Alignment.CENTER,
        maxPowerDeltaForFullRotation = 150,
        showArrow = false,
        viewSize = Pair(400, 300),
    )
}
