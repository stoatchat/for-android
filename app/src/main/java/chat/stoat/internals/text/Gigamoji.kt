package chat.stoat.internals.text

private val STRING_IS_CUSTOM_EMOTES_REGEX = Regex("(?::[0-9A-HJKMNP-TV-Z]{26}:|\\s)+")
private val CUSTOM_EMOTE_REGEX = Regex(":[0-9A-HJKMNP-TV-Z]{26}:")

sealed class GigamojiState {
    object None : GigamojiState()
    object Single : GigamojiState()
    object Multiple : GigamojiState()

    val isGigamoji: Boolean get() = this != None
}

object Gigamoji {
    private fun countCustomEmotes(content: String): Int =
        CUSTOM_EMOTE_REGEX.findAll(content).count()

    private fun unicodeEmojiCount(unfilteredContent: String): Int? {
        val content = unfilteredContent.replace(STRING_IS_CUSTOM_EMOTES_REGEX, "")
        if (content.isBlank()) return 0

        var count = 0
        var lastWasZwj = false
        for (codepoint in content.codePoints()) {
            when {
                Character.isWhitespace(codepoint) -> lastWasZwj = false
                codepoint == 0x200D -> lastWasZwj = true
                codepoint == 0xFE0F || codepoint == 0xFE0E || codepoint == 0x20E3 -> Unit
                codepoint in PUA_MIN..PUA_MAX -> Unit
                codepoint in 0x1F3FB..0x1F3FF -> lastWasZwj = false
                MessageProcessor.emoji.codepointIsEmoji(codepoint) -> {
                    if (!lastWasZwj) count++
                    lastWasZwj = false
                }
                else -> return null
            }
        }
        return count
    }

    fun useGigamojiForMessage(content: String): GigamojiState {
        if (content.isBlank()) return GigamojiState.None
        val unicodeCount = unicodeEmojiCount(content) ?: return GigamojiState.None
        val total = unicodeCount + countCustomEmotes(content)
        return when {
            total == 0 -> GigamojiState.None
            total == 1 -> GigamojiState.Single
            else -> GigamojiState.Multiple
        }
    }
}
