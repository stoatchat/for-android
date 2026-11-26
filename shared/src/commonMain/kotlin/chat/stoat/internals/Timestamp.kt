package chat.stoat.internals

/**
 * Display a timestamp according to Stoat timestamp syntax (`<t:123456789:X>`)
 */
expect fun resolveTimestamp(timestamp: Long, modifier: String? = null): String
