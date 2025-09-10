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
import androidx.glance.LocalSize
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
import androidx.glance.layout.wrapContentWidth
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
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
    currentPower: Int,
    criticalPower: Int,
    wPrimePercentage: Float,
    textSize: Int = 56, // This will act as maxSp for dynamic sizing
    alignment: ViewConfig.Alignment = ViewConfig.Alignment.RIGHT,
    maxPowerDeltaForFullRotation: Int = 150,
    numberVerticalOffset: Int = 0, // new configurable vertical offset
    targetHeightFraction: Float = 0.5f,
    valueBottomExtraPadding: Int = 0,
    fixedCharCount: Int? = null, // NEW
    sizeScale: Float = 1f, // NEW scale multiplier
) {
    val (textAlign, horizontalAlignment) = when (alignment) {
        ViewConfig.Alignment.LEFT -> TextAlign.Start to Alignment.Start
        ViewConfig.Alignment.CENTER -> TextAlign.Center to Alignment.CenterHorizontally
        ViewConfig.Alignment.RIGHT -> TextAlign.End to Alignment.End
    }

    val powerDelta = currentPower - criticalPower
    val wPrimeIsFull = wPrimePercentage >= 0.99f
    val showArrow = !(currentPower < criticalPower && wPrimeIsFull)

    val rotationDegrees = if (showArrow) {
        val rotationRatio = (powerDelta.toFloat() / maxPowerDeltaForFullRotation).coerceIn(-1f, 1f)
        ((if (powerDelta == 0) 0f else rotationRatio * 90f) / 15f).roundToInt() * 15f
    } else {
        0f
    }

    // Dynamic text size calculation
    val currentWidgetSize = LocalSize.current
    val iconSizeDp = 28.dp // Consistent with icon display
    val iconStartPaddingDp = 8.dp // Consistent with icon display
    // Reserve space for sizing heuristic only if arrow will be shown
    val sizingReservedHorizontal = if (showArrow) iconSizeDp + iconStartPaddingDp else 0.dp

    val baseAutoSp = pickTextSizeSp(
        value = value,
        widgetWidth = currentWidgetSize.width,
        widgetHeight = currentWidgetSize.height,
        reservedHorizontal = sizingReservedHorizontal, // use reserved width only for sizing
        maxSp = textSize, // revert to provided max
        minSp = 24, // Default minimum, can be adjusted
        targetHeightFraction = targetHeightFraction,
        fixedCharCount = fixedCharCount,
    )
    val autoTextSp = (baseAutoSp * sizeScale).toInt().coerceAtLeast(8)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(8.dp)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = horizontalAlignment,
            verticalAlignment = Alignment.Top,
            modifier = GlanceModifier.fillMaxHeight().wrapContentWidth(),
        ) {
            TitleRow(fieldLabel, textAlign, horizontalAlignment)
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(top = (2 + numberVerticalOffset).dp, bottom = (2 + valueBottomExtraPadding).dp),
            ) {
                Text(
                    text = value,
                    modifier = GlanceModifier.fillMaxWidth(),
                    style = TextStyle(
                        color = UnitColorProvider(Color.White),
                        fontSize = autoTextSp.sp, // Use dynamically calculated text size
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                )

                if (showArrow) {
                    // Overlay arrow without affecting text layout
                    Row(
                        modifier = GlanceModifier
                            .padding(start = iconStartPaddingDp)
                            .height(iconSizeDp),
                        verticalAlignment = Alignment.CenterVertically,
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
                            colorFilter = ColorFilter.tint(ColorProvider(Color.White)),
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun TitleRow(
    text: String,
    textAlign: TextAlign,
    horizontalAlignment: Alignment.Horizontal,
) {
    Row(
        horizontalAlignment = horizontalAlignment,
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier
            .padding(0.dp)
            .height(22.dp), // This height is used in pickTextSizeSp
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_wprime_battery),
            contentDescription = "W' Icon",
            modifier = GlanceModifier.size(22.dp).absolutePadding(top = 4.dp),
        )
        Text(
            text = text,
            style = TextStyle(
                color = UnitColorProvider(Color.White),
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
            ),
        )
    }
}

/**
 * Glance composable for "Not Available" or "Searching" states
 */
@SuppressLint("RestrictedApi")
@Composable
fun WPrimeNotAvailableGlanceView(
    message: String = "N/A",
    isKaroo3: Boolean = true,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .let { if (isKaroo3) it.cornerRadius(8.dp) else it }
            .padding(1.dp),
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.Gray)
                .padding(8.dp), // This padding is used as 'margins' in pickTextSizeSp
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
private const val CHAR_WIDTH_FACTOR = 0.62f
private const val LINE_HEIGHT_FACTOR = 1.2f

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
    val availW = (widgetWidth - reservedHorizontal * 2 - 4.dp).coerceAtLeast(0.dp).value
    val titleRowHeight = 22.dp
    val verticalMargins = 8.dp
    val availH = (widgetHeight - titleRowHeight - verticalMargins).coerceAtLeast(0.dp).value
    if (availW <= 0f || availH <= 0f) {
        return safeMax
    }

    val targetChars = fixedCharCount ?: value.length
    // Approx char units using monospace proportions from effectiveCharUnits for '8' width baseline
    val avgUnitPerChar = 1.0f // treat each char equal so fixed width stable
    val units = targetChars * avgUnitPerChar

    val fromWidth = if (units * CHAR_WIDTH_FACTOR > 0) (availW / (units * CHAR_WIDTH_FACTOR)) else safeMax.toFloat()
    val adjustedFraction = targetHeightFraction.coerceIn(0.3f, 0.85f)
    val fromHeight = (availH * adjustedFraction) / LINE_HEIGHT_FACTOR
    val raw = fromWidth.coerceAtMost(fromHeight)
    val clamped = raw.coerceIn(minSp.toFloat(), safeMax.toFloat())

    // For fixedCharCount we skip quantization to preserve precise scaling
    if (fixedCharCount != null) return clamped.toInt()

    // Previous quantization for percentage case if not fixed
    val steps = listOf(64, 56, 50, 46, 42, 38, 34, 32, 30, 28, 26, 24)
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
        wPrimePercentage = 0.65f,
        textSize = 50, // This is maxSp
        alignment = ViewConfig.Alignment.CENTER,
        maxPowerDeltaForFullRotation = 150,
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
        wPrimePercentage = 0.9f,
        textSize = 50, // This is maxSp
        alignment = ViewConfig.Alignment.CENTER,
        maxPowerDeltaForFullRotation = 150,
    )
}

