package chat.stoat.api

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import chat.stoat.BuildConfig
import chat.stoat.StoatApplication
import chat.stoat.api.StoatAPI.initialize
import chat.stoat.api.internals.ActiveSlowmode
import chat.stoat.api.internals.Members
import chat.stoat.api.realtime.DisconnectionState
import chat.stoat.api.realtime.RealtimeSocket
import chat.stoat.api.routes.account.MFA_TICKET_HEADER_NAME
import chat.stoat.api.routes.user.fetchSelf
import chat.stoat.api.unreads.Unreads
import chat.stoat.core.model.data.STOAT_BASE
import chat.stoat.core.model.schemas.AutumnResource
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.core.model.schemas.Emoji
import chat.stoat.core.model.schemas.Message
import chat.stoat.core.model.schemas.Server
import chat.stoat.core.model.schemas.User
import chat.stoat.core.model.util.ChannelVoiceState
import chat.stoat.persistence.Database
import chat.stoat.persistence.SqlStorage
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import java.net.SocketException
import kotlin.time.Duration.Companion.seconds
import chat.stoat.core.model.schemas.Channel as ChannelSchema

fun String.api(): String {
    return "$STOAT_BASE$this"
}

fun buildUserAgent(accessMethod: String = "Ktor"): String {
    return "$accessMethod StoatForAndroid/${BuildConfig.VERSION_NAME} " +
            "${BuildConfig.APPLICATION_ID} Android/${android.os.Build.VERSION.SDK_INT} " +
            "(${android.os.Build.MANUFACTURER} ${android.os.Build.DEVICE}) Kotlin/${KotlinVersion.CURRENT}"
}

@OptIn(ExperimentalSerializationApi::class)
val StoatJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

@OptIn(ExperimentalSerializationApi::class)
val StoatCbor = Cbor {
    ignoreUnknownKeys = true
}

val StoatHttp = HttpClient(OkHttp) {
    install(DefaultRequest)
    install(ContentNegotiation) {
        json(StoatJson)
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
        context = StoatApplication.instance,
        showNotification = true,
        retentionPeriod = RetentionManager.Period.ONE_DAY
    )

    val chuckerInterceptor = ChuckerInterceptor.Builder(StoatApplication.instance)
        .collector(chuckerCollector)
        .maxContentLength(250_000L)
        .redactHeaders(StoatAPI.TOKEN_HEADER_NAME, MFA_TICKET_HEADER_NAME)
        .alwaysReadResponseBody(true)
        .createShortcut(false)
        .build()

    engine {
        addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .apply {
                    if (chain.request().headers[StoatAPI.TOKEN_HEADER_NAME] == null) {
                        header(StoatAPI.TOKEN_HEADER_NAME, StoatAPI.sessionToken)
                    }
                }
                .build()
            chain.proceed(request)
        }
        addInterceptor(chuckerInterceptor)
    }

    defaultRequest {
        url(STOAT_BASE)
        header("User-Agent", buildUserAgent())
    }
}

object StoatAPI {
    const val TOKEN_HEADER_NAME = "x-session-token"
    private const val WS_EVENT_BUFFER_CAPACITY =
        128 // arbitrary -- should be adjusted if too much gets dropped...
    private val INITIAL_RECONNECT_DELAY = 1.seconds
    private val MAX_RECONNECT_DELAY = 30.seconds
    private val PING_INTERVAL = 30.seconds // Same interval as the web clients (/revolt.js)

    val userCache = mutableStateMapOf<String, User>()
    val serverCache = mutableStateMapOf<String, Server>()
    val channelCache = mutableStateMapOf<String, ChannelSchema>()
    val emojiCache = mutableStateMapOf<String, Emoji>()
    val messageCache = mutableStateMapOf<String, Message>()
    val voiceStateCache = mutableStateMapOf<String, ChannelVoiceState>()
    val userSlowmodeCache = mutableStateMapOf<String, ActiveSlowmode>()

    val members = Members()

    val unreads = Unreads()

    var selfId: String? = null

    var sessionToken: String = ""
        private set
    var sessionId: String = ""
        private set

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    val realtimeContext = newSingleThreadContext("RealtimeContext")
    val wsFrameChannel = MutableSharedFlow<Any>(
        replay = 0,
        extraBufferCapacity = WS_EVENT_BUFFER_CAPACITY,
    )

    private var socketCoroutine: Job? = null
    private var pingCoroutine: Job? = null

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
    suspend fun connectWS() {
        socketCoroutine?.cancelAndJoin()
        RealtimeSocket.updateDisconnectionState(DisconnectionState.Reconnecting)
        val token = sessionToken
        socketCoroutine = CoroutineScope(Dispatchers.IO).launch {
            var reconnectDelay = INITIAL_RECONNECT_DELAY
            while (isActive && sessionToken == token) {
                try {
                    withContext(realtimeContext) {
                        RealtimeSocket.connect(token)
                    }
                    reconnectDelay = INITIAL_RECONNECT_DELAY
                } catch (e: CancellationException) {
                    throw e
                } catch (e: SocketException) {
                    logcat { "WebSocket closed: ${e.message}" }
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR) { "WebSocket error:\n${e.asLog()}" }
                }

                if (!isActive || sessionToken != token) break

                try {
                    RealtimeSocket.updateDisconnectionState(DisconnectionState.Reconnecting)
                    delay(reconnectDelay)
                    reconnectDelay =
                        (reconnectDelay * 2).coerceAtMost(MAX_RECONNECT_DELAY)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    RealtimeSocket.updateDisconnectionState(DisconnectionState.Disconnected)
                    Sentry.captureMessage("Error in socket error handling: $e")
                }
            }
        }
    }

    private suspend fun startSocketOps() {
        connectWS()

        // Send a ping every roughly PING_INTERVAL else the socket dies
        pingCoroutine?.cancel()
        pingCoroutine = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(PING_INTERVAL)
                try {
                    RealtimeSocket.sendPing()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR) { "Failed to ping WebSocket:\n${e.asLog()}" }
                }
            }
        }
    }

    suspend fun initialize() {
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
        userSlowmodeCache.clear()

        members.clear()
        unreads.clear()

        socketCoroutine?.cancel()
        pingCoroutine?.cancel()

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
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Hydrate caches from a local database.
     */
    fun hydrateFromPersistentCache() {
        if (!openForLocalHydration) {
            Log.w("RevoltAPI", "Hydration is closed, but was called")
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
                } catch (e: Exception) {
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
data class StoatAPIError(val type: String)

@Serializable
data class RateLimitResponse(@SerialName("retry_after") val retryAfter: Int) {
    fun toException(): HitRateLimitException {
        return HitRateLimitException(retryAfter)
    }
}

internal const val NO_RETRY_AFTER = Int.MIN_VALUE

class HitRateLimitException(retryAfter: Int = NO_RETRY_AFTER) :
    Exception(if (retryAfter == NO_RETRY_AFTER) "Hit rate limit" else "Hit rate limit, retry after ${retryAfter}ms")
