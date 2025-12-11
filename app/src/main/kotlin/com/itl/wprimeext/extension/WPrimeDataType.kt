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
package com.itl.wprimeext.extension

import android.content.Context
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataType

class WPrimeDataType(
    karooSystem: KarooSystemService,
    context: Context,
    extension: String,
) : WPrimeDataTypeBase(karooSystem, context, extension, "wprime") {

    // Base emits Joules; we convert to percentage for display
    override fun getDisplayText(joulesValue: Double): String {
        val capacity = getAnaerobicCapacity().coerceAtLeast(1.0)
        val pct = (joulesValue / capacity * 100.0).coerceIn(0.0, 100.0)
        return pct.toInt().toString()
    }

    override fun getFormatDataTypeId(): String = DataType.Type.PERCENT_MAX_FTP

    override fun getUnitText(): String = "%"

    override fun getFieldLabel(): String = "%W'"

    // Stream mapping: provide percent so Karoo can format with percent DataType
    override fun getInitialStreamValue(): Double = 100.0
    override fun mapJoulesToStreamValue(joules: Double): Double {
        val capacity = getAnaerobicCapacity().coerceAtLeast(1.0)
        return (joules / capacity * 100.0).coerceIn(0.0, 100.0)
    }

    override fun getNumberVerticalOffset(): Int = 0 // percent view baseline
    override fun getFixedCharCount(): Int = 3 // Always size for 3 chars (e.g., 100)
}
