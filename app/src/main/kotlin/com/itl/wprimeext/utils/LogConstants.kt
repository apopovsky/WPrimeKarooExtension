package com.itl.wprimeext.utils

object LogConstants {
    // Extension lifecycle
    const val EXTENSION_STARTED = "Extension started"
    const val EXTENSION_STOPPED = "Extension stopped"
    const val SERVICE_CONNECTED = "Karoo service connected"
    const val SERVICE_DISCONNECTED = "Karoo service disconnected"

    // W Prime calculations
    const val WPRIME_INITIALIZED = "W Prime calculator initialized"
    const val WPRIME_CONFIG_UPDATING = "Updating W Prime configuration"
    const val WPRIME_CONFIG_UPDATED = "W Prime configuration updated"
    const val WPRIME_DEPLETED = "W Prime fully depleted"

    // Settings operations
    const val SETTINGS_SAVED = "Settings saved to storage"

    // Test/action broadcasts
    const val INTENT_RECEIVED = "Broadcast intent received"
}
