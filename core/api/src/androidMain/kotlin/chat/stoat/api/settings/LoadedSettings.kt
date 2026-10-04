package chat.stoat.api.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.stoat.api.StoatAPIHost
import chat.stoat.core.model.schemas.AndroidSpecificSettingsSpecialEmbedSettings

enum class Theme {
    None,
    Default,
    Light,
    M3Dynamic,
    Amoled
}

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
    var theme by mutableStateOf(StoatAPIHost.platform.defaultTheme)
    var messageReplyStyle by mutableStateOf(MessageReplyStyle.SwipeFromEnd)
    var avatarRadius by mutableIntStateOf(50)
    var messageComposerBlurEnabled by mutableStateOf(true)
    var experimentsEnabled by mutableStateOf(false)
    var specialEmbedSettings by mutableStateOf(SpecialEmbedSettings())
    var poorlyFormedSettingsKeys by mutableStateOf(emptySet<String>())
    var font by mutableStateOf(StoatAPIHost.platform.defaultFont)

    fun hydrateWithSettings(settings: SyncedSettings) {
        this.theme = settings.android.theme?.let {
            if (it == "Revolt") Theme.Default else Theme.valueOf(it)
        } ?: StoatAPIHost.platform.defaultTheme
        this.messageReplyStyle =
            settings.android.messageReplyStyle?.let { MessageReplyStyle.valueOf(it) }
                ?: MessageReplyStyle.SwipeFromEnd
        this.avatarRadius = settings.android.avatarRadius ?: 50
        this.messageComposerBlurEnabled = settings.android.messageComposerBlurEnabled ?: true
        this.specialEmbedSettings = settings.android.specialEmbedSettings ?: SpecialEmbedSettings()
        this.font = settings.android.font?.let {
            try {
                UserInterfaceFont.valueOf(it)
            } catch (e: Exception) {
                null
            }
        } ?: StoatAPIHost.platform.defaultFont
    }

    fun reset() {
        theme = StoatAPIHost.platform.defaultTheme
        messageReplyStyle = MessageReplyStyle.SwipeFromEnd
        avatarRadius = 50
        messageComposerBlurEnabled = true
        specialEmbedSettings = SpecialEmbedSettings()
        poorlyFormedSettingsKeys = emptySet()
        font = StoatAPIHost.platform.defaultFont
    }
}
