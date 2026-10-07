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

    val iconSizeDp = if (!isWide) {
        20.dp
    } else {
        when (fieldSize) {
            "LARGE" -> 48.dp
            "MEDIUM_WIDE" -> 32.dp
            "MEDIUM" -> 36.dp
            else -> 20.dp // Preserve native-size digits beside the narrow-field arrow.
        }
    }
    // Ancho de columnas: solo el ícono sin padding adicional
    val arrowColWidthDp = iconSizeDp

    // Reservar espacio para el cálculo de texto - reducido para dar más espacio al texto
    val showValueArrow = showArrow
    val sizingReservedHorizontal = if (showValueArrow) {
        if (alignment == ViewConfig.Alignment.CENTER) iconSizeDp * 2 else iconSizeDp
    } else {
        0.dp
    }

    val context = LocalContext.current
    val metrics = context.resources.displayMetrics
    val titleRowHeight = if (isWide) 26.dp else 32.dp
    val valuePadding = if (isWide) 0.dp else 2.dp
    val bodyHeightPx = viewSize.second - (titleRowHeight.value + 2 * valuePadding.value) * metrics.density
    // The host dimensions are pixels. Layout dp uses the project's /2 convention,
    // but font fitting must use the actual host pixels and Android's sp scale.
    val autoTextSp = pickTextSizeSp(
        value = value,
        availableWidthPx = viewSize.first - (sizingReservedHorizontal.value + 4f) * metrics.density,
        availableHeightPx = bodyHeightPx - 2f,
        pixelsPerSp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1f, metrics),
        maxSp = textSize,
    )

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
            val titleTextSize = if (isWidePx) 19.2f else 17.6f

            Box(
                modifier = GlanceModifier.fillMaxWidth().height(titleRowHeight),
                contentAlignment = Alignment.Center,
            ) {
                TitleRow(fieldLabel, textAlign, horizontalAlignment, textColor, titleRowHeight, titleIconSize, titleTextSize, if (isWidePx) 0.dp else 6.dp)
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
                        .padding(horizontal = 2.dp, vertical = valuePadding),
                    contentAlignment = when (alignment) {
                        ViewConfig.Alignment.LEFT -> Alignment.CenterStart
                        ViewConfig.Alignment.RIGHT -> Alignment.CenterEnd
                        else -> Alignment.Center
                    },
                ) {
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
                    val fontMetrics = numberPaint.fontMetrics
                    val lineHeightPx = kotlin.math.ceil(fontMetrics.descent - fontMetrics.ascent)
                    val naturalBaseline = if (android.os.Build.VERSION.SDK_INT >= 31) {
                        -fontMetrics.ascent
                    } else {
                        (bodyHeightPx - lineHeightPx).coerceAtLeast(0f) / 2f - fontMetrics.ascent
                    }
                    // Native Karoo values sit one dp above/below geometric glyph
                    // centering in wide/narrow rows (measured beside POWER/SPEED).
                    val nativeBaselineOffset = metrics.density * if (isWide) -1f else 1f
                    val centeredBaseline = (bodyHeightPx - glyphBounds.height()) / 2f - glyphBounds.top + nativeBaselineOffset
                    AndroidRemoteViews(
                        remoteViews = RemoteViews(context.packageName, R.layout.wprime_value).apply {
                            setTextViewText(R.id.wprime_value, value)
                            setTextViewTextSize(R.id.wprime_value, TypedValue.COMPLEX_UNIT_SP, fittedTextSp)
                            setTextColor(R.id.wprime_value, textColor.getColor(context).toArgb())
                            if (android.os.Build.VERSION.SDK_INT >= 31) {
                                // The font line is taller than its digits. Give TextView the
                                // complete line height to avoid internal clipping before translation.
                                setViewLayoutHeight(R.id.wprime_value, lineHeightPx, TypedValue.COMPLEX_UNIT_PX)
                            }
                            // Center visible glyphs independently of the taller font line box.
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
    textSizeSp: Float = 17.6f,
    textTopPadding: Dp = 6.dp,
) {
    Row(
        horizontalAlignment = horizontalAlignment,
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier
            .padding(0.dp)
            .height(heightDp),
    ) {
        // Offset the icon container rather than padding its fixed image bounds:
        // Glance would otherwise shrink the battery drawable.
        Box(
            modifier = GlanceModifier
                .width(iconSizeDp)
                .height(heightDp)
                .padding(top = if (textTopPadding > 0.dp) textTopPadding + 2.dp else 0.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_wprime_battery),
                contentDescription = "W' Icon",
                modifier = GlanceModifier.size(iconSizeDp).absolutePadding(top = 3.dp),
            )
        }

        Text(
            text = text,
            style = TextStyle(
                color = textColor,
                fontSize = textSizeSp.sp,
                fontFamily = FontFamily("ibm-plex-sans-condensed"),
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
            ),
            modifier = GlanceModifier.padding(top = textTopPadding),
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
    // Karoo singleNumericDataStyle uses Relative12 Regular (the monospace alias).
    typeface = Typeface.create("relative", Typeface.NORMAL)
    textSize = 100f
}

private fun pickTextSizeSp(
    value: String,
    availableWidthPx: Float,
    availableHeightPx: Float,
    pixelsPerSp: Float,
    maxSp: Int,
): Int {
    // Match the native TextView's -0.04 em letter spacing in width measurement.
    val widthFactor = valuePaint.measureText(value) / valuePaint.textSize -
        0.04f * (value.length - 1).coerceAtLeast(0)
    val bounds = Rect().also { valuePaint.getTextBounds("0123456789", 0, 10, it) }
    val heightFactor = bounds.height() / valuePaint.textSize
    return minOf(
        maxSp.toFloat(),
        availableWidthPx.coerceAtLeast(1f) / (widthFactor.coerceAtLeast(0.1f) * pixelsPerSp),
        availableHeightPx.coerceAtLeast(1f) / (heightFactor * pixelsPerSp),
    ).toInt().coerceAtLeast(8)
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
