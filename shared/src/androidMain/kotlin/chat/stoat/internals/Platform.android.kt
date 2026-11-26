package chat.stoat.internals

import android.os.Build

actual object Platform {
    actual fun needsShowClipboardNotification(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    }
}
