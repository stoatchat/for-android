package chat.stoat.api.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.stoat.core.model.schemas.AndroidSpecificSettingsSpecialEmbedSettings
import chat.stoat.ui.theme.Theme
import chat.stoat.ui.theme.getDefaultFont
import chat.stoat.ui.theme.getDefaultTheme

enum class MessageReplyStyle {
    None,
    SwipeFromEnd,
    DoubleTap
}

enum class UserInterfaceFont {
    Default,
    GoogleSansFlex,
}

typealias SpecialEmbedSettings = AndroidSpecificSettingsSpecialEmbedSettings

object LoadedSettings {
    var theme by mutableStateOf(getDefaultTheme())
    var messageReplyStyle by mutableStateOf(MessageReplyStyle.SwipeFromEnd)
    var avatarRadius by mutableIntStateOf(50)
    var experimentsEnabled by mutableStateOf(false)
    var specialEmbedSettings by mutableStateOf(SpecialEmbedSettings())
    var poorlyFormedSettingsKeys by mutableStateOf(emptySet<String>())
    var font by mutableStateOf(getDefaultFont())

    fun hydrateWithSettings(settings: SyncedSettings) {
        this.theme = settings.android.theme?.let {
            if (it == "Revolt") Theme.Default else Theme.valueOf(it)
        } ?: getDefaultTheme()
        this.messageReplyStyle =
            settings.android.messageReplyStyle?.let { MessageReplyStyle.valueOf(it) }
                ?: MessageReplyStyle.SwipeFromEnd
        this.avatarRadius = settings.android.avatarRadius ?: 50
        this.specialEmbedSettings = settings.android.specialEmbedSettings ?: SpecialEmbedSettings()
        this.font = settings.android.font?.let {
            try {
                UserInterfaceFont.valueOf(it)
            } catch (e: Exception) {
                null
            }
        } ?: getDefaultFont()
    }

    fun reset() {
        theme = getDefaultTheme()
        messageReplyStyle = MessageReplyStyle.SwipeFromEnd
        avatarRadius = 50
        specialEmbedSettings = SpecialEmbedSettings()
        poorlyFormedSettingsKeys = emptySet()
        font = getDefaultFont()
    }
}
