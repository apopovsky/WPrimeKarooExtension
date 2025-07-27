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

    override fun getDisplayValue(): Double {
        return getCalculator().getWPrimePercentage()
    }

    override fun getInitialValue(): Double {
        return 100.0 // Initial value for W Prime percentage
    }

    override fun getFormatDataTypeId(): String {
        return DataType.Type.PERCENT_MAX_FTP
    }

    override fun getDisplayText(value: Double): String {
        return "${value.toInt()}"
    }

    override fun getUnitText(): String {
        return "%"
    }

    override fun getFieldLabel(wideMode: Boolean): String {
        return if (wideMode) {
            "W PRIME (%W')"
        } else {
            "W' (%W')"
        }
    }
}
