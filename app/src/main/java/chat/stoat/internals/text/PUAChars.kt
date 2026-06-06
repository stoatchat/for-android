package chat.stoat.internals.text

internal const val PUA_MIN = 0xE0E0
internal const val PUA_MAX = 0xE0E6

internal fun String.stripPUAChars(): String = filter { it.code !in PUA_MIN..PUA_MAX }
