package chat.stoat.markdown

import org.intellij.markdown.parser.sequentialparsers.RangesListBuilder
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import org.intellij.markdown.parser.sequentialparsers.TokensCache

class SpoilerSequentialParser : SequentialParser {
    override fun parse(
        tokens: TokensCache,
        rangesToGlue: List<IntRange>,
    ): SequentialParser.ParsingResult {
        val result = SequentialParser.ParsingResultBuilder()
        val delegateIndices = RangesListBuilder()
        var iterator: TokensCache.Iterator = tokens.RangesListIterator(rangesToGlue)

        while (iterator.type != null) {
            if (iterator.type == SPOILER_DELIMITER_TOKEN_TYPE) {
                val opening = iterator
                var closing = iterator.advance()

                while (closing.type != null) {
                    if (
                        closing.type == SPOILER_DELIMITER_TOKEN_TYPE &&
                        closing.start >= opening.end
                    ) {
                        var inner = opening.advance()
                        while (inner.type != null && inner.index < closing.index) {
                            delegateIndices.put(inner.index)
                            inner = inner.advance()
                        }
                        result.withNode(
                            SequentialParser.Node(
                                opening.index..closing.index + 1,
                                SPOILER_ELEMENT_TYPE,
                            )
                        )
                        iterator = closing.advance()
                        break
                    }
                    closing = closing.advance()
                }

                if (iterator != opening) continue
            }

            delegateIndices.put(iterator.index)
            iterator = iterator.advance()
        }

        return result.withFurtherProcessing(delegateIndices.get())
    }
}
