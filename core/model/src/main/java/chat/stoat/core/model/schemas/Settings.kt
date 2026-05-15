package chat.stoat.core.model.schemas

import chat.stoat.core.model.data.OverridableColourScheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class OrderingSettings(
    val servers: List<String> = emptyList()
)


@Serializable
data class NotificationSettings(
    val channel: Map<String, String> = emptyMap(),
    val server: Map<String, String> = emptyMap()
)

@Serializable
data class _NotificationSettingsToParse( // quirk
    val channel: Map<String, JsonElement?> = emptyMap(),
    val server: Map<String, JsonElement?> = emptyMap()
)

@Serializable
data class ReleaseNotesSettings(
    val lastSeenId: String? = null,
    val lastSeenAt: String? = null
)

@Serializable
data class AndroidSpecificSettingsSpecialEmbedSettings(
    /**
     * Whether to embed YouTube videos interactively.
     * Boolean.
     */
    val embedYouTube: Boolean = true,
    /**
     * Whether to embed Apple Music albums and tracks interactively.
     * Boolean.
     */
    val embedAppleMusic: Boolean = false
)

@Serializable
data class AndroidSpecificSettings(
    /**
     * The theme to use for the app.
     * Can be one of `{ None, Default, Light, M3Dynamic, Amoled }`
     */
    var theme: String? = null,
    /**
     * The font to use for the app.
     * Can be one of `{ Default, GoogleSansFlex }`
     */
    var font: String? = null,
    /**
     * Colour overrides.
     * Map of `primary, onPrimary, primaryContainer, onPrimaryContainer, inversePrimary, secondary, onSecondary, secondaryContainer, onSecondaryContainer, tertiary, onTertiary, tertiaryContainer, onTertiaryContainer, background, onBackground, surface, onSurface, surfaceVariant, onSurfaceVariant, surfaceTint, inverseSurface, inverseOnSurface, error, onError, errorContainer, onErrorContainer, outline, outlineVariant, scrim` to int colours.
     */
    var colourOverrides: OverridableColourScheme? = null,
    /**
     * Message reply style.
     * Can be one of `{ None, SwipeFromEnd, DoubleTap }`
     */
    var messageReplyStyle: String? = null,
    /**
     * Avatar radius.
     * Must be integer in range 0..50 inclusive.
     */
    var avatarRadius: Int? = null,
    /**
     * Controls preferences for special embeds.
     * Object; See [AndroidSpecificSettingsSpecialEmbedSettings] for format.
     */
    var specialEmbedSettings: AndroidSpecificSettingsSpecialEmbedSettings? = null
)
