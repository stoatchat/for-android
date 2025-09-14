package chat.zekochat.screens

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
import chat.zekochat.PeptideApplication
import chat.zekochat.api.internals.getComponentActivity
import chat.zekochat.composables.screens.splash.DisconnectedScreen
import com.google.android.material.color.DynamicColors

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
            // Fix for SDK >=31, where core-splashscreen accidentally removes dynamic colours
            // See the other one in MainActivity.kt
            val activity = context.getComponentActivity() as Activity
            DynamicColors.applyToActivityIfAvailable(activity)
            DynamicColors.applyToActivitiesIfAvailable(PeptideApplication.instance)
            activity.window.statusBarColor = Color.Transparent.toArgb()

            navController.popBackStack(navController.graph.startDestinationRoute!!, true)
            navController.navigate(it)
        }
    }

    Box(Modifier.fillMaxSize())
}