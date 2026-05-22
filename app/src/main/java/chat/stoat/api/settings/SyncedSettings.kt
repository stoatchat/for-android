package chat.stoat.api.settings

import androidx.compose.runtime.mutableStateOf
import chat.stoat.api.StoatAPI
import chat.stoat.api.StoatJson
import chat.stoat.api.routes.sync.getKeys
import chat.stoat.api.routes.sync.setKey
import chat.stoat.core.model.schemas.AndroidSpecificSettings
import chat.stoat.core.model.schemas.NotificationSettings
import chat.stoat.core.model.schemas.OrderingSettings
import chat.stoat.core.model.schemas.ReleaseNotesSettings
import chat.stoat.core.model.schemas._NotificationSettingsToParse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import logcat.LogPriority
import logcat.asLog
import logcat.logcat

/*
 * - Note: When adding a new key -
 *  1. Add corresponding methods and fields here
 *  2. Add strings for poorly formed keys hint
 *  3. Add UI for resetting the key if it's poorly formed
 */

object SyncedSettings {
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

    val ordering: OrderingSettings
        get() = _ordering.value
    val android: AndroidSpecificSettings
        get() = _android.value
    val notifications: NotificationSettings
        get() = _notifications.value
    val releaseNotes: ReleaseNotesSettings
        get() = _releaseNotes.value

    suspend fun fetch(apiToken: String = StoatAPI.sessionToken) {
        try {
            val settings =
                getKeys("ordering", "android", "notifications", "release-notes", token = apiToken)

            settings["ordering"]?.let {
                try {
                    _ordering.value = StoatJson.decodeFromString(
                        OrderingSettings.serializer(),
                        it.value
                    )
                } catch (e: Exception) {
                    LoadedSettings.poorlyFormedSettingsKeys += "ordering"
                    e.printStackTrace()
                }
            }

            settings["android"]?.let {
                try {
                    _android.value = StoatJson.decodeFromString(
                        AndroidSpecificSettings.serializer(),
                        it.value
                    )
                } catch (e: Exception) {
                    LoadedSettings.poorlyFormedSettingsKeys += "android"
                    e.printStackTrace()
                }
            }

            settings["notifications"]?.let {
                // This is to fix a quirk where the web client sometimes leaves sub-objects in one of the objects
                // Because it is written in typescript and does what it wants
                _notifications.value = parseNotificationSettings(it.value)
            }

            settings["release-notes"]?.let {
                try {
                    _releaseNotes.value = StoatJson.decodeFromString(
                        ReleaseNotesSettings.serializer(),
                        it.value
                    )
                } catch (e: Exception) {
                    LoadedSettings.poorlyFormedSettingsKeys += "release-notes"
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _fetchCompleted.complete(Unit)
        }
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
        setKey("ordering", StoatJson.encodeToString(OrderingSettings.serializer(), value))
    }

    suspend fun updateAndroid(value: AndroidSpecificSettings) {
        _android.value = value
        setKey("android", StoatJson.encodeToString(AndroidSpecificSettings.serializer(), value))
    }

    suspend fun updateNotifications(value: NotificationSettings) {
        _notifications.value = value
        setKey("notifications", StoatJson.encodeToString(NotificationSettings.serializer(), value))
    }

    suspend fun updateReleaseNotes(value: ReleaseNotesSettings) {
        _releaseNotes.value = value
        setKey("release-notes", StoatJson.encodeToString(ReleaseNotesSettings.serializer(), value))
    }

    suspend fun resetOrdering() {
        val default = OrderingSettings()
        _ordering.value = default
        setKey("ordering", StoatJson.encodeToString(OrderingSettings.serializer(), default))
    }

    suspend fun resetAndroid() {
        val default = AndroidSpecificSettings(
            theme = "None",
            font = "Default",
            colourOverrides = null,
            messageReplyStyle = "None"
        )
        _android.value = default
        setKey("android", StoatJson.encodeToString(AndroidSpecificSettings.serializer(), default))
    }

    suspend fun resetNotifications() {
        val default = NotificationSettings()
        _notifications.value = default
        setKey(
            "notifications",
            StoatJson.encodeToString(NotificationSettings.serializer(), default)
        )
    }

    suspend fun resetReleaseNotes() {
        val default = ReleaseNotesSettings()
        _releaseNotes.value = default
        setKey(
            "release-notes",
            StoatJson.encodeToString(ReleaseNotesSettings.serializer(), default)
        )
    }
}
