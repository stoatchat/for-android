package chat.peptide.markdown.jbm.sequentialparsers

import chat.peptide.markdown.jbm.RSMElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.parser.sequentialparsers.RangesListBuilder
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import org.intellij.markdown.parser.sequentialparsers.TokensCache

class CustomEmoteParser : SequentialParser {
    override fun parse(
        tokens: TokensCache,
        rangesToGlue: List<IntRange>
    ): SequentialParser.ParsingResult {
        val result = SequentialParser.ParsingResultBuilder()
        val delegateIndices = RangesListBuilder()
        var iterator: TokensCache.Iterator = tokens.RangesListIterator(rangesToGlue)

        while (iterator.type != null) {
            if (iterator.type == MarkdownTokenTypes.COLON) {

                val endIterator = findNextColon(iterator.advance())

                if (endIterator != null) {
                    result.withNode(
                        SequentialParser.Node(
                            iterator.index..endIterator.index + 1,
                            RSMElementTypes.CUSTOM_EMOTE
                        )
                    )
                    iterator = endIterator.advance()
                    continue
                }
            }
            delegateIndices.put(iterator.index)
            iterator = iterator.advance()
        }

        return result.withFurtherProcessing(delegateIndices.get())
    }

    private fun findNextColon(it: TokensCache.Iterator): TokensCache.Iterator? {
        var iterator = it
        while (iterator.type != null) {
            if (iterator.type == MarkdownTokenTypes.COLON) {
                return iterator
            }

            iterator = iterator.advance()
        }
        return null
    }
}