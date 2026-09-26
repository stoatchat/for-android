package chat.stoat.api.settings

import androidx.compose.runtime.mutableStateOf
import chat.stoat.api.StoatAPI
import chat.stoat.api.StoatJson
import chat.stoat.api.routes.sync.SyncedSetting
import chat.stoat.api.routes.sync.getKeys
import chat.stoat.api.routes.sync.setKey
import chat.stoat.core.model.schemas.AndroidSpecificSettings
import chat.stoat.core.model.schemas.NotificationSettings
import chat.stoat.core.model.schemas.OrderingSettings
import chat.stoat.core.model.schemas.ReleaseNotesSettings
import chat.stoat.core.model.schemas.ServerFoldersSettings
import chat.stoat.core.model.schemas._NotificationSettingsToParse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import java.util.concurrent.ConcurrentHashMap

/*
 * - Note: When adding a new key -
 *  1. Add corresponding methods and fields here
 *  2. Add strings for poorly formed keys hint
 *  3. Add UI for resetting the key if it's poorly formed
 */

object SyncedSettings {
    private val KEYS =
        arrayOf("ordering", "android", "notifications", "release-notes", "server-folders")

    private val _fetchCompleted = CompletableDeferred<Unit>()

    suspend fun awaitFetched() = _fetchCompleted.await()

    private val _ordering = mutableStateOf(OrderingSettings())
    private val _android = mutableStateOf(
        AndroidSpecificSettings(
            theme = "None",
            font = "Default",
            colourOverrides = null,
            messageReplyStyle = "None"
        )
    )
    private val _notifications = mutableStateOf(NotificationSettings())
    private val _releaseNotes = mutableStateOf(ReleaseNotesSettings())
    private val _serverFolders = mutableStateOf(ServerFoldersSettings())
    private val revisions = ConcurrentHashMap<String, Long>()
    private val writeLock = Mutex()

    val ordering: OrderingSettings
        get() = _ordering.value
    val android: AndroidSpecificSettings
        get() = _android.value
    val notifications: NotificationSettings
        get() = _notifications.value
    val releaseNotes: ReleaseNotesSettings
        get() = _releaseNotes.value
    val serverFolders: ServerFoldersSettings
        get() = _serverFolders.value

    suspend fun fetch(apiToken: String = StoatAPI.sessionToken) {
        try {
            getKeys(*KEYS, token = apiToken).forEach { (key, setting) ->
                revisions[key] = setting.timestamp
                apply(key, setting.value)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _fetchCompleted.complete(Unit)
        }
    }

    fun applyRemoteUpdate(update: Map<String, SyncedSetting>) {
        update.forEach { (key, setting) ->
            if (key !in KEYS || setting.timestamp <= (revisions[key] ?: 0L)) return@forEach
            revisions[key] = setting.timestamp
            apply(key, setting.value)
            if (key == "android") LoadedSettings.hydrateWithSettings(this)
        }
    }

    private fun apply(key: String, value: String) {
        when (key) {
            "ordering" -> parseOrLogPoorlyFormed(key) {
                _ordering.value = cleanOrdering(StoatJson.parseToJsonElement(value))
            }

            "android" -> parseOrLogPoorlyFormed(key) {
                _android.value = StoatJson.decodeFromString(
                    AndroidSpecificSettings.serializer(),
                    value
                )
            }

            // This is to fix a quirk where the web client sometimes leaves sub-objects in one of the objects
            // Because it is written in typescript and does what it wants
            "notifications" -> _notifications.value = parseNotificationSettings(value)

            "release-notes" -> parseOrLogPoorlyFormed(key) {
                _releaseNotes.value = StoatJson.decodeFromString(
                    ReleaseNotesSettings.serializer(),
                    value
                )
            }

            "server-folders" -> parseOrLogPoorlyFormed(key) {
                _serverFolders.value = cleanServerFolders(StoatJson.parseToJsonElement(value))
            }
        }
    }

    private inline fun parseOrLogPoorlyFormed(key: String, parse: () -> Unit) {
        try {
            parse()
        } catch (e: Exception) {
            LoadedSettings.poorlyFormedSettingsKeys += key
            e.printStackTrace()
        }
    }

    private suspend fun write(key: String, value: String) {
        val timestamp = System.currentTimeMillis()
        revisions[key] = timestamp
        // mutex is FIFO === writes reach the server in the order their timestamps were taken
        writeLock.withLock { setKey(key, value, timestamp) }
    }

    private fun parseNotificationSettings(value: String): NotificationSettings {
        return try {
            var intermediate =
                StoatJson.decodeFromString(_NotificationSettingsToParse.serializer(), value)

            // Throw out any value of intermediate.server and .channel that isn't a string
            intermediate = intermediate.copy(
                server = intermediate.server.filterValues { it != null }
                    .filterValues { it is JsonPrimitive }
                    .filterValues { it!!.jsonPrimitive.isString },
                channel = intermediate.channel.filterValues { it != null }
                    .filterValues { it is JsonPrimitive }
                    .filterValues { it!!.jsonPrimitive.isString }
            )

            // Convert the intermediate to a NotificationSettings
            NotificationSettings(
                server = intermediate.server.mapValues { it.value!!.jsonPrimitive.content },
                channel = intermediate.channel.mapValues { it.value!!.jsonPrimitive.content }
            )
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { e.asLog() }
            LoadedSettings.poorlyFormedSettingsKeys += "notifications"
            NotificationSettings()
        }
    }

    suspend fun updateOrdering(value: OrderingSettings) {
        _ordering.value = value
        write("ordering", StoatJson.encodeToString(OrderingSettings.serializer(), value))
    }

    suspend fun updateAndroid(value: AndroidSpecificSettings) {
        _android.value = value
        write("android", StoatJson.encodeToString(AndroidSpecificSettings.serializer(), value))
    }

    suspend fun updateNotifications(value: NotificationSettings) {
        _notifications.value = value
        write("notifications", StoatJson.encodeToString(NotificationSettings.serializer(), value))
    }

    suspend fun updateReleaseNotes(value: ReleaseNotesSettings) {
        _releaseNotes.value = value
        write("release-notes", StoatJson.encodeToString(ReleaseNotesSettings.serializer(), value))
    }

    suspend fun updateServerFolders(value: ServerFoldersSettings) {
        _serverFolders.value = value
        write("server-folders", StoatJson.encodeToString(ServerFoldersSettings.serializer(), value))
    }

    suspend fun resetServerFolders() {
        val default = ServerFoldersSettings()
        _serverFolders.value = default
        write(
            "server-folders",
            StoatJson.encodeToString(ServerFoldersSettings.serializer(), default)
        )
    }

    suspend fun resetOrdering() {
        val default = OrderingSettings()
        _ordering.value = default
        write("ordering", StoatJson.encodeToString(OrderingSettings.serializer(), default))
    }

    suspend fun resetAndroid() {
        val default = AndroidSpecificSettings(
            theme = "None",
            font = "Default",
            colourOverrides = null,
            messageReplyStyle = "None"
        )
        _android.value = default
        write("android", StoatJson.encodeToString(AndroidSpecificSettings.serializer(), default))
    }

    suspend fun resetNotifications() {
        val default = NotificationSettings()
        _notifications.value = default
        write(
            "notifications",
            StoatJson.encodeToString(NotificationSettings.serializer(), default)
        )
    }

    suspend fun resetReleaseNotes() {
        val default = ReleaseNotesSettings()
        _releaseNotes.value = default
        write(
            "release-notes",
            StoatJson.encodeToString(ReleaseNotesSettings.serializer(), default)
        )
    }
}
