package com.itl.wprimeext.utils

object LogConstants {
    // Extension lifecycle
    const val EXTENSION_STARTED = "Extension started"
    const val EXTENSION_STOPPED = "Extension stopped"
    const val SERVICE_CONNECTED = "Karoo service connected"
    const val SERVICE_DISCONNECTED = "Karoo service disconnected"

    // Data streaming
    const val STREAM_STARTED = "Data stream started"
    const val STREAM_STOPPED = "Data stream stopped"
    const val STREAM_PAUSED = "Data stream paused"
    const val STREAM_RESUMED = "Data stream resumed"

    // W Prime calculations
    const val WPRIME_INITIALIZED = "W Prime calculator initialized"
    const val WPRIME_CONFIG_UPDATED = "W Prime configuration updated"
    const val WPRIME_DEPLETED = "W Prime fully depleted"
    const val WPRIME_RECOVERING = "W Prime recovering"

    // Settings operations
    const val SETTINGS_LOADED = "Settings loaded from storage"
    const val SETTINGS_SAVED = "Settings saved to storage"
    const val SETTINGS_DEFAULT = "Using default settings"
    const val SETTINGS_ERROR = "Error loading/saving settings"

    // UI operations
    const val UI_SCREEN_OPENED = "Configuration screen opened"
    const val UI_VALUES_CHANGED = "Configuration values changed"
    const val UI_VALIDATION_ERROR = "Configuration validation error"

    // System notifications
    const val NOTIFICATION_SENT = "System notification sent"
    const val INTENT_RECEIVED = "Broadcast intent received"

    // FIT file operations
    const val FIT_RECORD_WRITTEN = "FIT record written"
    const val FIT_SESSION_WRITTEN = "FIT session data written"
    const val FIT_EVENT_WRITTEN = "FIT event written"
}
