package chat.stoat.api.realtime.frames.receivable

import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.ChannelSlowmode
import chat.stoat.core.model.util.ChannelVoiceState
import chat.stoat.core.model.schemas.Embed
import chat.stoat.core.model.schemas.Emoji
import chat.stoat.core.model.schemas.Member
import chat.stoat.core.model.schemas.Message
import chat.stoat.core.model.util.PartialUserVoiceState
import chat.stoat.core.model.schemas.Role
import chat.stoat.core.model.schemas.Server
import chat.stoat.core.model.schemas.ServerUserChoice
import chat.stoat.core.model.schemas.User
import chat.stoat.core.model.util.UserVoiceState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AnyFrame(
    val type: String
)

@Serializable
data class ErrorFrame(
    val type: String = "Error",
    val error: String
)

@Serializable
data class BulkFrame(
    val type: String = "Bulk",
    val v: List<JsonObject>
)

@Serializable
data class PongFrame(
    val type: String = "Pong",
    val data: Long
)

@Serializable
data class ReadyFrame(
    val type: String = "Ready",
    val users: List<User>,
    val servers: List<Server>,
    val channels: List<Channel>,
    val emojis: List<Emoji>,
    @SerialName("voice_states") val voiceStates: List<ChannelVoiceState> = listOf(),
)

typealias MessageFrame = Message

@Serializable
data class MessageUpdateFrame(
    val type: String = "MessageUpdate",
    val id: String,
    val channel: String,
    val data: JsonObject
)

@Serializable
data class Appendable(
    val embeds: List<Embed>? = null
)

@Serializable
data class MessageAppendFrame(
    val type: String = "MessageAppend",
    val id: String,
    val channel: String,
    val append: Appendable
)

@Serializable
data class MessageDeleteFrame(
    val type: String = "MessageDelete",
    val id: String,
    val channel: String
)

@Serializable
data class MessageReactFrame(
    val type: String = "MessageReact",
    val id: String,
    val channel_id: String,
    val user_id: String,
    val emoji_id: String
)

@Serializable
data class MessageUnreactFrame(
    val type: String = "MessageUnreact",
    val id: String,
    val channel_id: String,
    val user_id: String,
    val emoji_id: String
)

@Serializable
data class MessageRemoveReactionFrame(
    val type: String = "MessageRemoveReaction",
    val id: String,
    val channel_id: String,
    val emoji_id: String
)

/* ChannelCreate: we already have a "type" property in channel so we just alias the type */
typealias ChannelCreateFrame = Channel

@Serializable
data class ChannelUpdateFrame(
    val type: String = "ChannelUpdate",
    val id: String,
    val data: Channel,
    val clear: List<String>? = null // "Icon" or "Description"
)

@Serializable
data class ChannelDeleteFrame(
    val type: String = "ChannelDelete",
    val id: String
)

@Serializable
data class ChannelGroupJoinFrame(
    val type: String = "ChannelGroupJoin",
    val id: String,
    val user: String
)

@Serializable
data class ChannelGroupLeaveFrame(
    val type: String = "ChannelGroupLeave",
    val id: String,
    val user: String
)

@Serializable
data class ChannelStartTypingFrame(
    val type: String = "ChannelStartTyping",
    val id: String,
    val user: String
)

@Serializable
data class ChannelStopTypingFrame(
    val type: String = "ChannelStopTyping",
    val id: String,
    val user: String
)

@Serializable
data class ChannelAckFrame(
    val type: String = "ChannelAck",
    val id: String,
    val user: String,
    @SerialName("message_id")
    val messageId: String
)

@Serializable
data class UserSlowmodesFrame(
    val type: String = "UserSlowmodes",
    val slowmodes: List<ChannelSlowmode>,
)

@Serializable
data class ServerCreateFrame(
    val type: String = "ServerCreate",
    val id: String,
    val server: Server,
    val channels: List<Channel>
)

@Serializable
data class ServerUpdateFrame(
    val type: String = "ServerUpdate",
    val id: String,
    val data: Server,
    val clear: List<String>? = null // "Icon", "Banner" or "Description"
)

@Serializable
data class ServerDeleteFrame(
    val type: String = "ServerDelete",
    val id: String
)

@Serializable
data class ServerMemberUpdateFrame(
    val type: String = "ServerMemberUpdate",
    val id: ServerUserChoice,
    val data: Member,
    val clear: List<String>? = null // "Nickname" or "Avatar"
)

@Serializable
data class ServerMemberJoinFrame(
    val type: String = "ServerMemberJoin",
    val id: String,
    val user: String
)

@Serializable
data class ServerMemberLeaveFrame(
    val type: String = "ServerMemberLeave",
    val id: String,
    val user: String
)

@Serializable
data class ServerRoleUpdateFrame(
    val type: String = "ServerRoleUpdate",
    val id: String,
    @SerialName("role_id")
    val roleId: String,
    val data: Role,
    val clear: List<String>? = null // "Colour"
)

@Serializable
data class ServerRoleDeleteFrame(
    val type: String = "ServerRoleDelete",
    val id: String,
    @SerialName("role_id")
    val roleId: String
)

@Serializable
data class UserUpdateFrame(
    val type: String = "UserUpdate",
    val id: String,
    val data: User,
    val clear: List<String>? = null
)

@Serializable
data class UserRelationshipFrame(
    val type: String = "UserRelationship",
    val id: String,
    val user: User,
    val status: String? = null
)

typealias EmojiCreateFrame = Emoji

@Serializable
data class EmojiDeleteFrame(
    val type: String = "EmojiDelete",
    val id: String
)

@Serializable
data class VoiceChannelJoinFrame(
    val type: String = "VoiceChannelJoin",
    val id: String,
    val state: UserVoiceState
)

@Serializable
data class VoiceChannelLeaveFrame(
    val type: String = "VoiceChannelLeave",
    val id: String,
    val user: String
)

@Serializable
data class VoiceChannelMoveFrame(
    val type: String = "VoiceChannelMove",
    val from: String,
    val to: String,
    val state: UserVoiceState
)

@Serializable
data class UserVoiceStateUpdateFrame(
    val type: String = "UserVoiceStateUpdate",
    val id: String,
    @SerialName("channel_id") val channelId: String,
    val data: PartialUserVoiceState
)

@Serializable
data class UserMoveVoiceChannelFrame(
    val type: String = "UserMoveVoiceChannel",
    val node: String,
    val token: String,
)
