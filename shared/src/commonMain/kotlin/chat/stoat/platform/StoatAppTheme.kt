package chat.stoat.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import chat.stoat.api.settings.LoadedSettings
import chat.stoat.api.settings.SyncedSettings
import chat.stoat.core.designsystem.theme.StoatTheme

@Composable
fun StoatAppTheme(content: @Composable () -> Unit) {
    StoatTheme(
        requestedTheme = LoadedSettings.theme,
        requestedUserInterfaceFont = LoadedSettings.font,
        colourOverrides = SyncedSettings.android.colourOverrides
    ) {
        CompositionLocalProvider(
            LocalToaster provides rememberPlatformToaster(),
            LocalUriHandler provides rememberPlatformUriHandler(),
            content = content
        )
    }
}

@Composable
internal expect fun rememberPlatformToaster(): Toaster

/** Must be called inside the theme */
@Composable
internal expect fun rememberPlatformUriHandler(): UriHandler
