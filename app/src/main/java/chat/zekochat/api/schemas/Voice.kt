package chat.zekochat.api.schemas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChannelVoiceState(
    val id: String,
    val participants: List<UserVoiceState>,
    val node: String,
)

@Serializable
data class UserVoiceState(
    val id: String,
    @SerialName("is_receiving") val isReceiving: Boolean,
    @SerialName("is_publishing") val isPublishing: Boolean,
    val screensharing: Boolean,
    val camera: Boolean,
)