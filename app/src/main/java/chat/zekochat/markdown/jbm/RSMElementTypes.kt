package chat.zekochat.markdown.jbm

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementType

object RSMElementTypes {
    @JvmField
    val USER_MENTION: IElementType = MarkdownElementType("USER_MENTION")

    @JvmField
    val CHANNEL_MENTION: IElementType = MarkdownElementType("CHANNEL_MENTION")

    @JvmField
    val ROLE_MENTION: IElementType = MarkdownElementType("ROLE_MENTION")
    
    @JvmField
    val MASS_MENTION: IElementType = MarkdownElementType("MASS_MENTION")

    @JvmField
    val CUSTOM_EMOTE: IElementType = MarkdownElementType("EMOJI")

    @JvmField
    val TIMESTAMP: IElementType = MarkdownElementType("TIMESTAMP")
}