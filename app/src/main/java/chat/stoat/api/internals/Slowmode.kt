package chat.stoat.api.internals

import chat.stoat.core.model.schemas.ChannelSlowmode

data class ActiveSlowmode(
    val durationSeconds: Long,
    val expiresAtMilliseconds: Long,
) {
    fun remainingSeconds(nowMilliseconds: Long): Long {
        val remainingMilliseconds = expiresAtMilliseconds - nowMilliseconds
        if (remainingMilliseconds <= 0) return 0

        return (remainingMilliseconds + 999) / 1_000
    }

    companion object {
        fun from(
            slowmode: ChannelSlowmode,
            receivedAtMilliseconds: Long,
        ) = ActiveSlowmode(
            durationSeconds = slowmode.duration,
            expiresAtMilliseconds =
                receivedAtMilliseconds + slowmode.retryAfter.coerceAtLeast(0) * 1_000,
        )
    }
}

internal fun formatCompactDuration(totalSeconds: Long): String {
    var remaining = totalSeconds.coerceAtLeast(0)
    val days = remaining / 86_400
    remaining %= 86_400
    val hours = remaining / 3_600
    remaining %= 3_600
    val minutes = remaining / 60
    val seconds = remaining % 60

    val parts = buildList {
        if (days > 0) add("${days}d")
        if (hours > 0) add("${hours}h")
        if (minutes > 0) add("${minutes}m")
        if (seconds > 0) add("${seconds}s")
    }

    return parts.take(2).joinToString(" ").ifEmpty { "0s" }
}
