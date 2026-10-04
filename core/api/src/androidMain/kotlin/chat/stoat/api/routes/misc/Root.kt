package chat.stoat.api.routes.misc

import chat.stoat.api.StoatHttp
import chat.stoat.api.api
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Root(
    val revolt: String,
    val features: Features,
    val ws: String,
    val app: String,
    val vapid: String
)

@Serializable
data class Features(
    val captcha: CAPTCHAFeature,
    val email: Boolean,
    @SerialName("invite_only") val inviteOnly: Boolean,
    val autumn: AutumnJanuaryFeature,
    val january: AutumnJanuaryFeature,
    val voso: LegacyVoiceFeature? = null,
    val livekit: LiveKitFeature? = null,
    val limits: LimitsConfig? = null,
)

@Serializable
data class LimitsConfig(
    val global: GlobalLimits,
    @SerialName("new_user") val newUser: UserLimits? = null,
    @SerialName("default") val defaultUser: UserLimits? = null,
)

@Serializable
data class GlobalLimits(
    @SerialName("server_emoji") val serverEmoji: Int,
)

@Serializable
data class UserLimits(
    @SerialName("file_upload_size_limits")
    val fileUploadSizeLimits: Map<String, Long> = emptyMap(),
)

@Serializable
data class AutumnJanuaryFeature(
    val enabled: Boolean,
    val url: String
)

@Serializable
data class CAPTCHAFeature(
    val enabled: Boolean,
    val key: String
)

@Serializable
data class LegacyVoiceFeature(
    val enabled: Boolean,
    val url: String,
    val ws: String
)

@Serializable
data class LiveKitFeature(
    val enabled: Boolean,
    val nodes: List<LiveKitNode>
)

@Serializable
data class LiveKitNode(
    val name: String,
    val lat: Double,
    val lon: Double,
    @SerialName("public_url") val publicUrl: String,
)

suspend fun getRootRoute(): Root {
    return StoatHttp.get("/".api()).body()
}
