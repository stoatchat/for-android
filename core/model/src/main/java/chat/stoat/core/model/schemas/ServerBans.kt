package chat.stoat.core.model.schemas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerBan(
    @SerialName("_id")
    val id: ServerUserChoice,
    val reason: String? = null,
)

@Serializable
data class BannedUser(
    @SerialName("_id")
    val id: String,
    val username: String,
    val discriminator: String,
    val avatar: AutumnResource? = null,
)

@Serializable
data class BanListResult(
    val users: List<BannedUser>,
    val bans: List<ServerBan>,
)
