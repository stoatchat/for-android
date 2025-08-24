package chat.peptide.api.schemas

import chat.peptide.api.PeptideAPI
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
