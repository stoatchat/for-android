package chat.stoat.markdown

import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.parser.sequentialparsers.RangesListBuilder
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import org.intellij.markdown.parser.sequentialparsers.TokensCache

class TimestampSequentialParser(private val content: String) : SequentialParser {
    override fun parse(tokens: TokensCache, rangesToGlue: List<IntRange>): SequentialParser.ParsingResult {
        val result = SequentialParser.ParsingResultBuilder()
        val delegateIndices = RangesListBuilder()
        var iterator: TokensCache.Iterator = tokens.RangesListIterator(rangesToGlue)

        while (iterator.type != null) {
            if (iterator.type == MarkdownTokenTypes.LT) {
                val ltEnd = iterator.end

                var lookahead = iterator.advance()
                var hops = 0
                while (lookahead.type != null &&
                    lookahead.type != MarkdownTokenTypes.GT &&
                    lookahead.type != MarkdownTokenTypes.EOL &&
                    hops < 15
                ) {
                    lookahead = lookahead.advance()
                    hops++
                }

                if (lookahead.type == MarkdownTokenTypes.GT) {
                    val innerText = content.substring(ltEnd, lookahead.start)
                    if (isTimestamp(innerText)) {
                        result.withNode(SequentialParser.Node(iterator.index..lookahead.index + 1, TIMESTAMP_ELEMENT_TYPE))
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

    private fun isTimestamp(inner: String): Boolean {
        if (!inner.startsWith("t:")) return false
        val rest = inner.removePrefix("t:")
        val colonIdx = rest.indexOf(':')
        return if (colonIdx == -1) {
            rest.all { it.isDigit() } && rest.isNotEmpty()
        } else {
            val digits = rest.substring(0, colonIdx)
            val style = rest.substring(colonIdx + 1)
            digits.all { it.isDigit() } && digits.isNotEmpty() && style.length == 1
        }
    }
}
