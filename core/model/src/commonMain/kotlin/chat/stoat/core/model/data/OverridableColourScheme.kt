package chat.stoat.core.model.data

import kotlinx.serialization.Serializable

@Serializable
data class OverridableColourScheme(
    val primary: Int? = null,
    val onPrimary: Int? = null,
    val primaryContainer: Int? = null,
    val onPrimaryContainer: Int? = null,
    val inversePrimary: Int? = null,
    val secondary: Int? = null,
    val onSecondary: Int? = null,
    val secondaryContainer: Int? = null,
    val onSecondaryContainer: Int? = null,
    val tertiary: Int? = null,
    val onTertiary: Int? = null,
    val tertiaryContainer: Int? = null,
    val onTertiaryContainer: Int? = null,
    val background: Int? = null,
    val onBackground: Int? = null,
    val surface: Int? = null,
    val onSurface: Int? = null,
    val surfaceVariant: Int? = null,
    val onSurfaceVariant: Int? = null,
    val surfaceTint: Int? = null,
    val inverseSurface: Int? = null,
    val inverseOnSurface: Int? = null,
    val error: Int? = null,
    val onError: Int? = null,
    val errorContainer: Int? = null,
    val onErrorContainer: Int? = null,
    val outline: Int? = null,
    val outlineVariant: Int? = null,
    val scrim: Int? = null
)