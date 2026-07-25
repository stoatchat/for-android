package chat.stoat.internals

import android.net.Uri
import androidx.core.net.toUri
import chat.stoat.api.internals.isUlid
import chat.stoat.callbacks.Action
import chat.stoat.core.model.data.STOAT_BETA_WEB_APP
import chat.stoat.core.model.data.STOAT_WEB_APP

sealed interface StoatWebLink {
    data class Server(val serverId: String) : StoatWebLink
    data class Channel(val channelId: String) : StoatWebLink
    data class Message(val channelId: String, val messageId: String) : StoatWebLink
}

fun Uri.toStoatWebLinkOrNull(): StoatWebLink? {
    if (scheme != "https") return null

    val validHosts = setOf(
        STOAT_WEB_APP.toUri().host,
        STOAT_BETA_WEB_APP.toUri().host,
    )
    if (host !in validHosts) return null

    val segments = pathSegments

    return when {
        segments.size == 2 &&
                segments[0] == "channel" &&
                segments[1].isUlid() ->
            StoatWebLink.Channel(channelId = segments[1])

        segments.size == 3 &&
                segments[0] == "channel" &&
                segments[1].isUlid() &&
                segments[2].isUlid() ->
            StoatWebLink.Message(
                channelId = segments[1],
                messageId = segments[2],
            )

        segments.size == 2 &&
                segments[0] == "server" &&
                segments[1].isUlid() ->
            StoatWebLink.Server(serverId = segments[1])

        segments.size == 4 &&
                segments[0] == "server" &&
                segments[1].isUlid() &&
                segments[2] == "channel" &&
                segments[3].isUlid() ->
            StoatWebLink.Channel(channelId = segments[3])

        segments.size == 5 &&
                segments[0] == "server" &&
                segments[1].isUlid() &&
                segments[2] == "channel" &&
                segments[3].isUlid() &&
                segments[4].isUlid() ->
            StoatWebLink.Message(
                channelId = segments[3],
                messageId = segments[4],
            )

        else -> null
    }
}

fun StoatWebLink.toNavigationAction(): Action = when (this) {
    is StoatWebLink.Server -> Action.SwitchServer(serverId)
    is StoatWebLink.Channel -> Action.SwitchChannel(channelId)
    is StoatWebLink.Message -> Action.JumpToMessage(channelId, messageId)
}
