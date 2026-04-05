package chat.stoat.ndk

annotation class NativeLibrary(val name: String) {
    companion object {
        const val LIB_NAME_NATIVE_MARKDOWN = "stendal"
    }
}

object NativeLibraries {
    fun init() {
        System.loadLibrary(NativeLibrary.LIB_NAME_NATIVE_MARKDOWN)
        Stendal.init()
    }
}
