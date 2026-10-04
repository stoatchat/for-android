package chat.stoat.core.model.schemas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthNotice(
    val version: String? = null,
    val alert: Alert? = null
)

@Serializable
data class Alert(
    val text: String? = null,
    @SerialName("dismissable")
    val dismissible: Boolean? = null,
    val actions: List<Action>? = null
)

@Serializable
data class Action(
    val text: String? = null,
    val type: String? = null,
    val href: String? = null
)
