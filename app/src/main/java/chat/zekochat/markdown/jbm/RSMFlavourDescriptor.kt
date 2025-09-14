package chat.zekochat.markdown.jbm

import chat.zekochat.markdown.jbm.sequentialparsers.ChannelMentionParser
import chat.zekochat.markdown.jbm.sequentialparsers.CustomEmoteParser
import chat.zekochat.markdown.jbm.sequentialparsers.MassMentionParser
import chat.zekochat.markdown.jbm.sequentialparsers.RoleMentionParser
import chat.zekochat.markdown.jbm.sequentialparsers.UserMentionParser
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.flavours.gfm.StrikeThroughDelimiterParser
import org.intellij.markdown.parser.sequentialparsers.EmphasisLikeParser
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import org.intellij.markdown.parser.sequentialparsers.SequentialParserManager
import org.intellij.markdown.parser.sequentialparsers.impl.AutolinkParser
import org.intellij.markdown.parser.sequentialparsers.impl.BacktickParser
import org.intellij.markdown.parser.sequentialparsers.impl.EmphStrongDelimiterParser
import org.intellij.markdown.parser.sequentialparsers.impl.ImageParser
import org.intellij.markdown.parser.sequentialparsers.impl.InlineLinkParser
import org.intellij.markdown.parser.sequentialparsers.impl.MathParser
import org.intellij.markdown.parser.sequentialparsers.impl.ReferenceLinkParser

class RSMFlavourDescriptor : GFMFlavourDescriptor() {
    override val sequentialParserManager = object : SequentialParserManager() {
        override fun getParserSequence(): List<SequentialParser> {
            return listOf(
                UserMentionParser(),
                ChannelMentionParser(),
                RoleMentionParser(),
                MassMentionParser(),
                CustomEmoteParser(),
                AutolinkParser(listOf(MarkdownTokenTypes.AUTOLINK, GFMTokenTypes.GFM_AUTOLINK)),
                BacktickParser(),
                MathParser(),
                ImageParser(),
                InlineLinkParser(),
                ReferenceLinkParser(),
                EmphasisLikeParser(EmphStrongDelimiterParser(), StrikeThroughDelimiterParser())
            )
        }
    }
}