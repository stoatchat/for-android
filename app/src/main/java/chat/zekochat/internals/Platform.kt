package chat.zekochat.internals

import android.os.Build

object Platform {
    fun needsShowClipboardNotification(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    }
}
