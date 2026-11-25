package chat.stoat.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import chat.stoat.core.model.data.OverridableColourScheme
import chat.stoat.core.model.settings.Theme
import chat.stoat.core.model.settings.UserInterfaceFont

val LightColorScheme = lightColorScheme(
    primary = Colour.PrimaryLight,
    onPrimary = Colour.OnPrimaryLight,
    primaryContainer = Colour.PrimaryContainerLight,
    onPrimaryContainer = Colour.OnPrimaryContainerLight,
    inversePrimary = Colour.InversePrimaryLight,
    secondary = Colour.SecondaryLight,
    onSecondary = Colour.OnSecondaryLight,
    secondaryContainer = Colour.SecondaryContainerLight,
    onSecondaryContainer = Colour.OnSecondaryContainerLight,
    tertiary = Colour.TertiaryLight,
    onTertiary = Colour.OnTertiaryLight,
    tertiaryContainer = Colour.TertiaryContainerLight,
    onTertiaryContainer = Colour.OnTertiaryContainerLight,
    background = Colour.BackgroundLight,
    onBackground = Colour.OnBackgroundLight,
    surface = Colour.SurfaceLight,
    onSurface = Colour.OnSurfaceLight,
    surfaceVariant = Colour.SurfaceVariantLight,
    onSurfaceVariant = Colour.OnSurfaceVariantLight,
    surfaceTint = Colour.SurfaceTintLight,
    inverseSurface = Colour.InverseSurfaceLight,
    inverseOnSurface = Colour.InverseOnSurfaceLight,
    error = Colour.ErrorLight,
    onError = Colour.OnErrorLight,
    errorContainer = Colour.ErrorContainerLight,
    onErrorContainer = Colour.OnErrorContainerLight,
    outline = Colour.OutlineLight,
    outlineVariant = Colour.OutlineVariantLight,
    scrim = Colour.ScrimLight,
    surfaceBright = Colour.SurfaceBrightLight,
    surfaceContainer = Colour.SurfaceContainerLight,
    surfaceContainerHigh = Colour.SurfaceContainerHighLight,
    surfaceContainerHighest = Colour.SurfaceContainerHighestLight,
    surfaceContainerLow = Colour.SurfaceContainerLowLight,
    surfaceContainerLowest = Colour.SurfaceContainerLowestLight,
    surfaceDim = Colour.SurfaceDimLight,
    primaryFixed = Colour.PrimaryFixed,
    primaryFixedDim = Colour.PrimaryFixedDim,
    onPrimaryFixed = Colour.OnPrimaryFixed,
    onPrimaryFixedVariant = Colour.OnPrimaryFixedVariant,
    secondaryFixed = Colour.SecondaryFixed,
    secondaryFixedDim = Colour.SecondaryFixedDim,
    onSecondaryFixed = Colour.OnSecondaryFixed,
    onSecondaryFixedVariant = Colour.OnSecondaryFixedVariant,
    tertiaryFixed = Colour.TertiaryFixed,
    tertiaryFixedDim = Colour.TertiaryFixedDim,
    onTertiaryFixed = Colour.OnTertiaryFixed,
    onTertiaryFixedVariant = Colour.OnTertiaryFixedVariant,
)

val DarkColorScheme = darkColorScheme(
    primary = Colour.PrimaryDark,
    onPrimary = Colour.OnPrimaryDark,
    primaryContainer = Colour.PrimaryContainerDark,
    onPrimaryContainer = Colour.OnPrimaryContainerDark,
    inversePrimary = Colour.InversePrimaryDark,
    secondary = Colour.SecondaryDark,
    onSecondary = Colour.OnSecondaryDark,
    secondaryContainer = Colour.SecondaryContainerDark,
    onSecondaryContainer = Colour.OnSecondaryContainerDark,
    tertiary = Colour.TertiaryDark,
    onTertiary = Colour.OnTertiaryDark,
    tertiaryContainer = Colour.TertiaryContainerDark,
    onTertiaryContainer = Colour.OnTertiaryContainerDark,
    background = Colour.BackgroundDark,
    onBackground = Colour.OnBackgroundDark,
    surface = Colour.SurfaceDark,
    onSurface = Colour.OnSurfaceDark,
    surfaceVariant = Colour.SurfaceVariantDark,
    onSurfaceVariant = Colour.OnSurfaceVariantDark,
    surfaceTint = Colour.SurfaceTintDark,
    inverseSurface = Colour.InverseSurfaceDark,
    inverseOnSurface = Colour.InverseOnSurfaceDark,
    error = Colour.ErrorDark,
    onError = Colour.OnErrorDark,
    errorContainer = Colour.ErrorContainerDark,
    onErrorContainer = Colour.OnErrorContainerDark,
    outline = Colour.OutlineDark,
    outlineVariant = Colour.OutlineVariantDark,
    scrim = Colour.ScrimDark,
    surfaceBright = Colour.SurfaceBrightDark,
    surfaceContainer = Colour.SurfaceContainerDark,
    surfaceContainerHigh = Colour.SurfaceContainerHighDark,
    surfaceContainerHighest = Colour.SurfaceContainerHighestDark,
    surfaceContainerLow = Colour.SurfaceContainerLowDark,
    surfaceContainerLowest = Colour.SurfaceContainerLowestDark,
    surfaceDim = Colour.SurfaceDimDark,
    primaryFixed = Colour.PrimaryFixed,
    primaryFixedDim = Colour.PrimaryFixedDim,
    onPrimaryFixed = Colour.OnPrimaryFixed,
    onPrimaryFixedVariant = Colour.OnPrimaryFixedVariant,
    secondaryFixed = Colour.SecondaryFixed,
    secondaryFixedDim = Colour.SecondaryFixedDim,
    onSecondaryFixed = Colour.OnSecondaryFixed,
    onSecondaryFixedVariant = Colour.OnSecondaryFixedVariant,
    tertiaryFixed = Colour.TertiaryFixed,
    tertiaryFixedDim = Colour.TertiaryFixedDim,
    onTertiaryFixed = Colour.OnTertiaryFixed,
    onTertiaryFixedVariant = Colour.OnTertiaryFixedVariant,
)

