package chat.stoat.api.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.stoat.api.StoatAPIHost
import chat.stoat.core.model.schemas.AndroidSpecificSettingsSpecialEmbedSettings
import chat.stoat.core.model.settings.Theme
import chat.stoat.core.model.settings.UserInterfaceFont

enum class MessageReplyStyle {
    None,
    SwipeFromEnd,
    DoubleTap
}

typealias SpecialEmbedSettings = AndroidSpecificSettingsSpecialEmbedSettings

object LoadedSettings {
    var darkTheme by mutableStateOf(StoatAPIHost.platform.defaultTheme)
    var lightTheme by mutableStateOf(StoatAPIHost.platform.defaultTheme)

    /** The theme for the system's current light/dark setting. */
    fun themeFor(systemIsDark: Boolean): Theme = if (systemIsDark) darkTheme else lightTheme
    var messageReplyStyle by mutableStateOf(MessageReplyStyle.SwipeFromEnd)
    var avatarRadius by mutableIntStateOf(50)
    var messageComposerBlurEnabled by mutableStateOf(true)
    var experimentsEnabled by mutableStateOf(false)
    var specialEmbedSettings by mutableStateOf(SpecialEmbedSettings())
    var poorlyFormedSettingsKeys by mutableStateOf(emptySet<String>())
    var font by mutableStateOf(StoatAPIHost.platform.defaultFont)

    fun hydrateWithSettings(settings: SyncedSettings) {
        // Settings from before per-mode themes only have `theme`, which applies to both modes;
        // its "System" value meant the default dark theme in dark mode and Light in light mode
        val legacy = settings.android.theme?.let(::parseTheme)
        this.darkTheme = settings.android.themeDark?.let(::parseTheme)
            ?: legacy?.let { if (it == Theme.None) Theme.Default else it }
            ?: StoatAPIHost.platform.defaultTheme
        this.lightTheme = settings.android.themeLight?.let(::parseTheme)
            ?: legacy?.let { if (it == Theme.None) Theme.Light else it }
            ?: StoatAPIHost.platform.defaultTheme
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
        darkTheme = StoatAPIHost.platform.defaultTheme
        lightTheme = StoatAPIHost.platform.defaultTheme
        messageReplyStyle = MessageReplyStyle.SwipeFromEnd
        avatarRadius = 50
        messageComposerBlurEnabled = true
        specialEmbedSettings = SpecialEmbedSettings()
        poorlyFormedSettingsKeys = emptySet()
        font = StoatAPIHost.platform.defaultFont
    }
}

private fun parseTheme(value: String): Theme? =
    if (value == "Revolt") Theme.Default else runCatching { Theme.valueOf(value) }.getOrNull()

/**
 * The single `theme` value written alongside the per-mode themes, for app versions that only read
 * that field: the theme itself when both modes match, "System" for the old default pairing, and the
 * dark mode theme otherwise.
 */
fun legacyThemeValue(dark: Theme, light: Theme): Theme = when {
    dark == light -> dark
    dark == Theme.Default && light == Theme.Light -> Theme.None
    else -> dark
}
