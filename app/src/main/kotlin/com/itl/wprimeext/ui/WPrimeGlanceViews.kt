package com.itl.wprimeext.ui

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.absolutePadding
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
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
import com.itl.wprimeext.R
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
    targetHeightFraction: Float = 0.5f,
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
        isWide -> "MEDIUM_WIDE" // wide but short (e.g. 239×71 dp)
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
    val sizingReservedHorizontal = if (showArrow) (iconSizeDp + 2.dp) else 0.dp

    // Escalar maxSp según el tamaño del campo
    val scaledMaxSp = when (fieldSize) {
        "LARGE" -> (textSize * 3.0f).toInt() // 3.0x
        "MEDIUM_WIDE" -> (textSize * 2.2f).toInt() // 2.2x para campos anchos pero bajos (NUEVO)
        "MEDIUM" -> (textSize * 1.8f).toInt() // 1.8x
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
    // Reduce text 10% for single-column (SMALL) fields to avoid oversized 2-char values
    val columnScaleFactor = if (fieldSize == "SMALL") 0.90f else 1.0f
    val autoTextSp = (baseAutoSp * sizeScale * columnScaleFactor).toInt().coerceAtLeast(8)

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
            // Title sizing: near-constant so it looks the same regardless of field size.
            // Wide (full-width, >400 px wide) is the base; narrow (single-column) is 0.90×.
            val isWidePx = viewSize.first > 400
            val titleIconSize   = if (isWidePx) 30.dp else 26.dp
            val titleRowHeight  = if (isWidePx) 32.dp else 28.dp
            val titleTextSize   = if (isWidePx) 20 else 18

            TitleRow(fieldLabel, textAlign, horizontalAlignment, textColor, titleRowHeight, titleIconSize, titleTextSize)

            // Value area - Row with arrow column(s) and text column
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // LEFT ARROW COLUMN (for RIGHT and CENTER alignment)
                if ((alignment == ViewConfig.Alignment.RIGHT || alignment == ViewConfig.Alignment.CENTER) && showArrow) {
                    ArrowColumn(rotationDegrees, iconSizeDp, arrowColWidthDp, textColor)
                }

                // TEXT COLUMN (bottom-aligned, texto pegado al fondo)
                Box(
                    modifier = GlanceModifier
                        .fillMaxHeight()
                        .defaultWeight()
                        .padding(bottom = 2.dp),
                    contentAlignment = when (alignment) {
                        ViewConfig.Alignment.LEFT -> Alignment.BottomStart
                        ViewConfig.Alignment.RIGHT -> Alignment.BottomEnd
                        else -> Alignment.BottomCenter
                    },
                ) {
                    Text(
                        text = value,
                        style = TextStyle(
                            color = textColor,
                            fontSize = autoTextSp.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            textAlign = textAlign,
                        ),
                        maxLines = 1,
                    )
                }

                // RIGHT ARROW COLUMN (for LEFT alignment only)
                if (alignment == ViewConfig.Alignment.LEFT && showArrow) {
                    ArrowColumn(rotationDegrees, iconSizeDp, arrowColWidthDp, textColor)
                }

                // RIGHT SPACER for CENTER alignment (balances left arrow to keep text centered)
                if (alignment == ViewConfig.Alignment.CENTER && showArrow) {
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
    iconSizeDp: Dp = 26.dp,
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
            modifier = GlanceModifier.size(iconSizeDp).absolutePadding(top = 4.dp),
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
private const val CHAR_WIDTH_FACTOR = 0.30f // Intentionally low vs actual ~0.6x; widthFactor per-case compensates
private const val LINE_HEIGHT_FACTOR = 0.75f // Accounts for monospace cap height ≈ 70% of sp value

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

    val fieldArea = widgetWidth.value * widgetHeight.value
    val isWide = widgetWidth.value > 200

    // Title row height mirrors WPrimeGlanceView: wide = 32 dp, narrow = 28 dp.
    // Keep in sync with the titleRowHeight block above or text sizing will be off.
    val titleRowHeight = if (isWide) 32.dp else 28.dp
    val verticalMargins = if (fieldArea > 20000) 4.dp else 2.dp
    val availH = (widgetHeight - titleRowHeight - verticalMargins).coerceAtLeast(0.dp).value
    if (availW <= 0f || availH <= 0f) {
        return safeMax
    }

    val targetChars = fixedCharCount ?: value.length

    // Use ACTUAL text length (not fixedCharCount) for width-factor decision so that
    // short values (e.g. "9.4", 3 chars) are not penalised by a larger fixedCharCount,
    // while longer values (e.g. "11.6", 4 chars) still get the protection they need.
    val widthFactorChars = value.length
    val widthFactorAdjustment = when {
        widthFactorChars <= 2 -> 1.0f
        widthFactorChars == 3 -> if (isWide) 1.0f else 1.85f // Must fit "100" in narrow: ~47sp avoids truncation
        widthFactorChars == 4 -> if (isWide) 1.0f else 1.6f // Reduced from 2.0 → less aggressive for "10.3"-style values
        widthFactorChars == 5 -> if (isWide) 1.05f else 1.7f
        else -> if (isWide) 1.2f else 1.8f
    }

    val avgUnitPerChar = 1.0f
    // When current text is shorter than fixedCharCount, use actual char count for width
    // so a 3-char "9.9" gets sized as 3 chars, not penalised by fixedCharCount=4
    val effectiveCharsForWidth = if (fixedCharCount != null && value.length < fixedCharCount) {
        value.length.toFloat()
    } else {
        targetChars.toFloat()
    }
    val units = effectiveCharsForWidth * avgUnitPerChar * widthFactorAdjustment

    val fromWidth = if (units * CHAR_WIDTH_FACTOR > 0) (availW / (units * CHAR_WIDTH_FACTOR)) else safeMax.toFloat()
    // Increase height usage factor for better vertical space utilization
    val adjustedFraction = targetHeightFraction.coerceIn(0.5f, 0.95f) // Aumentado de 0.9 a 0.95
    val fromHeight = (availH * adjustedFraction) / LINE_HEIGHT_FACTOR
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
