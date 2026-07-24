package chat.stoat.screens.chat.views.channel

internal data class NearbyBoundaries(
    val canLoadNewer: Boolean,
    val canLoadOlder: Boolean,
)

sealed interface ChannelScrollRequest {
    val requestId: Long

    data class FocusMessage(
        val messageId: String,
        val animated: Boolean,
        override val requestId: Long,
    ) : ChannelScrollRequest

    data class Bottom(
        override val requestId: Long,
    ) : ChannelScrollRequest
}

data class MessageJumpFailure(
    val messageId: String,
    val requestId: Long,
)

internal fun ChannelScreenItem.messageIdOrNull(): String? = when (this) {
    is ChannelScreenItem.RegularMessage -> message.id
    is ChannelScreenItem.ProspectiveMessage -> message.id
    is ChannelScreenItem.FailedMessage -> message.id
    is ChannelScreenItem.SystemMessage -> message.id
    is ChannelScreenItem.DateDivider,
    is ChannelScreenItem.LoadTrigger,
    is ChannelScreenItem.Loading -> null
}

internal fun <T> normalizeByUlid(
    values: Iterable<T>,
    idOf: (T) -> String?,
): List<T> = values
    .mapNotNull { value -> idOf(value)?.let { id -> id to value } }
    .distinctBy { (id) -> id }
    .sortedByDescending { (id) -> id }
    .map { (_, value) -> value }

internal fun calculateNearbyBoundaries(
    messageIds: Iterable<String>,
    targetMessageId: String,
    requestedLimit: Int,
): NearbyBoundaries {
    val ids = messageIds.toList()
    val sideCapacity = requestedLimit / 2 + 1

    return NearbyBoundaries(
        canLoadNewer = ids.count { it >= targetMessageId } >= sideCapacity,
        canLoadOlder = ids.count { it < targetMessageId } >= sideCapacity,
    )
}
