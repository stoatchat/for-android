package chat.zekochat.markdown.jbm.sequentialparsers

import org.intellij.markdown.parser.sequentialparsers.RangesListBuilder
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import org.intellij.markdown.parser.sequentialparsers.TokensCache

class MassMentionParser : SequentialParser {
    override fun parse(
        tokens: TokensCache,
        rangesToGlue: List<IntRange>
    ): SequentialParser.ParsingResult {
        // TODO - Implement
        val result = SequentialParser.ParsingResultBuilder()
        val delegateIndices = RangesListBuilder()
        return result.withFurtherProcessing(delegateIndices.get())
    }
}
