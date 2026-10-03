package com.itl.wprimeext.simulator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerScenarioTest {
    @Test fun importsSilenceAndEventsWithoutInventingPower() {
        val samples = PowerScenario.parse("time_s,power_w,event\n0,400,RECORDING\n1.5,,LOST\n6,0,PAUSED")
        assertEquals(1500L, samples[1].timeMs)
        assertNull(samples[1].power)
        assertEquals("LOST", samples[1].event)
        assertEquals(0.0, samples[2].power!!, 0.0)
    }

    @Test fun rejectsInvalidInputs() {
        listOf(
            "0,NaN,", "0,-1,", "0,Infinity,", "0,2001,", "0,20,UNKNOWN",
            "0,20,\n0,10,", "1,20,\n0,10,", "-1,20,", "NaN,20,",
        ).forEach { rows ->
            assertThrows(IllegalArgumentException::class.java) { PowerScenario.parse("time_s,power_w,event\n$rows") }
        }
        assertThrows(IllegalArgumentException::class.java) { PowerScenario.parse("time,power\n0,2") }
        assertThrows(IllegalArgumentException::class.java) { PowerScenario.parse("time_s,power_w,event") }
    }

    @Test fun builtInLossScenarioIsDeterministic() {
        val first = PowerScenario.builtIn("Sensor loss")
        assertEquals(first, PowerScenario.builtIn("Sensor loss"))
        assertEquals("LOST", first[90].event)
        assertTrue(first.subList(90, 151).all { it.power == null })
        assertEquals("FOUND", first[151].event)
    }
}
