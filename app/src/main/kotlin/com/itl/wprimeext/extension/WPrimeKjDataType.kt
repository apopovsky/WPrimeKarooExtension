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

class WPrimeKjDataType(
    karooSystem: KarooSystemService,
    context: Context,
    extension: String,
) : WPrimeDataTypeBase(karooSystem, context, extension, "wprime-kj") {

    // Display in kJ with 1 decimal (e.g. "7.5", "12.0") – 4 chars max
    override fun getDisplayText(joulesValue: Double): String = "%.1f".format(joulesValue / 1000.0)

    override fun getFormatDataTypeId(): String {
        return DataType.Type.POWER // Use power-like numeric format (no percent)
    }

    override fun getUnitText(): String = "kJ"

    override fun getFieldLabel(): String = "W' (kJ)"

    // Stream mapping: emit kJ directly
    override fun getInitialStreamValue(): Double = getAnaerobicCapacity() / 1000.0
    override fun mapJoulesToStreamValue(joules: Double): Double = joules / 1000.0

    override fun getTargetHeightFraction(): Float = 0.5f  // same as base default
    override fun getFixedCharCount(): Int = 4             // "12.0" → 4 chars max
    override fun getSizeScale(): Float = 1.0f             // no extra reduction needed
}
