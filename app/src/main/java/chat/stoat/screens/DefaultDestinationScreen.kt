package chat.stoat.screens

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import chat.stoat.composables.screens.splash.DisconnectedScreen
import chat.stoat.internals.extensions.getComponentActivity
import chat.stoat.navigation.Navigator
import chat.stoat.navigation.Route

@Composable
fun DefaultDestinationScreen(
    navigator: Navigator,
    nextDestination: Route? = null,
    isConnected: Boolean = false,
    onRetryConnection: () -> Unit = {}
) {
    val context = LocalContext.current

    if (!isConnected) {
        DisconnectedScreen(
            onRetry = {
                onRetryConnection()
            }
        )
        return
    }

    LaunchedEffect(nextDestination) {
        nextDestination?.let {
            // The window can lose its transparent status bar before we leave this destination
            // See the other one in MainActivity.kt
            val activity = context.getComponentActivity() as Activity
            activity.window.statusBarColor = Color.Transparent.toArgb()

            navigator.resetTo(it)
        }
    }

    Box(Modifier.fillMaxSize())
}