@Suppress("unused")
@SuppressLint("RestrictedApi")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun WPrimeGlanceViewPreview_FullNoArrow() {
    WPrimeGlanceView(
        value = "11438", // Long value to test auto-sizing
        fieldLabel = "W' (kJ)",
        backgroundColor = UnitColorProvider(Color.hsl(120f, 0.5f, 0.5f)),
        currentPower = 100,
        criticalPower = 200,
        wPrimePercentage = 1.0f,
        textSize = 50, // This is maxSp
        alignment = ViewConfig.Alignment.CENTER,
        maxPowerDeltaForFullRotation = 150,
    )
}

@Suppress("unused")
@SuppressLint("RestrictedApi")
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun WPrimeGlanceViewPreview_MaxEffort() {
    WPrimeGlanceView(
        value = "3789", // Another long value
        fieldLabel = "W' (kJ)",
        backgroundColor = UnitColorProvider(Color.Red),
        currentPower = 380,
        criticalPower = 200,
        wPrimePercentage = 0.1f,
        textSize = 50, // This is maxSp
        alignment = ViewConfig.Alignment.CENTER,
        maxPowerDeltaForFullRotation = 150,
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
        wPrimePercentage = 0.75f,
        textSize = 50, // This is maxSp
        alignment = ViewConfig.Alignment.CENTER,
        maxPowerDeltaForFullRotation = 150,
    )
}
