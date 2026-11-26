package chat.stoat.composables.markdown.prose

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.mikepenz.markdown.model.State
import io.ratex.DisplayList
import io.ratex.RaTeXEngine
import io.ratex.measure
import kotlin.coroutines.cancellation.CancellationException

private const val MAX_CACHED_FORMULAS = 512

// Hard-cap ratex blocks size so adversarial expressions can't crash the layout engine
private const val MAX_MATH_DIMENSION_PX = 4096f

internal data class MathKey(val latex: String, val displayMode: Boolean)

/**
 * Background cache for KaTeX formulas, shared among [ChatMarkdown]s
 */
internal object MathCache {
    private val layouts = mutableStateMapOf<MathKey, DisplayList>()
    private val coloured = mutableStateMapOf<Pair<MathKey, Int>, DisplayList>()

    fun size(key: MathKey, fontSizePx: Float): Size? =
        layouts[key]
            ?.measure(fontSizePx)
            ?.let {
                Size(
                    it.widthPx.coerceIn(0f, MAX_MATH_DIMENSION_PX),
                    it.totalHeightPx.coerceIn(0f, MAX_MATH_DIMENSION_PX),
                )
            }

    fun coloured(key: MathKey, colourArgb: Int): DisplayList? = coloured[key to colourArgb]

    suspend fun parseLayout(key: MathKey) {
        if (key in layouts) return
        putLayout(key, parseOrNull(key, Color.Black) ?: return)
    }

    private fun putLayout(key: MathKey, displayList: DisplayList) {
        if (key in layouts) return
        if (layouts.size >= MAX_CACHED_FORMULAS) layouts.clear()
        layouts[key] = displayList
    }

    suspend fun parseColoured(key: MathKey, colourArgb: Int): DisplayList? {
        coloured[key to colourArgb]?.let { return it }
        val displayList = parseOrNull(key, Color(colourArgb)) ?: return null
        if (coloured.size >= MAX_CACHED_FORMULAS) coloured.clear()
        coloured[key to colourArgb] = displayList
        putLayout(key, displayList)
        return displayList
    }

    private suspend fun parseOrNull(key: MathKey, colour: Color): DisplayList? = try {
        RaTeXEngine.parse(key.latex, key.displayMode, colour)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}

/**
 * Lay math out ahead of time
 */
suspend fun prepareChatMarkdownMath(state: State) {
    if (state !is State.Success) return
    collectMathEntries(state.node, state.content)
        .map { MathKey(it.latex, it.displayMode) }
        .distinct()
        .forEach { MathCache.parseLayout(it) }
}
