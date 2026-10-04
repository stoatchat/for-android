package chat.stoat.api.internals

import io.ktor.client.engine.HttpClientEngine

/**
 * Millis clock that keeps counting while the device is asleep (unlike `TimeSource.Monotonic`).
 * Guaranteed aligned to realworld seconds.
 *
 * Only meaningful as a difference.
 */
expect fun elapsedRealtimeMillis(): Long

internal expect fun platformUserAgent(): String

expect fun friendlySessionName(): String

internal expect fun defaultHttpEngine(): HttpClientEngine
