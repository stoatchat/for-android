package chat.stoat.api.settings

import chat.stoat.StoatApplication
import chat.stoat.core.model.data.STOAT_BASE
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.data.STOAT_PROXY
import chat.stoat.core.model.data.STOAT_WEBSOCKET
import chat.stoat.core.model.data.STOAT_WEB_APP
import chat.stoat.persistence.KVStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logcat.LogPriority
import logcat.logcat

/**
 * Lets the app point at a self-hosted Stoat/Revolt instance instead of the instance baked
 * into [chat.stoat.core.model.data.Constants], without needing a rebuild. The instance's root
 * API endpoint is queried for its configuration (the same "instance discovery" document the
 * official web app uses) to find the real websocket/file/proxy URLs, since self-hosted setups
 * commonly place those behind different subpaths or subdomains.
 */
object InstanceManager {
    private const val KV_KEY = "customInstanceApiBase"

    private val defaultApiBase = STOAT_BASE
    private val defaultWebsocket = STOAT_WEBSOCKET
    private val defaultFiles = STOAT_FILES
    private val defaultProxy = STOAT_PROXY
    private val defaultWebApp = STOAT_WEB_APP

    private val hydrationMutex = Mutex()
    private var hydrated = false

    private val discoveryHttp by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; explicitNulls = false })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 8000
                connectTimeoutMillis = 8000
            }
        }
    }

    /** Currently active API base, for display purposes (e.g. in the custom instance dialog). */
    val activeApiBase: String
        get() = STOAT_BASE

    val isCustomInstanceActive: Boolean
        get() = STOAT_BASE != defaultApiBase

    /** Re-applies a previously saved custom instance, if any. Safe to call more than once. */
    suspend fun hydrate() {
        if (hydrated) return
        hydrationMutex.withLock {
            if (hydrated) return
            hydrated = true
            val saved = KVStorage(StoatApplication.instance).get(KV_KEY)
            if (saved.isNullOrBlank()) return
            resolveAndApply(saved, persist = false).onFailure {
                logcat(LogPriority.WARN) { "Failed to re-apply saved custom instance '$saved': ${it.message}" }
            }
        }
    }

    /**
     * Tries to resolve [rawUrl] as a Stoat/Revolt instance (as typed, and with "/api" appended)
     * and, on success, points the app at it and persists the choice. Returns the resolved host
     * on success.
     */
    suspend fun resolveAndApply(rawUrl: String, persist: Boolean = true): Result<String> {
        val normalized = normalizeUrl(rawUrl)
            ?: return Result.failure(IllegalArgumentException("Adresse invalide"))

        val candidates = listOf(normalized, "${normalized.trimEnd('/')}/api").distinct()

        for (candidate in candidates) {
            val info = tryFetch(candidate) ?: continue
            if (info.ws == null) continue

            STOAT_BASE = candidate
            STOAT_WEBSOCKET = info.ws
            info.features?.autumn?.takeIf { it.enabled && it.url != null }?.let { STOAT_FILES = it.url!! }
            info.features?.january?.takeIf { it.enabled && it.url != null }?.let { STOAT_PROXY = it.url!! }
            STOAT_WEB_APP = info.app ?: candidate

            if (persist) {
                KVStorage(StoatApplication.instance).set(KV_KEY, candidate)
            }
            hydrated = true

            return Result.success(hostOf(candidate) ?: candidate)
        }

        return Result.failure(IllegalStateException("Impossible de joindre un serveur Stoat/Revolt à cette adresse"))
    }

    /** Reverts to the instance baked into the app at build time. */
    suspend fun resetToDefault() {
        STOAT_BASE = defaultApiBase
        STOAT_WEBSOCKET = defaultWebsocket
        STOAT_FILES = defaultFiles
        STOAT_PROXY = defaultProxy
        STOAT_WEB_APP = defaultWebApp
        KVStorage(StoatApplication.instance).set(KV_KEY, "")
        hydrated = true
    }

    private suspend fun tryFetch(url: String): InstanceQueryResponse? {
        return try {
            val response = discoveryHttp.get(url)
            if (response.status.value != 200) return null
            response.body<InstanceQueryResponse>()
        } catch (e: Exception) {
            logcat(LogPriority.DEBUG) { "Instance discovery failed for '$url': ${e.message}" }
            null
        }
    }

    private fun normalizeUrl(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return withScheme.trimEnd('/')
    }

    private fun hostOf(url: String): String? = try {
        java.net.URI(url).host
    } catch (e: Exception) {
        null
    }
}

@Serializable
private data class InstanceQueryResponse(
    val ws: String? = null,
    val app: String? = null,
    val features: InstanceFeatures? = null
)

@Serializable
private data class InstanceFeatures(
    val autumn: InstanceService? = null,
    val january: InstanceService? = null
)

@Serializable
private data class InstanceService(
    val enabled: Boolean = false,
    val url: String? = null
)
