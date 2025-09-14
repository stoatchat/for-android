package chat.zekochat.api.schemas

import chat.zekochat.api.PeptideAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Session(
    @SerialName("_id") val id: String,
    val name: String
) {
    fun isCurrent(): Boolean {
        return id == PeptideAPI.sessionId
    }
}
