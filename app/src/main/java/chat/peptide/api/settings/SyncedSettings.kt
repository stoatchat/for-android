package chat.peptide.api.settings

import androidx.compose.runtime.mutableStateOf
import chat.peptide.api.PeptideAPI
import chat.peptide.api.PeptideJson
import chat.peptide.api.routes.sync.getKeys
import chat.peptide.api.routes.sync.setKey
import chat.peptide.api.schemas.AndroidSpecificSettings
import chat.peptide.api.schemas.NotificationSettings
import chat.peptide.api.schemas.OrderingSettings
import chat.peptide.api.schemas._NotificationSettingsToParse
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
    private val _ordering = mutableStateOf(OrderingSettings())
    private val _android = mutableStateOf(
        AndroidSpecificSettings(
            theme = "None",
            colourOverrides = null,
            messageReplyStyle = "None"
        )
    )
    private val _notifications = mutableStateOf(NotificationSettings())

    val ordering: OrderingSettings
        get() = _ordering.value
    val android: AndroidSpecificSettings
        get() = _android.value
    val notifications: NotificationSettings
        get() = _notifications.value

    suspend fun fetch(peptideToken: String = PeptideAPI.sessionToken) {
        try {
            val settings =
                getKeys("ordering", "android", "notifications", peptideToken = peptideToken)

            settings["ordering"]?.let {
                try {
                    _ordering.value = PeptideJson.decodeFromString(
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
                    _android.value = PeptideJson.decodeFromString(
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseNotificationSettings(value: String): NotificationSettings {
        return try {
            var intermediate =
                PeptideJson.decodeFromString(_NotificationSettingsToParse.serializer(), value)

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
        setKey("ordering", PeptideJson.encodeToString(OrderingSettings.serializer(), value))
    }

    suspend fun updateAndroid(value: AndroidSpecificSettings) {
        _android.value = value
        setKey("android", PeptideJson.encodeToString(AndroidSpecificSettings.serializer(), value))
    }

    suspend fun updateNotifications(value: NotificationSettings) {
        _notifications.value = value
        setKey("notifications", PeptideJson.encodeToString(NotificationSettings.serializer(), value))
    }

    suspend fun resetOrdering() {
        val default = OrderingSettings()
        _ordering.value = default
        setKey("ordering", PeptideJson.encodeToString(OrderingSettings.serializer(), default))
    }

    suspend fun resetAndroid() {
        val default = AndroidSpecificSettings(
            theme = "None",
            colourOverrides = null,
            messageReplyStyle = "None"
        )
        _android.value = default
        setKey("android", PeptideJson.encodeToString(AndroidSpecificSettings.serializer(), default))
    }

    suspend fun resetNotifications() {
        val default = NotificationSettings()
        _notifications.value = default
        setKey(
            "notifications",
            PeptideJson.encodeToString(NotificationSettings.serializer(), default)
        )
    }
}
