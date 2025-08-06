package com.itl.wprimeext.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.itl.wprimeext.R
import androidx.glance.unit.ColorProvider as UnitColorProvider
import io.hammerhead.karooext.models.ViewConfig

/**
 * Glance composable for W' display - follows CustomDoubleTypeView pattern
 */
@Composable
fun WPrimeGlanceView(
    value: String,
    fieldLabel: String,
    backgroundColor: UnitColorProvider,
    textSize: Int = 56,
    alignment: ViewConfig.Alignment = ViewConfig.Alignment.RIGHT,
    numberVerticalOffset: Int = -15
) {
    val (textAlign, horizontalAlignment) = when (alignment) {
        ViewConfig.Alignment.LEFT -> TextAlign.Start to Alignment.Start
        ViewConfig.Alignment.CENTER -> TextAlign.Center to Alignment.CenterHorizontally
        ViewConfig.Alignment.RIGHT -> TextAlign.End to Alignment.End
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(8.dp)
            .background(backgroundColor),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = horizontalAlignment,
            verticalAlignment = Alignment.Top,
            modifier = GlanceModifier
                .fillMaxSize()
        ) {

            TitleRow(fieldLabel, textAlign, horizontalAlignment)

            NumberRow(value, textAlign, horizontalAlignment, textSize, numberVerticalOffset)
        }
    }
}

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
            .height(22.dp)
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
                textAlign = textAlign
            )
        )
    }
}

@Composable
private fun NumberRow(
    text: String,
    textAlign: TextAlign,
    horizontalAlignment: Alignment.Horizontal,
    textSize: Int,
    numberVerticalOffset: Int
) {
    Row(
        modifier = GlanceModifier
            .padding(top = numberVerticalOffset.dp)
            .fillMaxWidth(),
        horizontalAlignment = horizontalAlignment,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = text,
            modifier = GlanceModifier
                .fillMaxWidth(), // make the text occupy full width
            style = TextStyle(
                color = UnitColorProvider(Color.White),
                fontSize = textSize.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign
            )
        )
    }
}

/**
 * Glance composable for "Not Available" or "Searching" states
 */
@Composable
fun WPrimeNotAvailableGlanceView(
    message: String = "N/A",
    isKaroo3: Boolean = true,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .let { if (isKaroo3) it.cornerRadius(8.dp) else it }
            .padding(1.dp)
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.Gray)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = message,
                style = TextStyle(
                    color = UnitColorProvider(Color.White),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun WPrimeGlanceViewPreview() {
    WPrimeGlanceView(
        value = "1234",
        fieldLabel = "W' (kJ)",
        backgroundColor = UnitColorProvider(Color.Gray),
        textSize = 69,
        alignment = ViewConfig.Alignment.CENTER,
        numberVerticalOffset = -19
    )
}
