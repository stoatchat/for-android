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
import androidx.navigation.NavController
import chat.stoat.api.internals.getComponentActivity
import chat.stoat.composables.screens.splash.DisconnectedScreen

@Composable
fun DefaultDestinationScreen(
    navController: NavController,
    nextDestination: String? = null,
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

            navController.popBackStack(navController.graph.startDestinationRoute!!, true)
            navController.navigate(it)
        }
    }

    Box(Modifier.fillMaxSize())
}