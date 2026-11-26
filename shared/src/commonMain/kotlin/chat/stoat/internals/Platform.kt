package chat.stoat.internals

expect object Platform {
    /** Whether the app should confirm a copy itself, because the system doesn't show a notice. */
    fun needsShowClipboardNotification(): Boolean
}
