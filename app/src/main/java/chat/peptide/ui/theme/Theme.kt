package chat.peptide.ui.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat

//val LightColorScheme = lightColorScheme(
//    primary = AppColors.PrimaryLight,
//    onPrimary = AppColors.OnPrimaryLight,
//    primaryContainer = AppColors.PrimaryContainerLight,
//    onPrimaryContainer = AppColors.OnPrimaryContainerLight,
//    secondary = AppColors.SecondaryLight,
//    onSecondary = AppColors.OnSecondaryLight,
//    secondaryContainer = AppColors.SecondaryContainerLight,
//    onSecondaryContainer = AppColors.OnSecondaryContainerLight,
//    tertiary = AppColors.TertiaryLight,
//    onTertiary = AppColors.OnTertiaryLight,
//    tertiaryContainer = AppColors.TertiaryContainerLight,
//    onTertiaryContainer = AppColors.OnTertiaryContainerLight,
//    error = AppColors.ErrorLight,
//    onError = AppColors.OnErrorLight,
//    errorContainer = AppColors.ErrorContainerLight,
//    onErrorContainer = AppColors.OnErrorContainerLight,
//    background = AppColors.BackgroundLight,
//    onBackground = AppColors.OnBackgroundLight,
//    surface = AppColors.SurfaceLight,
//    onSurface = AppColors.OnSurfaceLight,
//    surfaceVariant = AppColors.SurfaceVariantLight,
//    onSurfaceVariant = AppColors.OnSurfaceVariantLight,
//    outline = AppColors.OutlineLight,
//    outlineVariant = AppColors.OutlineVariantLight,
//    scrim = AppColors.ScrimLight,
//    inverseSurface = AppColors.InverseSurfaceLight,
//    inverseOnSurface = AppColors.InverseOnSurfaceLight,
//    inversePrimary = AppColors.InversePrimaryLight,
//    surfaceDim = AppColors.SurfaceDimLight,
//    surfaceBright = AppColors.SurfaceBrightLight,
//    surfaceContainerLowest = AppColors.SurfaceContainerLowestLight,
//    surfaceContainerLow = AppColors.SurfaceContainerLowLight,
//    surfaceContainer = AppColors.SurfaceContainerLight,
//    surfaceContainerHigh = AppColors.SurfaceContainerHighLight,
//    surfaceContainerHighest = AppColors.SurfaceContainerHighestLight,
//)

private val PeptideColorScheme = darkColorScheme(
    primary = AppColors.PrimaryDark,
    onPrimary = AppColors.OnPrimaryDark,
    primaryContainer = AppColors.PrimaryContainerDark,
    onPrimaryContainer = AppColors.OnPrimaryContainerDark,
    secondary = AppColors.SecondaryDark,
    onSecondary = AppColors.OnSecondaryDark,
    secondaryContainer = AppColors.SecondaryContainerDark,
    onSecondaryContainer = AppColors.OnSecondaryContainerDark,
    tertiary = AppColors.TertiaryDark,
    onTertiary = AppColors.OnTertiaryDark,
    tertiaryContainer = AppColors.TertiaryContainerDark,
    onTertiaryContainer = AppColors.OnTertiaryContainerDark,
    error = AppColors.ErrorDark,
    onError = AppColors.OnErrorDark,
    errorContainer = AppColors.ErrorContainerDark,
    onErrorContainer = AppColors.OnErrorContainerDark,
    background = AppColors.BackgroundDark,
    onBackground = AppColors.OnBackgroundDark,
    surface = AppColors.SurfaceDark,
    onSurface = AppColors.OnSurfaceDark,
    surfaceVariant = AppColors.SurfaceVariantDark,
    onSurfaceVariant = AppColors.OnSurfaceVariantDark,
    outline = AppColors.OutlineDark,
    outlineVariant = AppColors.OutlineVariantDark,
    scrim = AppColors.ScrimDark,
    inverseSurface = AppColors.InverseSurfaceDark,
    inverseOnSurface = AppColors.InverseOnSurfaceDark,
    inversePrimary = AppColors.InversePrimaryDark,
    surfaceDim = AppColors.SurfaceDimDark,
    surfaceBright = AppColors.SurfaceBrightDark,
    surfaceContainerLowest = AppColors.SurfaceContainerLowestDark,
    surfaceContainerLow = AppColors.SurfaceContainerLowDark,
    surfaceContainer = AppColors.SurfaceContainerDark,
    surfaceContainerHigh = AppColors.SurfaceContainerHighDark,
    surfaceContainerHighest = AppColors.SurfaceContainerHighestDark,
)

enum class Theme {
    None,
    M3Dynamic,
}

@Composable
fun getColorScheme(
    requestedTheme: Theme,
    colourOverrides: OverridableColourScheme? = null
): ColorScheme {
    val context = LocalContext.current

    val systemInDarkTheme = isSystemInDarkTheme()
    val m3Supported = systemSupportsDynamicColors()

    val colorScheme = when {
        m3Supported && requestedTheme == Theme.M3Dynamic && systemInDarkTheme -> dynamicDarkColorScheme(
            context
        )

        m3Supported && requestedTheme == Theme.M3Dynamic && !systemInDarkTheme -> dynamicLightColorScheme(
            context
        )

        requestedTheme == Theme.None && systemInDarkTheme -> PeptideColorScheme
//        requestedTheme == Theme.None && !systemInDarkTheme -> LightColorScheme
        else -> PeptideColorScheme
    }.copy()

    val colorSchemeIsDark = when {
        m3Supported && requestedTheme == Theme.M3Dynamic -> isSystemInDarkTheme()
        requestedTheme == Theme.None && systemInDarkTheme -> true
        requestedTheme == Theme.None && !systemInDarkTheme -> false
        else -> true
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars =
                !colorSchemeIsDark
        }
    }

    if (colourOverrides == null) return colorScheme
    return colourOverrides.applyTo(colorScheme)
}

@SuppressLint("NewApi")
@Composable
fun PeptideTheme(
    requestedTheme: Theme,
    colourOverrides: OverridableColourScheme? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = getColorScheme(requestedTheme, colourOverrides)

    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(
            small = MaterialTheme.shapes.small.copy(
                topStart = CornerSize(8.dp),
                topEnd = CornerSize(8.dp),
                bottomStart = CornerSize(8.dp),
                bottomEnd = CornerSize(8.dp)
            )
        ),
        colorScheme = colorScheme,
        typography = PeptideTypography,
        content = content
    )
}

fun systemSupportsDynamicColors(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

fun getDefaultTheme(): Theme {
    return when {
        systemSupportsDynamicColors() -> Theme.M3Dynamic
        else -> Theme.None
    }
}

fun isThemeDark(theme: Theme, systemIsDark: Boolean): Boolean {
    return when (theme) {
        Theme.M3Dynamic, Theme.None -> systemIsDark
    }
}

@Composable
fun isThemeDark(theme: Theme) = isThemeDark(theme, isSystemInDarkTheme())