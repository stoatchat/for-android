package chat.peptide.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import chat.peptide.R
import kotlinx.serialization.Serializable

// Word of warning, this file is ugly, because I've had to fight a bit with the Compose compiler,
// namely native Kotlin (not Java) reflection seems to consistently break it.
// So I've had to resort to... methods like this. I'm sorry.
// If you've been linked to this file, I promise the rest of the codebase is not like this.
// Original comments during research and development preserved.

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
) {
    fun applyTo(colorScheme: ColorScheme): ColorScheme {
        var newScheme = colorScheme.copy()

        // This is SLOW. It is also STUPID. But using reflection breaks the Compose compiler.
        // Another piece of trash from Google. This company should go bankrupt already, what a
        // joke.
        if (primary != null) newScheme = newScheme.copy(primary = Color(primary))
        if (onPrimary != null) newScheme = newScheme.copy(onPrimary = Color(onPrimary))
        if (primaryContainer != null) newScheme =
            newScheme.copy(primaryContainer = Color(primaryContainer))
        if (onPrimaryContainer != null) newScheme =
            newScheme.copy(onPrimaryContainer = Color(onPrimaryContainer))
        if (inversePrimary != null) newScheme =
            newScheme.copy(inversePrimary = Color(inversePrimary))
        if (secondary != null) newScheme = newScheme.copy(secondary = Color(secondary))
        if (onSecondary != null) newScheme = newScheme.copy(onSecondary = Color(onSecondary))
        if (secondaryContainer != null) newScheme =
            newScheme.copy(secondaryContainer = Color(secondaryContainer))
        if (onSecondaryContainer != null) newScheme =
            newScheme.copy(onSecondaryContainer = Color(onSecondaryContainer))
        if (tertiary != null) newScheme = newScheme.copy(tertiary = Color(tertiary))
        if (onTertiary != null) newScheme = newScheme.copy(onTertiary = Color(onTertiary))
        if (tertiaryContainer != null) newScheme =
            newScheme.copy(tertiaryContainer = Color(tertiaryContainer))
        if (onTertiaryContainer != null) newScheme =
            newScheme.copy(onTertiaryContainer = Color(onTertiaryContainer))
        if (background != null) newScheme = newScheme.copy(background = Color(background))
        if (onBackground != null) newScheme = newScheme.copy(onBackground = Color(onBackground))
        if (surface != null) newScheme = newScheme.copy(surface = Color(surface))
        if (onSurface != null) newScheme = newScheme.copy(onSurface = Color(onSurface))
        if (surfaceVariant != null) newScheme =
            newScheme.copy(surfaceVariant = Color(surfaceVariant))
        if (onSurfaceVariant != null) newScheme =
            newScheme.copy(onSurfaceVariant = Color(onSurfaceVariant))
        if (surfaceTint != null) newScheme = newScheme.copy(surfaceTint = Color(surfaceTint))
        if (inverseSurface != null) newScheme =
            newScheme.copy(inverseSurface = Color(inverseSurface))
        if (inverseOnSurface != null) newScheme =
            newScheme.copy(inverseOnSurface = Color(inverseOnSurface))
        if (error != null) newScheme = newScheme.copy(error = Color(error))
        if (onError != null) newScheme = newScheme.copy(onError = Color(onError))
        if (errorContainer != null) newScheme =
            newScheme.copy(errorContainer = Color(errorContainer))
        if (onErrorContainer != null) newScheme =
            newScheme.copy(onErrorContainer = Color(onErrorContainer))
        if (outline != null) newScheme = newScheme.copy(outline = Color(outline))
        if (outlineVariant != null) newScheme =
            newScheme.copy(outlineVariant = Color(outlineVariant))
        if (scrim != null) newScheme = newScheme.copy(scrim = Color(scrim))

        return newScheme
    }

    fun applyFromKeyValueMap(map: Map<String, Int>): OverridableColourScheme {
        var newScheme = this

        map.filterKeys { it in fieldNames }.forEach { (key, value) ->
            when (key) {
                "primary" -> newScheme = newScheme.copy(primary = value)
                "onPrimary" -> newScheme = newScheme.copy(onPrimary = value)
                "primaryContainer" -> newScheme = newScheme.copy(primaryContainer = value)
                "onPrimaryContainer" -> newScheme =
                    newScheme.copy(onPrimaryContainer = value)

                "inversePrimary" -> newScheme = newScheme.copy(inversePrimary = (value))
                "secondary" -> newScheme = newScheme.copy(secondary = (value))
                "onSecondary" -> newScheme = newScheme.copy(onSecondary = (value))
                "secondaryContainer" -> newScheme =
                    newScheme.copy(secondaryContainer = (value))

                "onSecondaryContainer" -> newScheme =
                    newScheme.copy(onSecondaryContainer = (value))

                "tertiary" -> newScheme = newScheme.copy(tertiary = (value))
                "onTertiary" -> newScheme = newScheme.copy(onTertiary = (value))
                "tertiaryContainer" -> newScheme = newScheme.copy(tertiaryContainer = (value))
                "onTertiaryContainer" -> newScheme =
                    newScheme.copy(onTertiaryContainer = (value))

                "background" -> newScheme = newScheme.copy(background = (value))
                "onBackground" -> newScheme = newScheme.copy(onBackground = (value))
                "surface" -> newScheme = newScheme.copy(surface = (value))
                "onSurface" -> newScheme = newScheme.copy(onSurface = (value))
                "surfaceVariant" -> newScheme = newScheme.copy(surfaceVariant = (value))
                "onSurfaceVariant" -> newScheme = newScheme.copy(onSurfaceVariant = (value))
                "surfaceTint" -> newScheme = newScheme.copy(surfaceTint = (value))
                "inverseSurface" -> newScheme = newScheme.copy(inverseSurface = (value))
                "inverseOnSurface" -> newScheme = newScheme.copy(inverseOnSurface = (value))
                "error" -> newScheme = newScheme.copy(error = (value))
                "onError" -> newScheme = newScheme.copy(onError = (value))
                "errorContainer" -> newScheme = newScheme.copy(errorContainer = (value))
                "onErrorContainer" -> newScheme = newScheme.copy(onErrorContainer = (value))
                "outline" -> newScheme = newScheme.copy(outline = (value))
                "outlineVariant" -> newScheme = newScheme.copy(outlineVariant = (value))
                "scrim" -> newScheme = newScheme.copy(scrim = (value))
            }
        }

        return newScheme
    }

    fun getFieldByName(name: String): Int? {
        return when (name) {
            "primary" -> primary
            "onPrimary" -> onPrimary
            "primaryContainer" -> primaryContainer
            "onPrimaryContainer" -> onPrimaryContainer
            "inversePrimary" -> inversePrimary
            "secondary" -> secondary
            "onSecondary" -> onSecondary
            "secondaryContainer" -> secondaryContainer
            "onSecondaryContainer" -> onSecondaryContainer
            "tertiary" -> tertiary
            "onTertiary" -> onTertiary
            "tertiaryContainer" -> tertiaryContainer
            "onTertiaryContainer" -> onTertiaryContainer
            "background" -> background
            "onBackground" -> onBackground
            "surface" -> surface
            "onSurface" -> onSurface
            "surfaceVariant" -> surfaceVariant
            "onSurfaceVariant" -> onSurfaceVariant
            "surfaceTint" -> surfaceTint
            "inverseSurface" -> inverseSurface
            "inverseOnSurface" -> inverseOnSurface
            "error" -> error
            "onError" -> onError
            "errorContainer" -> errorContainer
            "onErrorContainer" -> onErrorContainer
            "outline" -> outline
            "outlineVariant" -> outlineVariant
            "scrim" -> scrim
            else -> null
        }
    }

    companion object {
        // I am genuinely going to go to Google's office and hand them this code and tell them
        // to fix their garbage Gradle plugin
        val fieldNames = listOf(
            "primary",
            "onPrimary",
            "primaryContainer",
            "onPrimaryContainer",
            "inversePrimary",
            "secondary",
            "onSecondary",
            "secondaryContainer",
            "onSecondaryContainer",
            "tertiary",
            "onTertiary",
            "tertiaryContainer",
            "onTertiaryContainer",
            "background",
            "onBackground",
            "surface",
            "onSurface",
            "surfaceVariant",
            "onSurfaceVariant",
            "surfaceTint",
            "inverseSurface",
            "inverseOnSurface",
            "error",
            "onError",
            "errorContainer",
            "onErrorContainer",
            "outline",
            "outlineVariant",
            "scrim"
        )

        // See above comment HOLY SHIT i am genuinely going insane
        val fieldNameToResource = mapOf(
            "primary" to R.string.settings_appearance_colour_overrides_primary,
            "onPrimary" to R.string.settings_appearance_colour_overrides_on_primary,
            "primaryContainer" to R.string.settings_appearance_colour_overrides_primary_container,
            "onPrimaryContainer" to R.string.settings_appearance_colour_overrides_on_primary_container,
            "inversePrimary" to R.string.settings_appearance_colour_overrides_inverse_primary,
            "secondary" to R.string.settings_appearance_colour_overrides_secondary,
            "onSecondary" to R.string.settings_appearance_colour_overrides_on_secondary,
            "secondaryContainer" to R.string.settings_appearance_colour_overrides_secondary_container,
            "onSecondaryContainer" to R.string.settings_appearance_colour_overrides_on_secondary_container,
            "tertiary" to R.string.settings_appearance_colour_overrides_tertiary,
            "onTertiary" to R.string.settings_appearance_colour_overrides_on_tertiary,
            "tertiaryContainer" to R.string.settings_appearance_colour_overrides_tertiary_container,
            "onTertiaryContainer" to R.string.settings_appearance_colour_overrides_on_tertiary_container,
            "background" to R.string.settings_appearance_colour_overrides_background,
            "onBackground" to R.string.settings_appearance_colour_overrides_on_background,
            "surface" to R.string.settings_appearance_colour_overrides_surface,
            "onSurface" to R.string.settings_appearance_colour_overrides_on_surface,
            "surfaceVariant" to R.string.settings_appearance_colour_overrides_surface_variant,
            "onSurfaceVariant" to R.string.settings_appearance_colour_overrides_on_surface_variant,
            "surfaceTint" to R.string.settings_appearance_colour_overrides_surface_tint,
            "inverseSurface" to R.string.settings_appearance_colour_overrides_inverse_surface,
            "inverseOnSurface" to R.string.settings_appearance_colour_overrides_inverse_on_surface,
            "error" to R.string.settings_appearance_colour_overrides_error,
            "onError" to R.string.settings_appearance_colour_overrides_on_error,
            "errorContainer" to R.string.settings_appearance_colour_overrides_error_container,
            "onErrorContainer" to R.string.settings_appearance_colour_overrides_on_error_container,
            "outline" to R.string.settings_appearance_colour_overrides_outline,
            "outlineVariant" to R.string.settings_appearance_colour_overrides_outline_variant,
            "scrim" to R.string.settings_appearance_colour_overrides_scrim
        )
    }
}

