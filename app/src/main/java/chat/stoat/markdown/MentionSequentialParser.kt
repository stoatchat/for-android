package chat.stoat.markdown

import chat.stoat.api.internals.isUlid
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.parser.sequentialparsers.RangesListBuilder
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import org.intellij.markdown.parser.sequentialparsers.TokensCache

class MentionSequentialParser(private val content: String) : SequentialParser {
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
                    hops < 5
                ) {
                    lookahead = lookahead.advance()
                    hops++
                }

                if (lookahead.type == MarkdownTokenTypes.GT) {
                    val innerText = content.substring(ltEnd, lookahead.start)

                    when {
                        innerText.length == 27 && innerText[0] == '@' && innerText.substring(1).isUlid() -> {
                            result.withNode(SequentialParser.Node(iterator.index..lookahead.index + 1, USER_MENTION_ELEMENT_TYPE))
                            iterator = lookahead.advance()
                            continue
                        }
                        innerText.length == 27 && innerText[0] == '#' && innerText.substring(1).isUlid() -> {
                            result.withNode(SequentialParser.Node(iterator.index..lookahead.index + 1, CHANNEL_MENTION_ELEMENT_TYPE))
                            iterator = lookahead.advance()
                            continue
                        }
                        innerText.length == 27 && innerText[0] == '%' && innerText.substring(1).isUlid() -> {
                            result.withNode(SequentialParser.Node(iterator.index..lookahead.index + 1, ROLE_MENTION_ELEMENT_TYPE))
                            iterator = lookahead.advance()
                            continue
                        }
                        innerText == "@EVERYONE" || innerText == "@ONLINE" -> {
                            result.withNode(SequentialParser.Node(iterator.index..lookahead.index + 1, MASS_MENTION_ELEMENT_TYPE))
                            iterator = lookahead.advance()
                            continue
                        }
                    }
                }
            }
            delegateIndices.put(iterator.index)
            iterator = iterator.advance()
        }

        return result.withFurtherProcessing(delegateIndices.get())
    }
}
