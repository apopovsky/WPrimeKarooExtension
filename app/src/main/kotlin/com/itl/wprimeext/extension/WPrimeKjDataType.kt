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
import kotlin.math.roundToInt

class WPrimeKjDataType(
    karooSystem: KarooSystemService,
    context: Context,
    extension: String,
) : WPrimeDataTypeBase(karooSystem, context, extension, "wprime-kj") {

    // Display text uses raw Joules rounded
    override fun getDisplayText(joulesValue: Double): String = joulesValue.roundToInt().toString()

    override fun getFormatDataTypeId(): String {
        return DataType.Type.POWER // Use power-like numeric format (no percent)
    }

    override fun getUnitText(): String = "J"

    override fun getFieldLabel(): String = "W' (J)"

    // Stream mapping: emit Joules directly
    override fun getInitialStreamValue(): Double = getAnaerobicCapacity()
    override fun mapJoulesToStreamValue(joules: Double): Double = joules

    override fun getNumberVerticalOffset(): Int = 0 // align baseline
    override fun getTargetHeightFraction(): Float = 0.43f // smaller
    override fun getValueBottomPaddingExtra(): Int = 1 // revert
    override fun getFixedCharCount(): Int = 5 // size for 5 chars (e.g., 12000)
    override fun getSizeScale(): Float = 0.85f // further shrink
}
