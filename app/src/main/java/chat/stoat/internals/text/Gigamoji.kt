package chat.stoat.internals.text

private val STRING_IS_CUSTOM_EMOTES_REGEX = Regex("(?::[0-9A-HJKMNP-TV-Z]{26}:|\\s)+")
private val CUSTOM_EMOTE_REGEX = Regex(":[0-9A-HJKMNP-TV-Z]{26}:")
private const val VARIATION_SELECTOR_16 = 0xFE0F
private const val COMBINING_ENCLOSING_KEYCAP = 0x20E3

private fun isKeycapBase(codepoint: Int): Boolean =
    codepoint == '#'.code || codepoint == '*'.code || codepoint in '0'.code..'9'.code

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

        val codepoints = content.codePoints().toArray()
        var count = 0
        var lastWasZwj = false
        var index = 0
        while (index < codepoints.size) {
            val codepoint = codepoints[index]
            when {
                Character.isWhitespace(codepoint) -> lastWasZwj = false
                codepoint == 0x200D -> lastWasZwj = true
                isKeycapBase(codepoint) -> {
                    val selectorOffset =
                        if (codepoints.getOrNull(index + 1) == VARIATION_SELECTOR_16) 1 else 0
                    if (codepoints.getOrNull(index + selectorOffset + 1) !=
                        COMBINING_ENCLOSING_KEYCAP
                    ) {
                        return null
                    }

                    if (!lastWasZwj) count++
                    lastWasZwj = false
                    index += selectorOffset + 1
                }
                codepoint == 0xFE0F || codepoint == 0xFE0E || codepoint == 0x20E3 -> Unit
                codepoint in PUA_MIN..PUA_MAX -> Unit
                codepoint in 0x1F3FB..0x1F3FF -> lastWasZwj = false
                MessageProcessor.emoji.codepointIsEmoji(codepoint) -> {
                    if (!lastWasZwj) count++
                    lastWasZwj = false
                }
                else -> return null
            }
            index++
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
