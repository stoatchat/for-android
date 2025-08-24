package chat.peptide.api.internals

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

fun Color.asComponentJsonPrimitiveString(): JsonPrimitive {
    val rgb = this.toArgb()
    val r = rgb shr 16 and 0xFF
    val g = rgb shr 8 and 0xFF
    val b = rgb shr 0 and 0xFF
    return JsonPrimitive("$r, $g, $b")
}

fun Color.asHexJsonPrimitiveString(): JsonPrimitive {
    val rgb = this.toArgb()
    val r = rgb shr 16 and 0xFF
    val g = rgb shr 8 and 0xFF
    val b = rgb shr 0 and 0xFF
    return JsonPrimitive("#${r.toString(16)}${g.toString(16)}${b.toString(16)}")
}

object ThemeCompat {
    @Composable
    fun materialThemeAsDiscoverTheme(materialTheme: MaterialTheme): JsonObject {
        // https://github.com/peptidechat/discover/blob/6effdf4a611e89b38b5e6bccefa1cd999e4b545f/styles/variables.scss
        return JsonObject(
            mapOf(
                "accent" to materialTheme.colorScheme.primary.asHexJsonPrimitiveString(),
                "accent-rgb" to materialTheme.colorScheme.primary.asComponentJsonPrimitiveString(),
                "background" to materialTheme.colorScheme.background.asHexJsonPrimitiveString(),
                "background-rgb" to materialTheme.colorScheme.background.asComponentJsonPrimitiveString(),
                "foreground" to materialTheme.colorScheme.onBackground.asHexJsonPrimitiveString(),
                "foreground-rgb" to materialTheme.colorScheme.onBackground.asComponentJsonPrimitiveString(),
                "block" to materialTheme.colorScheme.surface.asHexJsonPrimitiveString(),
                "block-rgb" to materialTheme.colorScheme.surface.asComponentJsonPrimitiveString(),
                "message-box" to materialTheme.colorScheme.surface.asHexJsonPrimitiveString(),
                "message-box-rgb" to materialTheme.colorScheme.surface.asComponentJsonPrimitiveString(),
                "mention" to materialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    .asHexJsonPrimitiveString(),
                "mention-rgb" to materialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    .asComponentJsonPrimitiveString(),
                "success" to materialTheme.colorScheme.primary.asHexJsonPrimitiveString(),
                "success-rgb" to materialTheme.colorScheme.primary.asComponentJsonPrimitiveString(),
                "warning" to materialTheme.colorScheme.secondary.asHexJsonPrimitiveString(),
                "warning-rgb" to materialTheme.colorScheme.secondary.asComponentJsonPrimitiveString(),
                "error" to materialTheme.colorScheme.error.asHexJsonPrimitiveString(),
                "error-rgb" to materialTheme.colorScheme.error.asComponentJsonPrimitiveString(),
                "hover" to materialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    .asHexJsonPrimitiveString(),
                "hover-rgb" to materialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    .asComponentJsonPrimitiveString(),
                "scrollbar-thumb" to materialTheme.colorScheme.primary.asHexJsonPrimitiveString(),
                "scrollbar-thumb-rgb" to materialTheme.colorScheme.primary.asComponentJsonPrimitiveString(),
                "scrollbar-track" to materialTheme.colorScheme.background.asHexJsonPrimitiveString(),
                "scrollbar-track-rgb" to materialTheme.colorScheme.background.asComponentJsonPrimitiveString(),
                "primary-background" to materialTheme.colorScheme.background.asHexJsonPrimitiveString(),
                "primary-background-rgb" to materialTheme.colorScheme.background.asComponentJsonPrimitiveString(),
                "primary-header" to materialTheme.colorScheme.background.asHexJsonPrimitiveString(),
                "primary-header-rgb" to materialTheme.colorScheme.background.asComponentJsonPrimitiveString(),
                "secondary-background" to materialTheme.colorScheme.surface.asHexJsonPrimitiveString(),
                "secondary-background-rgb" to materialTheme.colorScheme.surface.asComponentJsonPrimitiveString(),
                "secondary-foreground" to materialTheme.colorScheme.onSurface.asHexJsonPrimitiveString(),
                "secondary-foreground-rgb" to materialTheme.colorScheme.onSurface.asComponentJsonPrimitiveString(),
                "secondary-header" to materialTheme.colorScheme.surface.asHexJsonPrimitiveString(),
                "secondary-header-rgb" to materialTheme.colorScheme.surface.asComponentJsonPrimitiveString(),
                "tertiary-background" to materialTheme.colorScheme.surface.asHexJsonPrimitiveString(),
                "tertiary-background-rgb" to materialTheme.colorScheme.surface.asComponentJsonPrimitiveString(),
                "tertiary-foreground" to materialTheme.colorScheme.onSurface.asHexJsonPrimitiveString(),
                "tertiary-foreground-rgb" to materialTheme.colorScheme.onSurface.asComponentJsonPrimitiveString(),
                "accent-contrast" to materialTheme.colorScheme.onPrimary.asHexJsonPrimitiveString(),
                "accent-contrast-rgb" to materialTheme.colorScheme.onPrimary.asComponentJsonPrimitiveString(),
                "background-contrast" to materialTheme.colorScheme.onBackground.asHexJsonPrimitiveString(),
                "background-contrast-rgb" to materialTheme.colorScheme.onBackground.asComponentJsonPrimitiveString(),
                "foreground-contrast" to materialTheme.colorScheme.inverseOnSurface.asHexJsonPrimitiveString(),
                "foreground-contrast-rgb" to materialTheme.colorScheme.inverseOnSurface.asComponentJsonPrimitiveString(),
                "block-contrast" to materialTheme.colorScheme.onSurface.asHexJsonPrimitiveString(),
                "block-contrast-rgb" to materialTheme.colorScheme.onSurface.asComponentJsonPrimitiveString(),
                "message-box-contrast" to materialTheme.colorScheme.onSurface.asHexJsonPrimitiveString(),
                "message-box-contrast-rgb" to materialTheme.colorScheme.onSurface.asComponentJsonPrimitiveString(),
                "mention-contrast" to materialTheme.colorScheme.onPrimary.asHexJsonPrimitiveString(),
                "mention-contrast-rgb" to materialTheme.colorScheme.onPrimary.asComponentJsonPrimitiveString(),
                "success-contrast" to materialTheme.colorScheme.onPrimary.asHexJsonPrimitiveString(),
                "success-contrast-rgb" to materialTheme.colorScheme.onPrimary.asComponentJsonPrimitiveString(),
                "warning-contrast" to materialTheme.colorScheme.onSecondary.asHexJsonPrimitiveString(),
                "warning-contrast-rgb" to materialTheme.colorScheme.onSecondary.asComponentJsonPrimitiveString(),
                "error-contrast" to materialTheme.colorScheme.onError.asHexJsonPrimitiveString(),
                "error-contrast-rgb" to materialTheme.colorScheme.onError.asComponentJsonPrimitiveString(),
                "primary-background-contrast" to materialTheme.colorScheme.onBackground.asHexJsonPrimitiveString(),
                "primary-background-contrast-rgb" to materialTheme.colorScheme.onBackground.asComponentJsonPrimitiveString(),
                "primary-header-contrast" to materialTheme.colorScheme.onBackground.asHexJsonPrimitiveString(),
                "primary-header-contrast-rgb" to materialTheme.colorScheme.onBackground.asComponentJsonPrimitiveString(),
                "secondary-background-contrast" to materialTheme.colorScheme.onSurface.asHexJsonPrimitiveString(),
                "secondary-background-contrast-rgb" to materialTheme.colorScheme.onSurface.asComponentJsonPrimitiveString(),
                "secondary-foreground-contrast" to materialTheme.colorScheme.inverseOnSurface.asHexJsonPrimitiveString(),
                "secondary-foreground-contrast-rgb" to materialTheme.colorScheme.inverseOnSurface.asComponentJsonPrimitiveString(),
                "secondary-header-contrast" to materialTheme.colorScheme.onSurface.asHexJsonPrimitiveString(),
                "secondary-header-contrast-rgb" to materialTheme.colorScheme.onSurface.asComponentJsonPrimitiveString(),
                "tertiary-background-contrast" to materialTheme.colorScheme.onSurface.asHexJsonPrimitiveString(),
                "tertiary-background-contrast-rgb" to materialTheme.colorScheme.onSurface.asComponentJsonPrimitiveString(),
                "tertiary-foreground-contrast" to materialTheme.colorScheme.inverseOnSurface.asHexJsonPrimitiveString(),
                "tertiary-foreground-contrast-rgb" to materialTheme.colorScheme.inverseOnSurface.asComponentJsonPrimitiveString(),
            )
        )
    }
}