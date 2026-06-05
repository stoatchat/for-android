package chat.zekochat.api.routes.googlesheets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models for the public servers API: GET https://manageapi.peptide.chat/api/directory/servers
 */
@Serializable
data class ServerEntry(
    val id: String,
    val name: String = "",
    val description: String = "",
    val inviteCode: String? = null,
    val disabled: Boolean = false,
    @SerialName("new") val isNew: Boolean = false,
    @SerialName("showcolor") val showcolor: String? = null,
    @SerialName("sortorder") val sortorder: Int? = null,
)

@Serializable
data class ServersResponse(
    val success: Boolean,
    val data: List<ServerEntry> = emptyList(),
)
