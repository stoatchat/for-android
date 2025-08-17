package chat.peptide.api

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import chat.peptide.BuildConfig
import chat.peptide.PeptideApplication
import chat.peptide.api.PeptideAPI.initialize
import chat.peptide.api.internals.Members
import chat.peptide.api.realtime.DisconnectionState
import chat.peptide.api.realtime.RealtimeSocket
import chat.peptide.api.routes.user.fetchSelf
import chat.peptide.api.schemas.AutumnResource
import chat.peptide.api.schemas.ChannelType
import chat.peptide.api.schemas.Emoji
import chat.peptide.api.schemas.Message
import chat.peptide.api.schemas.Server
import chat.peptide.api.schemas.User
import chat.peptide.api.unreads.Unreads
import chat.peptide.getKVStorage
import chat.peptide.persistence.Database
import chat.peptide.persistence.SqlStorage
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import io.sentry.Sentry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import java.net.SocketException
import chat.peptide.api.schemas.Channel as ChannelSchema

/**
 * Enum representing available platforms in the application.
 */
enum class ApplicationPlatform(val baseUrl: String) {
    REVOLT("https://api.peptide.chat"),
    PEP("https://peptide.chat/api");

    companion object {
        fun fromName(name: String): ApplicationPlatform? {
            return ApplicationPlatform.entries.find { it.name == name }
        }
    }

}

/**
 * Platform URL configuration
 */
data class PlatformUrls(
    val base: String,
    val marketing: String,
    val files: String,
    val january: String,
    val app: String,
    val invites: String,
    val websocket: String,
    val autumn: String,
    val kjbook: String
)

// Platform URL configurations
private val PLATFORM_URLS = mapOf(
    ApplicationPlatform.REVOLT to PlatformUrls(
        base = "https://api.revolt.chat/0.8",
        marketing = "https://revolt.chat",
        files = "https://cdn.revoltusercontent.com",
        january = "https://jan.revolt.chat",
        app = "https://app.revolt.chat",
        invites = "https://rvlt.gg",
        websocket = "wss://ws.revolt.chat",
        autumn = "https://autumn.revolt.chat",
        kjbook = "https://revoltchat.github.io/android"
    ),
    ApplicationPlatform.PEP to PlatformUrls(
        base = "https://pepchat.io/api",
        marketing = "https://peptide.chat",
        files = "https://cdn.pepusercontent.com",
        january = "https://pepchat.io/january",
        app = "https://peptide.chat",
        invites = "https://pep.gg",
        websocket = "wss://pepchat.io/ws",
        autumn = "https://pepchat.io/autumn",
        // TODO: Replace with correct URL
        kjbook = "https://revoltchat.github.io/android"
        // TODO: Replace with correct URL
    )
)

object UrlsStorageKeys {
    const val PLATFORM = "platform_key"
}

fun String.api(): String {
    return "${PeptideAPI.getCurrentBaseUrl()}$this"
}

/**
 * Helper functions to get URLs for a specific platform
 */
private fun getUrlForPlatform(platform: ApplicationPlatform, urlSelector: (PlatformUrls) -> String): String {
    return urlSelector(PLATFORM_URLS[platform]!!)
}

fun buildUserAgent(accessMethod: String = "Ktor"): String {
    return "$accessMethod PeptideAndroid/${BuildConfig.VERSION_NAME} " +
            "${BuildConfig.APPLICATION_ID} Android/${android.os.Build.VERSION.SDK_INT} " +
            "(${android.os.Build.MANUFACTURER} ${android.os.Build.DEVICE}) Kotlin/${KotlinVersion.CURRENT}"
}

@OptIn(ExperimentalSerializationApi::class)
val PeptideJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

@OptIn(ExperimentalSerializationApi::class)
val PeptideCbor = Cbor {
    ignoreUnknownKeys = true
}

val PeptideHttp by lazy {
    HttpClient(OkHttp) {
        install(DefaultRequest)
        install(ContentNegotiation) {
            json(PeptideJson)
        }

        install(WebSockets)

        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 5)
            retryOnException(maxRetries = 5)

            modifyRequest { request ->
                request.headers.append("x-retry-count", retryCount.toString())
            }

            exponentialDelay()
        }

        install(Logging) { level = LogLevel.INFO }

        val chuckerCollector = ChuckerCollector(
            context = PeptideApplication.instance,
            showNotification = true,
            retentionPeriod = RetentionManager.Period.ONE_DAY
        )

        val chuckerInterceptor = ChuckerInterceptor.Builder(PeptideApplication.instance)
            .collector(chuckerCollector)
            .maxContentLength(250_000L)
            .redactHeaders(PeptideAPI.TOKEN_HEADER_NAME)
            .alwaysReadResponseBody(true)
            .createShortcut(false)
            .build()

        engine {
            addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .apply {
                        if (chain.request().headers[PeptideAPI.TOKEN_HEADER_NAME] == null) {
                            header(PeptideAPI.TOKEN_HEADER_NAME, PeptideAPI.sessionToken)
                        }
                    }
                    .build()
                chain.proceed(request)
            }
            addInterceptor(chuckerInterceptor)
        }

        defaultRequest {
            url(PeptideAPI.getCurrentBaseUrl())
            header("User-Agent", buildUserAgent())
        }
    }
}

