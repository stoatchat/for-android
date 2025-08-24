package chat.peptide.markdown.jbm.sequentialparsers

import chat.peptide.markdown.jbm.RSMElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.parser.sequentialparsers.RangesListBuilder
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import org.intellij.markdown.parser.sequentialparsers.TokensCache

class UserMentionParser : SequentialParser {
    override fun parse(
        tokens: TokensCache,
        rangesToGlue: List<IntRange>
    ): SequentialParser.ParsingResult {
        val result = SequentialParser.ParsingResultBuilder()
        val delegateIndices = RangesListBuilder()
        var iterator: TokensCache.Iterator = tokens.RangesListIterator(rangesToGlue)

        while (iterator.type != null) {
            if (iterator.type == MarkdownTokenTypes.LT && iterator.charLookup(1) == '@') {
                val start = iterator.index
                while (iterator.type != MarkdownTokenTypes.GT && iterator.type != null) {
                    iterator = iterator.advance()
                }
                if (iterator.type == MarkdownTokenTypes.GT) {
                    result.withNode(
                        SequentialParser.Node(
                            start..iterator.index + 1,
                            RSMElementTypes.USER_MENTION
                        )
                    )
                }
            } else {
                delegateIndices.put(iterator.index)
            }
            iterator = iterator.advance()
        }

        return result.withFurtherProcessing(delegateIndices.get())
    }
}