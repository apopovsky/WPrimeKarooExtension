package com.itl.wprimeext.extension

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WPrimeFitProjectionTest {
    private fun state() = WPrimeEngine().snapshot().copy(rideState = WPrimeRideState.RECORDING)

    @Test fun constantPowerRecordsEveryNewTimestampButNotCosmeticDuplicates() {
        val projection = WPrimeFitProjection()
        val initial = state()
        assertEquals(WPrimeFitMessage.RECORD, projection.next(initial)!!.message)
        assertNull(projection.next(initial.copy(configuration = initial.configuration.copy(showArrow = false))))
        assertNotNull(projection.next(initial.copy(timestampMs = 1000)))
    }

    @Test fun pausedMessagesOnlyContainChangedRoundedValues() {
        val projection = WPrimeFitProjection()
        val paused = state().copy(rideState = WPrimeRideState.PAUSED, wPrimeJoules = 10000.1, percentage = 83.1)
        assertEquals(WPrimeFitMessage.SESSION, projection.next(paused)!!.message)
        assertNull(projection.next(paused.copy(timestampMs = 3000, wPrimeJoules = 10000.2)))
        assertNotNull(projection.next(paused.copy(timestampMs = 6000, wPrimeJoules = 10001.1)))
    }

    @Test fun idleAndFitDisabledNeverWriteAndResetEligibility() {
        val projection = WPrimeFitProjection()
        val recording = state()
        assertNotNull(projection.next(recording))
        assertNull(projection.next(recording.copy(configuration = recording.configuration.copy(recordFit = false))))
        assertNotNull(projection.next(recording))
        assertNull(projection.next(recording.copy(rideState = WPrimeRideState.IDLE)))
        assertNotNull(projection.next(recording))
    }

    @Test fun publishedFitUnitsStayJoulesAndUnsignedRanges() {
        val values = WPrimeFitProjection().next(state().copy(wPrimeJoules = 12000.0, percentage = 100.0))!!
        assertEquals(12000.0, values.joules, 0.0)
        assertEquals(100.0, values.percentage, 0.0)
        assertEquals(4294967295.0, WPrimeFitProjection().next(state().copy(wPrimeJoules = 1e15))!!.joules, 0.0)
    }

    @Test fun changedPhysiologyAtSameTimeCanReplaceCurrentRecordValues() {
        val projection = WPrimeFitProjection()
        val initial = state()
        projection.next(initial)
        assertNotNull(projection.next(initial.copy(wPrimeJoules = 20000.0)))
    }

    @Test fun fitJoulesPreserveHalfUpRoundingWithoutIntOverflow() {
        val projection = WPrimeFitProjection()
        assertEquals(3.0, projection.next(state().copy(wPrimeJoules = 2.5))!!.joules, 0.0)
        assertEquals(3000000001.0, projection.next(state().copy(wPrimeJoules = 3000000000.5))!!.joules, 0.0)
    }
}
