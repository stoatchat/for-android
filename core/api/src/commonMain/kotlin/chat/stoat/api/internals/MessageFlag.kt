package chat.stoat.api.internals

/**
 * Flags for messages that can be set to modify their behavior.
 *
 * See [Reference](https://docs.rs/revolt-models/latest/revolt_models/v0/enum.MessageFlags.html) for
 * values
 *
 * `shl 0` is not used.
 *
 * @property value The integer value representing the flag.
 */
enum class MessageFlag(val value: Int) {
    /**
     * Message will not send push / desktop notifications.
     */
    SuppressNotifications(1 shl 1),

    /**
     * Message will mention all users who can see the channel
     * > **Cannot be set on send**
     */
    MentionsEveryone(1 shl 2),

    /**
     * Message will mention all users who are online and can see the channel.
     *
     * This cannot be true if [MentionsEveryone] is true.
     *
     * > **Cannot be set on send**
     */
    MentionsOnline(1 shl 3)
}

operator fun Int.plus(other: MessageFlag): Int {
    return this or other.value
}

fun Int.hasMessageFlag(flag: MessageFlag): Boolean {
    return this and flag.value == flag.value
}

infix fun Int?.has(flag: MessageFlag): Boolean {
    return this != null && this.hasMessageFlag(flag)
}