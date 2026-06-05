package chat.stoat.markdown

import chat.stoat.api.internals.isUlid
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.parser.sequentialparsers.RangesListBuilder
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import org.intellij.markdown.parser.sequentialparsers.TokensCache

class CustomEmoteSequentialParser(private val content: String) : SequentialParser {

    override fun parse(
        tokens: TokensCache,
        rangesToGlue: List<IntRange>,
    ): SequentialParser.ParsingResult {
        val result = SequentialParser.ParsingResultBuilder()
        val delegateIndices = RangesListBuilder()
        var iterator: TokensCache.Iterator = tokens.RangesListIterator(rangesToGlue)

        while (iterator.type != null) {
            if (iterator.type == MarkdownTokenTypes.COLON) {
                val openEnd = iterator.end

                var lookahead = iterator.advance()
                var hops = 0
                while (lookahead.type != null &&
                    lookahead.type != MarkdownTokenTypes.COLON &&
                    lookahead.type != MarkdownTokenTypes.EOL &&
                    hops < 30
                ) {
                    lookahead = lookahead.advance()
                    hops++
                }

                if (lookahead.type == MarkdownTokenTypes.COLON) {
                    val innerText = content.substring(openEnd, lookahead.start)
                    if (innerText.isUlid()) {
                        result.withNode(
                            SequentialParser.Node(
                                iterator.index..lookahead.index + 1,
                                CUSTOM_EMOTE_ELEMENT_TYPE
                            )
                        )
                        iterator = lookahead.advance()
                        continue
                    }
                }
            }
            delegateIndices.put(iterator.index)
            iterator = iterator.advance()
        }

        return result.withFurtherProcessing(delegateIndices.get())
    }
}