val AmoledColorScheme = DarkColorScheme.copy(
    background = Color(0xff000000),
    onBackground = Color(0xffffffff),
    surfaceVariant = Color(0xff131313),
    onSurfaceVariant = Color(0xffffffff),
    surface = Color(0xff000000),
    onSurface = Color(0xffffffff),
    surfaceContainerLowest = Color(0xff000000),
    surfaceContainerLow = Color(0xff000000),
    surfaceContainer = Color(0xff000000),
    surfaceContainerHigh = Color(0xff000000),
    surfaceContainerHighest = Color(0xff000000),
)

@Composable
fun getColorScheme(
    requestedTheme: Theme,
    colourOverrides: OverridableColourScheme? = null
): ColorScheme {
    val systemInDarkTheme = isSystemInDarkTheme()
    val m3Supported = systemSupportsDynamicColors()

    val colorScheme = when {
        m3Supported && requestedTheme == Theme.M3Dynamic -> dynamicColorScheme(systemInDarkTheme)

        requestedTheme == Theme.Default -> DarkColorScheme
        requestedTheme == Theme.Light -> LightColorScheme
        requestedTheme == Theme.Amoled -> AmoledColorScheme
        requestedTheme == Theme.None && systemInDarkTheme -> DarkColorScheme
        requestedTheme == Theme.None && !systemInDarkTheme -> LightColorScheme
        else -> DarkColorScheme
    }.copy()

    val colorSchemeIsDark = when {
        m3Supported && requestedTheme == Theme.M3Dynamic -> isSystemInDarkTheme()
        requestedTheme == Theme.Default -> true
        requestedTheme == Theme.Light -> false
        requestedTheme == Theme.Amoled -> true
        requestedTheme == Theme.None && systemInDarkTheme -> true
        requestedTheme == Theme.None && !systemInDarkTheme -> false
        else -> true
    }

    SystemBarAppearance(isDark = colorSchemeIsDark)

    if (colourOverrides == null) return colorScheme
    return colourOverrides.applyTo(colorScheme)
}

/** Whether the active theme is dark, including themes that follow the system setting. */
val LocalThemeIsDark = staticCompositionLocalOf { true }

@Composable
fun StoatTheme(
    requestedTheme: Theme,
    requestedUserInterfaceFont: UserInterfaceFont,
    colourOverrides: OverridableColourScheme? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = getColorScheme(requestedTheme, colourOverrides)
    val typography = when (requestedUserInterfaceFont) {
        UserInterfaceFont.Default -> StoatTypography
        UserInterfaceFont.GoogleSansFlex -> GoogleTypography
    }

    CompositionLocalProvider(LocalThemeIsDark provides isThemeDark(requestedTheme)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

fun getDefaultTheme(): Theme {
    return when {
        systemSupportsDynamicColors() -> Theme.M3Dynamic
        else -> Theme.Default
    }
}

fun isThemeDark(theme: Theme, systemIsDark: Boolean): Boolean {
    return when (theme) {
        Theme.Default, Theme.Amoled -> true
        Theme.Light -> false
        Theme.M3Dynamic, Theme.None -> systemIsDark
    }
}

@Composable
fun isThemeDark(theme: Theme) = isThemeDark(theme, isSystemInDarkTheme())