fun ColorScheme.getFieldByName(name: String): Int? {
    return when (name) {
        "primary" -> primary.toArgb()
        "onPrimary" -> onPrimary.toArgb()
        "primaryContainer" -> primaryContainer.toArgb()
        "onPrimaryContainer" -> onPrimaryContainer.toArgb()
        "inversePrimary" -> inversePrimary.toArgb()
        "secondary" -> secondary.toArgb()
        "onSecondary" -> onSecondary.toArgb()
        "secondaryContainer" -> secondaryContainer.toArgb()
        "onSecondaryContainer" -> onSecondaryContainer.toArgb()
        "tertiary" -> tertiary.toArgb()
        "onTertiary" -> onTertiary.toArgb()
        "tertiaryContainer" -> tertiaryContainer.toArgb()
        "onTertiaryContainer" -> onTertiaryContainer.toArgb()
        "background" -> background.toArgb()
        "onBackground" -> onBackground.toArgb()
        "surface" -> surface.toArgb()
        "onSurface" -> onSurface.toArgb()
        "surfaceVariant" -> surfaceVariant.toArgb()
        "onSurfaceVariant" -> onSurfaceVariant.toArgb()
        "surfaceTint" -> surfaceTint.toArgb()
        "inverseSurface" -> inverseSurface.toArgb()
        "inverseOnSurface" -> inverseOnSurface.toArgb()
        "error" -> error.toArgb()
        "onError" -> onError.toArgb()
        "errorContainer" -> errorContainer.toArgb()
        "onErrorContainer" -> onErrorContainer.toArgb()
        "outline" -> outline.toArgb()
        "outlineVariant" -> outlineVariant.toArgb()
        "scrim" -> scrim.toArgb()
        else -> null
    }
}