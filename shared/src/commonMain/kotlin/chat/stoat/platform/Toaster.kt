package chat.stoat.platform

import androidx.compose.runtime.staticCompositionLocalOf

enum class ToastDuration { Short, Long }

fun interface Toaster {
    fun show(message: String, duration: ToastDuration)
}

fun Toaster.show(message: String) = show(message, ToastDuration.Short)

val LocalToaster = staticCompositionLocalOf<Toaster> {
    error("No Toaster, wrap the content in StoatAppTheme")
}
