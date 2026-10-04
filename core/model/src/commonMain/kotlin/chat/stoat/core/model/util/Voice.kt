package chat.stoat.core.model.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChannelVoiceState(
    val id: String,
    val participants: List<UserVoiceState> = emptyList(),
)

@Serializable
data class UserVoiceState(
    val id: String,
    @SerialName("is_receiving") val isReceiving: Boolean,
    @SerialName("is_publishing") val isPublishing: Boolean,
    val screensharing: Boolean,
    val camera: Boolean,
    @SerialName(value = "joined_at") val joinedAt: String? = null,
)

@Serializable
data class PartialUserVoiceState(
    val id: String? = null,
    @SerialName("is_receiving") val isReceiving: Boolean? = null,
    @SerialName("is_publishing") val isPublishing: Boolean? = null,
    val screensharing: Boolean? = null,
    val camera: Boolean? = null,
    @SerialName(value = "joined_at") val joinedAt: String? = null,
) {
    fun overrideInto(other: UserVoiceState): UserVoiceState {
        return UserVoiceState(
            id = this.id ?: other.id,
            isReceiving = this.isReceiving ?: other.isReceiving,
            isPublishing = this.isPublishing ?: other.isPublishing,
            screensharing = this.screensharing ?: other.screensharing,
            camera = this.camera ?: other.camera,
            joinedAt = this.joinedAt ?: other.joinedAt,
        )
    }
}