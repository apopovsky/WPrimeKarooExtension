/**
 * Copyright (c) 2024 SRAM LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.itl.wprimeext.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * Glance composable for W' display - follows CustomDoubleTypeView pattern
 */
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 150)
@Composable
fun WPrimeGlanceView(
    value: String,
    fieldLabel: String,
    backgroundColor: ColorProvider,
    textSize: Int = 56,
    alignment: io.hammerhead.karooext.models.ViewConfig.Alignment = io.hammerhead.karooext.models.ViewConfig.Alignment.RIGHT,
) {
    val (textAlign, horizontalAlignment) = when (alignment) {
        io.hammerhead.karooext.models.ViewConfig.Alignment.LEFT ->
            TextAlign.Start to Alignment.Start
        io.hammerhead.karooext.models.ViewConfig.Alignment.CENTER ->
            TextAlign.Center to Alignment.CenterHorizontally
        io.hammerhead.karooext.models.ViewConfig.Alignment.RIGHT ->
            TextAlign.End to Alignment.End
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(8.dp)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = horizontalAlignment,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = GlanceModifier.height(8.dp))
            TitleRow("🔋 $fieldLabel", textAlign, horizontalAlignment)
            Spacer(modifier = GlanceModifier.height(2.dp))
            NumberRow(value, textAlign, horizontalAlignment, textSize)
            Spacer(modifier = GlanceModifier.height(2.dp))
        }
    }
}

@Composable
private fun TitleRow(
    text: String,
    textAlign: TextAlign,
    horizontalAlignment: Alignment.Horizontal
) {
    Row(
        horizontalAlignment = horizontalAlignment,
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier.padding(0.dp).height(16.dp)
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                textAlign = textAlign,
            ),
        )
    }
}

@Composable
private fun NumberRow(
    text: String,
    textAlign: TextAlign,
    horizontalAlignment: Alignment.Horizontal,
    textSize: Int
) {
    Row(
        horizontalAlignment = horizontalAlignment,
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier.padding(0.dp)
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = textSize.sp,
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
                .background(ColorProvider(Color.Gray, Color.Gray))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = TextStyle(
                    color = ColorProvider(Color.White, Color.White),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}
