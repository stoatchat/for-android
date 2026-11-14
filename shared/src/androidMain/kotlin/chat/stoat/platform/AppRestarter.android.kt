package chat.stoat.platform

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberAppRestarter(): () -> Unit {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        {
            // cf. https://stackoverflow.com/a/46848226
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
                val restart = Intent.makeRestartActivityTask(launch.component)
                restart.`package` = context.packageName
                context.startActivity(restart)
                Runtime.getRuntime().exit(0)
            }
        }
    }
}
