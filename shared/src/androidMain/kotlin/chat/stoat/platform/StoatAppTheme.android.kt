package chat.stoat.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.UriHandler
import androidx.core.net.toUri

@Composable
internal actual fun rememberPlatformToaster(): Toaster {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        Toaster { message, duration ->
            val length = when (duration) {
                ToastDuration.Short -> Toast.LENGTH_SHORT
                ToastDuration.Long -> Toast.LENGTH_LONG
            }
            Toast.makeText(context, message, length).show()
        }
    }
}

@Composable
internal actual fun rememberPlatformUriHandler(): UriHandler {
    val context = LocalContext.current
    val toolbarColour = MaterialTheme.colorScheme.surfaceContainer.toArgb()
    return remember(context, toolbarColour) { CustomTabsUriHandler(context, toolbarColour) }
}

private class CustomTabsUriHandler(
    private val context: Context,
    private val toolbarColour: Int,
) : UriHandler {
    override fun openUri(uri: String) {
        val parsed = uri.toUri()
        val customTab = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(toolbarColour)
                    .build()
            )
            .build()
        try {
            customTab.launchUrl(context, parsed)
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, parsed).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: ActivityNotFoundException) {
                throw IllegalArgumentException("Cannot open $uri", e)
            }
        }
    }
}
