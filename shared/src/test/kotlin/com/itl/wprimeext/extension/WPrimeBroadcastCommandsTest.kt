package com.itl.wprimeext.extension

import io.hammerhead.karooext.models.MarkLap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WPrimeBroadcastCommandsTest {
    @Test fun debugEffectsUseClosedAllowlist() {
        assertEquals(MarkLap, debugRideEffect("io.hammerhead.karooext.models.MarkLap"))
        for (input in listOf(null, "java.lang.Runtime", "io.hammerhead.karooext.models.RequestBluetooth", "MarkLap")) {
            assertNull(debugRideEffect(input))
        }
    }

    @Test fun malformedAlertCommandsAreRejected() {
        assertNull(testAlertCommand("", 25, false, "DROP"))
        assertNull(testAlertCommand("x".repeat(129), 25, false, "DROP"))
        assertNull(testAlertCommand("test", -1, false, "DROP"))
        assertNull(testAlertCommand("test", 101, false, "DROP"))
        assertNull(testAlertCommand("test", 25, false, "unknown"))
        assertEquals(AlertType.REPLENISH, testAlertCommand("test", 25, false, "REPLENISH")!!.alertType)
        assertEquals(AlertType.DROP, testAlertCommand("test", 25, false, null)!!.alertType)
    }
}
