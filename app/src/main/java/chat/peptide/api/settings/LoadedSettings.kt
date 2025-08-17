package chat.peptide.api.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.peptide.api.schemas.AndroidSpecificSettingsSpecialEmbedSettings
import chat.peptide.ui.theme.Theme
import chat.peptide.ui.theme.getDefaultTheme

enum class MessageReplyStyle {
    None,
    SwipeFromEnd,
    DoubleTap
}

typealias SpecialEmbedSettings = AndroidSpecificSettingsSpecialEmbedSettings

object LoadedSettings {
    var theme by mutableStateOf(getDefaultTheme())
    var messageReplyStyle by mutableStateOf(MessageReplyStyle.SwipeFromEnd)
    var avatarRadius by mutableIntStateOf(50)
    var experimentsEnabled by mutableStateOf(false)
    var specialEmbedSettings by mutableStateOf(SpecialEmbedSettings())
    var poorlyFormedSettingsKeys by mutableStateOf(emptySet<String>())

    fun hydrateWithSettings(settings: SyncedSettings) {
        try {
            this.theme = settings.android.theme?.let { Theme.valueOf(it) } ?: getDefaultTheme()
            this.messageReplyStyle =
                settings.android.messageReplyStyle?.let { MessageReplyStyle.valueOf(it) }
                    ?: MessageReplyStyle.SwipeFromEnd
            this.avatarRadius = settings.android.avatarRadius ?: 50
            this.specialEmbedSettings = settings.android.specialEmbedSettings ?: SpecialEmbedSettings()
        } catch (_: Exception) {
            reset()
        }
    }

    fun reset() {
        theme = getDefaultTheme()
        messageReplyStyle = MessageReplyStyle.SwipeFromEnd
        avatarRadius = 50
        specialEmbedSettings = SpecialEmbedSettings()
        poorlyFormedSettingsKeys = emptySet()
    }
}