val mainHandler = Handler(Looper.getMainLooper())

object PeptideAPI {
    const val TOKEN_HEADER_NAME = "x-session-token"

    val userCache = mutableStateMapOf<String, User>()
    val serverCache = mutableStateMapOf<String, Server>()
    val channelCache = mutableStateMapOf<String, ChannelSchema>()
    val emojiCache = mutableStateMapOf<String, Emoji>()
    val messageCache = mutableStateMapOf<String, Message>()

    val members = Members()

    val unreads = Unreads()

    var selfId: String? = null

    var sessionToken: String = ""
        private set
    var sessionId: String = ""
        private set

    /**
     * The currently selected platform.
     * Default is REVOLT.
     */
    var selectedApplicationPlatform: ApplicationPlatform = ApplicationPlatform.PEP
        private set

    /**
     * Sets the current platform and saves it to persistent storage.
     * @param applicationPlatform The platform to set
     */
    fun setPlatform(applicationPlatform: ApplicationPlatform) {
        selectedApplicationPlatform = applicationPlatform
        CoroutineScope(Dispatchers.IO).launch {
            savePlatformSelection()
        }
    }

    /**
     * Saves the current platform selection to persistent storage.
     */
    private suspend fun savePlatformSelection() {
        val kvStorage = PeptideApplication.instance.getKVStorage()
        kvStorage.set(UrlsStorageKeys.PLATFORM, selectedApplicationPlatform.name)
    }

    /**
     * Loads the platform selection from persistent storage.
     */
    suspend fun loadPlatformSelection() {
        val kvStorage = PeptideApplication.instance.getKVStorage()
        val applicationPlatformName = kvStorage.get(UrlsStorageKeys.PLATFORM) ?: ApplicationPlatform.REVOLT.name
        selectedApplicationPlatform = try {
            ApplicationPlatform.valueOf(applicationPlatformName)
        } catch (_: IllegalArgumentException) {
            ApplicationPlatform.REVOLT
        }
    }

    // URL getter functions
    fun getCurrentBaseUrl(): String = getUrlForPlatform(selectedApplicationPlatform) { it.base }
    fun getCurrentMarketingUrl(): String = getUrlForPlatform(selectedApplicationPlatform) { it.marketing }
    fun getCurrentFilesUrl(): String = getUrlForPlatform(selectedApplicationPlatform) { it.autumn }
    fun getCurrentJanuaryUrl(): String = getUrlForPlatform(selectedApplicationPlatform) { it.january }
    fun getCurrentAppUrl(): String = getUrlForPlatform(selectedApplicationPlatform) { it.app }
    fun getCurrentInvitesUrl(): String = getUrlForPlatform(selectedApplicationPlatform) { it.invites }
    fun getCurrentWebSocketUrl(): String = getUrlForPlatform(selectedApplicationPlatform) { it.websocket }
    fun getCurrentAutumnUrl(): String = getUrlForPlatform(selectedApplicationPlatform) { it.autumn }
    fun getCurrentKjBookUrl(): String = getUrlForPlatform(selectedApplicationPlatform) { it.kjbook }

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    val realtimeContext = newSingleThreadContext("RealtimeContext")
    val wsFrameChannel = Channel<Any>(Channel.UNLIMITED)

    private var socketCoroutine: Job? = null

    private var openForLocalHydration = true

    fun setSessionHeader(token: String) {
        sessionToken = token
    }

    fun setSessionId(id: String) {
        sessionId = id
    }

