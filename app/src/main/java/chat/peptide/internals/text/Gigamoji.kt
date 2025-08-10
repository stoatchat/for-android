package chat.peptide.internals.text

private val STRING_IS_CUSTOM_EMOTES_REGEX = Regex("(?::[0-9A-HJKMNP-TV-Z]{26}:|\\s)+")

object Gigamoji {
    private fun stringIsEntirelyCustomEmotes(content: String): Boolean {
        return STRING_IS_CUSTOM_EMOTES_REGEX.matches(content)
    }

    private fun stringIsEntirelyUnicodeEmojis(unfilteredContent: String): Boolean {
        // Remove all custom emotes
        val content = unfilteredContent.replace(STRING_IS_CUSTOM_EMOTES_REGEX, "")

        // the message is solely custom emotes
        if (content.isBlank()) return true

        if ("[0-9A-Za-z#*]".toRegex()
                .containsMatchIn(content)
        ) { // reject common non-emoji characters
            return false
        }

        for (codepoint in content.codePoints()) {
            if (MessageProcessor.emoji.codepointIsEmoji(codepoint)) {
                return true
            }
        }

        return false
    }

    fun useGigamojiForMessage(content: String): Boolean {
        if (content.isBlank()) return false
        return stringIsEntirelyCustomEmotes(content) || stringIsEntirelyUnicodeEmojis(content)
    }
}