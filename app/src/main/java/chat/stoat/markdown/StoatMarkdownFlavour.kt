package chat.stoat.markdown

import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import org.intellij.markdown.parser.sequentialparsers.SequentialParserManager

class StoatMarkdownFlavour(private val content: String) : GFMFlavourDescriptor() {
    override val sequentialParserManager = object : SequentialParserManager() {
        override fun getParserSequence(): List<SequentialParser> {
            return listOf(
                MentionSequentialParser(content),
                TimestampSequentialParser(content),
                CustomEmoteSequentialParser(content),
            ) + super@StoatMarkdownFlavour.sequentialParserManager.getParserSequence()
        }
    }
}
