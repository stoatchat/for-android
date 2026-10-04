package chat.stoat.api.realtime

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal val LONG_BACKGROUND_THRESHOLD: Duration = 30.seconds
internal val STALE_CONNECTION_THRESHOLD: Duration = 75.seconds

internal fun shouldReconnectOnForeground(
    state: DisconnectionState,
    backgroundDuration: Duration,
    connectionIsStale: Boolean,
): Boolean =
    state != DisconnectionState.Connected ||
        connectionIsStale ||
        backgroundDuration >= LONG_BACKGROUND_THRESHOLD
