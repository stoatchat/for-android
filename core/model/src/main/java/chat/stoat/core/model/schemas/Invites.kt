package chat.stoat.core.model.schemas

import android.net.Uri
import androidx.core.net.toUri
import chat.stoat.core.model.data.STOAT_INVITES
import chat.stoat.core.model.data.STOAT_WEB_APP
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Invite(
    val type: String? = null,
    val code: String? = null,

    @SerialName("server_id")
    val serverId: String? = null,

    @SerialName("server_name")
    val serverName: String? = null,

    @SerialName("server_icon")
    val serverIcon: AutumnResource? = null,

    @SerialName("server_banner")
    val serverBanner: AutumnResource? = null,

    @SerialName("server_flags")
    val serverFlags: Long? = null,

    @SerialName("channel_id")
    val channelId: String? = null,

    @SerialName("channel_name")
    val channelName: String? = null,

    @SerialName("user_name")
    val userName: String? = null,

    @SerialName("user_avatar")
    val userAvatar: AutumnResource? = null,

    @SerialName("member_count")
    val memberCount: Long? = null
)

@Serializable
data class InviteJoined(
    val type: String? = null,
    val channels: List<Channel>? = null,
    val server: Server? = null
)

@Serializable
data class ServerInvite(
    val type: String,
    @SerialName("_id")
    val id: String,
    val server: String,
    val creator: String,
    val channel: String,
)

fun Uri.isInviteUri(): Boolean {
    val firstPathSegmentIsInvite = this.pathSegments.firstOrNull() == "invite"
    val isStoatChat = this.host == STOAT_WEB_APP.toUri().host
    val matchSttGG = this.host == STOAT_INVITES.toUri().host

    val matchApp = isStoatChat && firstPathSegmentIsInvite

    val hasEnoughSegments =
        if (matchApp) this.pathSegments.size == 2 else this.pathSegments.size == 1

    return (matchApp || matchSttGG) && hasEnoughSegments
}
