package chat.stoat.markdown

import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.lexer.MarkdownLexer
import org.intellij.markdown.parser.sequentialparsers.EmphasisLikeParser
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import org.intellij.markdown.parser.sequentialparsers.SequentialParserManager

class StoatMarkdownFlavour(private val content: String) : GFMFlavourDescriptor() {
    override fun createInlinesLexer(): MarkdownLexer = MarkdownLexer(SpoilerLexer())

    override val sequentialParserManager = object : SequentialParserManager() {
        // We need to do it dynamically like this to ensure that this does not break with JB Markdown updates
        override fun getParserSequence(): List<SequentialParser> {
            val upstream = super@StoatMarkdownFlavour.sequentialParserManager.getParserSequence()
            val emphasisIndices = upstream.indices.filter { upstream[it] is EmphasisLikeParser }
            check(emphasisIndices.size == 1) {
                "Expected exactly one upstream emphasis parser, found ${emphasisIndices.size}"
            }
            val emphasisIndex = emphasisIndices.single()

            return listOf(
                MentionSequentialParser(content),
                TimestampSequentialParser(content),
                CustomEmoteSequentialParser(content),
            ) + upstream.take(emphasisIndex) + SpoilerSequentialParser() +
                    upstream.drop(emphasisIndex)
        }
    }
}
