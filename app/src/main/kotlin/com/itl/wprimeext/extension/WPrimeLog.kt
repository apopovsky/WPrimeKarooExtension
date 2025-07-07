/**
 * Copyright (c) 2024 SRAM LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.itl.wprimeext.extension

import android.util.Log
import timber.log.Timber

/**
 * Centralized logging utility for W Prime Extension.
 * Provides consistent logging format across all components.
 */
object WPrimeLog {

    // Extension constants
    const val EXTENSION_NAME = "WPrimeExt"
    const val EXTENSION_VERSION = "1.0"

    // Component tags
    private const val TAG_DATA_TYPE = "DataType"
    private const val TAG_CALCULATOR = "Calculator"
    private const val TAG_SETTINGS = "Settings"
    private const val TAG_EXTENSION = "Extension"
    private const val TAG_UI = "UI"
    private const val TAG_TEST = "Test"

    // Log levels for production vs debug
    private const val USE_ANDROID_LOG = true // Set to false for Timber only

    /**
     * Format: [WPrimeExt:Component] Message
     */
    private fun formatMessage(component: String, message: String): String {
        return "[$EXTENSION_NAME:$component] $message"
    }

    // DataType logging
    fun dataType(message: String) {
        val formatted = formatMessage(TAG_DATA_TYPE, message)
        if (USE_ANDROID_LOG) Log.i(EXTENSION_NAME, formatted)
        Timber.i(formatted)
    }

    fun dataTypeDebug(message: String) {
        val formatted = formatMessage(TAG_DATA_TYPE, message)
        if (USE_ANDROID_LOG) Log.d(EXTENSION_NAME, formatted)
        Timber.d(formatted)
    }

    // Calculator logging
    fun calculator(message: String) {
        val formatted = formatMessage(TAG_CALCULATOR, message)
        if (USE_ANDROID_LOG) Log.i(EXTENSION_NAME, formatted)
        Timber.i(formatted)
    }

    fun calculatorDebug(message: String) {
        val formatted = formatMessage(TAG_CALCULATOR, message)
        if (USE_ANDROID_LOG) Log.d(EXTENSION_NAME, formatted)
        Timber.d(formatted)
    }

    // Settings logging
    fun settings(message: String) {
        val formatted = formatMessage(TAG_SETTINGS, message)
        if (USE_ANDROID_LOG) Log.i(EXTENSION_NAME, formatted)
        Timber.i(formatted)
    }

    fun settingsDebug(message: String) {
        val formatted = formatMessage(TAG_SETTINGS, message)
        if (USE_ANDROID_LOG) Log.d(EXTENSION_NAME, formatted)
        Timber.d(formatted)
    }

    // Extension/Service logging
    fun extension(message: String) {
        val formatted = formatMessage(TAG_EXTENSION, message)
        if (USE_ANDROID_LOG) Log.i(EXTENSION_NAME, formatted)
        Timber.i(formatted)
    }

    fun extensionDebug(message: String) {
        val formatted = formatMessage(TAG_EXTENSION, message)
        if (USE_ANDROID_LOG) Log.d(EXTENSION_NAME, formatted)
        Timber.d(formatted)
    }

    // UI logging
    fun ui(message: String) {
        val formatted = formatMessage(TAG_UI, message)
        if (USE_ANDROID_LOG) Log.i(EXTENSION_NAME, formatted)
        Timber.i(formatted)
    }

    fun uiDebug(message: String) {
        val formatted = formatMessage(TAG_UI, message)
        if (USE_ANDROID_LOG) Log.d(EXTENSION_NAME, formatted)
        Timber.d(formatted)
    }

    // Test mode logging
    fun test(message: String) {
        val formatted = formatMessage(TAG_TEST, message)
        if (USE_ANDROID_LOG) Log.i(EXTENSION_NAME, formatted)
        Timber.i(formatted)
    }

    fun testDebug(message: String) {
        val formatted = formatMessage(TAG_TEST, message)
        if (USE_ANDROID_LOG) Log.d(EXTENSION_NAME, formatted)
        Timber.d(formatted)
    }

    // Error logging (always logs to both)
    fun error(message: String, throwable: Throwable? = null) {
        val formatted = formatMessage("ERROR", message)
        Log.e(EXTENSION_NAME, formatted, throwable)
        if (throwable != null) {
            Timber.e(throwable, formatted)
        } else {
            Timber.e(formatted)
        }
    }

    // Warning logging
    fun warn(message: String) {
        val formatted = formatMessage("WARN", message)
        Log.w(EXTENSION_NAME, formatted)
        Timber.w(formatted)
    }
}
