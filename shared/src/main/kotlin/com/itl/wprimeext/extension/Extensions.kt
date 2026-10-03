package com.itl.wprimeext.extension

import android.os.SystemClock
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.KarooEvent
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.atomic.AtomicLong

/** The SDK DataPoint has no acquisition time: capture a monotonic time at the callback boundary. */
data class TimedStreamState(val state: StreamState, val timestampMs: Long, val sequence: Long)

fun KarooSystemService.timedStreamDataFlow(dataTypeId: String): Flow<TimedStreamState> = callbackFlow {
    val sequence = AtomicLong()
    val listenerId = addConsumer(OnStreamState.StartStreaming(dataTypeId)) { event: OnStreamState ->
        trySend(TimedStreamState(event.state, SystemClock.elapsedRealtime(), sequence.incrementAndGet()))
    }
    awaitClose { removeConsumer(listenerId) }
}.buffer(64, BufferOverflow.DROP_OLDEST)

inline fun <reified T : KarooEvent> KarooSystemService.consumerFlow(): Flow<T> = callbackFlow {
    val listenerId = addConsumer<T> { trySend(it) }
    awaitClose { removeConsumer(listenerId) }
}

fun KarooSystemService.userProfileFlow(): Flow<UserProfile?> = callbackFlow {
    trySend(null)
    val listenerId = addConsumer<UserProfile> { trySend(it) }
    awaitClose { removeConsumer(listenerId) }
}