    suspend fun loginAs(token: String) {
        setSessionHeader(token)
        fetchSelf()
        startSocketOps()
        unreads.sync()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun connectWS() {
        socketCoroutine = CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(realtimeContext) {
                    try {
                        RealtimeSocket.connect(sessionToken)
                    } catch (e: SocketException) {
                        Log.d("PeptideAPI", "Socket closed, probably no big deal /// " + e.message)
                        RealtimeSocket.updateDisconnectionState(DisconnectionState.Disconnected)
                    } catch (e: Exception) {
                        Log.e("PeptideAPI", "WebSocket error", e)
                        RealtimeSocket.updateDisconnectionState(DisconnectionState.Disconnected)
                    }
                }
            } catch (e: Exception) {
                try {
                    if (e is InterruptedException) {
                        Log.d("PeptideAPI", "Socket interrupted")
                    } else {
                        Log.e("PeptideAPI", "WebSocket error", e)
                    }
                    RealtimeSocket.updateDisconnectionState(DisconnectionState.Disconnected)
                } catch (e: Exception) {
                    Sentry.captureMessage("Error in socket error handling: $e")
                }
            }
        }
    }

    private fun startSocketOps() {
        connectWS()

        // Send a ping every roughly 30 seconds else the socket dies
        // Same interval as the web clients (/peptide.js)
        // Note: This will run even if the socket is closed (sendPing will just exit early)
        mainHandler.post(object : Runnable {
            override fun run() {
                runBlocking {
                    RealtimeSocket.sendPing()
                }
                mainHandler.postDelayed(this, 30 * 1000)
            }
        })
    }

    suspend fun initialize() {
        loadPlatformSelection()
        if (sessionToken != "") {
            fetchSelf()
        }
    }

    /**
     * Returns true if the user is logged in and the current user has been fetched at least once.
     * Call [initialize] to fetch the current user first, else this will return false.
     */
    fun isLoggedIn(): Boolean {
        return selfId != null
    }

    /**
     * Clears the API client's state completely.
     */
    fun logout() {
        selfId = null
        sessionToken = ""
        sessionId = ""

        userCache.clear()
        serverCache.clear()
        channelCache.clear()
        emojiCache.clear()
        messageCache.clear()

        members.clear()
        unreads.clear()

        socketCoroutine?.cancel()
        mainHandler.removeCallbacksAndMessages(null)

        clearPersistentCache()
    }

    /**
     * Checks if a session token is valid.
     */
    suspend fun checkSessionToken(token: String): Boolean {
        return try {
            setSessionHeader(token)
            fetchSelf()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Hydrate caches from a local database.
     */
    fun hydrateFromPersistentCache() {
        if (!openForLocalHydration) {
            Log.w("PeptideAPI", "Hydration is closed, but was called")
            // Stale data is worst case, let's track it even in prod
            Sentry.captureMessage("Local hydration called twice or after real data was fetched")
            return
        }

        val db = Database(SqlStorage.driver)

        val channels = db.channelQueries.selectAll().executeAsList().map {
            ChannelSchema(
                id = it.id,
                channelType = try {
                    ChannelType.valueOf(it.channelType)
                } catch (_: Exception) {
                    null
                },
                user = it.userId,
                name = it.name,
                owner = it.owner,
                description = it.description,
                recipients = selfId?.let { selfId ->
                    it.userId?.let { u -> listOf(u, selfId) }
                } ?: it.userId?.let { u -> listOf(u) },
                icon = AutumnResource(
                    id = it.iconId,
                ),
                server = it.server,
                lastMessageID = it.lastMessageId,
                active = it.active == 1L,
                nsfw = it.nsfw == 1L
            )
        }
        channelCache.clear()
        channelCache.putAll(channels.associateBy { it.id!! })

        val servers = db.serverQueries.selectAll().executeAsList().map {
            Server(
                id = it.id,
                owner = it.owner,
                name = it.name,
                description = it.description,
                icon = AutumnResource(
                    id = it.iconId,
                ),
                banner = AutumnResource(
                    id = it.bannerId,
                ),
                flags = it.flags,
                channels = channels
                    .filter { c -> c.server == it.id }
                    .filterNot { c -> c.id == null }
                    .map { c -> c.id!! },
            )
        }
        serverCache.clear()
        serverCache.putAll(servers.associateBy { it.id!! })

        openForLocalHydration = false
    }

    /**
     * Clear the local caching database.
     */
    private fun clearPersistentCache() {
        val db = Database(SqlStorage.driver)
        db.serverQueries.clear()
        db.channelQueries.clear()
    }

    /**
     * Marks database as hydrated (after real data was fetched, for example).
     */
    fun closeHydration() {
        openForLocalHydration = false
    }
}

@Serializable
data class PeptideError(val type: String)

@Serializable
data class RateLimitResponse(@SerialName("retry_after") val retryAfter: Int) {
    fun toException(): HitRateLimitException {
        return HitRateLimitException(retryAfter)
    }
}

internal const val NO_RETRY_AFTER = Int.MIN_VALUE

class HitRateLimitException(retryAfter: Int = NO_RETRY_AFTER) :
    Exception(if (retryAfter == NO_RETRY_AFTER) "Hit rate limit" else "Hit rate limit, retry after ${retryAfter}ms")