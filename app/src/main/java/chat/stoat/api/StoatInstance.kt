package chat.stoat.api

import chat.stoat.persistence.KVStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object StoatInstance {
    const val KV_INSTANCE_API_BASE = "instanceApiBase"

    private const val DEFAULT_API_BASE = "https://api.stoat.chat/0.8"
    private const val DEFAULT_FILES_BASE = "https://cdn.stoatusercontent.com"
    private const val DEFAULT_PROXY_BASE = "https://proxy.stoatusercontent.com"
    private const val DEFAULT_WEB_APP = "https://stoat.chat"
    private const val DEFAULT_INVITES = "https://stt.gg"
    private const val DEFAULT_WEBSOCKET = "wss://events.stoat.chat"

    var apiBase: String = DEFAULT_API_BASE
        private set
    var filesBase: String = DEFAULT_FILES_BASE
        private set
    var proxyBase: String = DEFAULT_PROXY_BASE
        private set
    var webApp: String = DEFAULT_WEB_APP
        private set
    var invites: String = DEFAULT_INVITES
        private set
    var websocket: String = DEFAULT_WEBSOCKET
        private set

    suspend fun restore(kvStorage: KVStorage) {
        configure(kvStorage.get(KV_INSTANCE_API_BASE) ?: DEFAULT_API_BASE)
    }

    suspend fun configure(rawApiBase: String, kvStorage: KVStorage? = null) {
        val normalizedApiBase = normalizeApiBase(rawApiBase)
        applyFallbacks(normalizedApiBase)
        discoverInstanceMetadata(normalizedApiBase)
        kvStorage?.set(KV_INSTANCE_API_BASE, normalizedApiBase)
    }

    fun normalizeApiBase(rawApiBase: String): String {
        val trimmed = rawApiBase.trim().ifBlank { DEFAULT_API_BASE }
        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        return withScheme.trimEnd('/')
    }

    private fun applyFallbacks(normalizedApiBase: String) {
        apiBase = normalizedApiBase
        if (normalizedApiBase == DEFAULT_API_BASE) {
            filesBase = DEFAULT_FILES_BASE
            proxyBase = DEFAULT_PROXY_BASE
            webApp = DEFAULT_WEB_APP
            invites = DEFAULT_INVITES
            websocket = DEFAULT_WEBSOCKET
            return
        }

        val serviceRoot = normalizedApiBase.removeSuffix("/0.8").removeSuffix("/api")
        filesBase = "$serviceRoot/autumn"
        proxyBase = "$serviceRoot/january"
        webApp = serviceRoot
        invites = serviceRoot
        websocket = serviceRoot
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://") + "/ws"
    }

    private suspend fun discoverInstanceMetadata(normalizedApiBase: String) {
        val client = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(StoatJson)
            }
        }

        try {
            val metadata = client.get(normalizedApiBase).body<InstanceMetadata>()
            metadata.ws?.trimEnd('/')?.let { websocket = it }
            metadata.revolt?.trimEnd('/')?.let { webApp = it }
            metadata.features?.autumn?.url?.trimEnd('/')?.let { filesBase = it }
            metadata.features?.january?.url?.trimEnd('/')?.let { proxyBase = it }
        } catch (_: Exception) {
            // Keep derived fallbacks. Login requests will surface connection/auth errors to the user.
        } finally {
            client.close()
        }
    }
}

@Serializable
private data class InstanceMetadata(
    val revolt: String? = null,
    val ws: String? = null,
    val features: InstanceFeatures? = null
)

@Serializable
private data class InstanceFeatures(
    val autumn: InstanceFeatureEndpoint? = null,
    val january: InstanceFeatureEndpoint? = null
)

@Serializable
private data class InstanceFeatureEndpoint(
    val enabled: Boolean = false,
    @SerialName("url")
    val url: String? = null
)
