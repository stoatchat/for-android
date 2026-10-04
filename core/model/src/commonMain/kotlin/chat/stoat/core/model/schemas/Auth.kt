package chat.stoat.core.model.schemas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Session(
    @SerialName("_id") val id: String,
    val name: String
)
