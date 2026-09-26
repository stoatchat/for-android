package chat.stoat.composables.screens.chat.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import chat.stoat.api.settings.ServerSidebarEntry
import chat.stoat.core.model.schemas.Server
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.roundToInt

private const val FOLD_BAND = 0.5f
private val CancelMargin = 80.dp
private val AutoScrollEdge = 64.dp
private val AutoScrollMaxSpeed = 24.dp
internal val RailItemGap = 6.dp

sealed interface RailRow {
    val key: String
    val parent: String?

    data class ServerRow(
        val server: Server,
        override val parent: String? = null,
    ) : RailRow {
        override val key: String get() = server.id!!
    }

    data class FolderRow(val entry: ServerSidebarEntry.Folder) : RailRow {
        override val key: String get() = entry.id
        override val parent: String? get() = null
    }
}

fun List<ServerSidebarEntry>.toRailRows(): List<RailRow> = flatMap { entry ->
    when (entry) {
        is ServerSidebarEntry.Single -> listOf(RailRow.ServerRow(entry.server))
        is ServerSidebarEntry.Folder -> buildList<RailRow> {
            add(RailRow.FolderRow(entry))
            if (entry.folder.collapsed != true) {
                entry.servers.forEach { add(RailRow.ServerRow(it, parent = entry.id)) }
            }
        }
    }
}

sealed interface RailIntent {
    data class Fold(val target: String) : RailIntent
    data class Move(val before: String?, val parent: String?) : RailIntent
}

@Stable
class RailDragState(
    private val listState: LazyListState,
    private val stickyHeaderKey: Any? = null,
) {
    var rows: List<RailRow> = emptyList()
        set(value) {
            field = value
            rowIndex = value.withIndex().associate { it.value.key to it.index }
        }
    private var rowIndex: Map<String, Int> = emptyMap()

    var held by mutableStateOf<String?>(null)
        private set
    var pointer by mutableStateOf(Offset.Zero)
        private set
    var intent by mutableStateOf<RailIntent?>(null)
        private set

    private var moved = false

    internal var railWidthPx = 0f
    internal var railHeightPx = 0f

    private data class RowBox(val index: Int, val top: Float, val bottom: Float)

    private fun boxes(): List<RowBox> =
        listState.layoutInfo.visibleItemsInfo.mapNotNull { info ->
            val index = rowIndex[info.key] ?: return@mapNotNull null
            RowBox(index, info.offset.toFloat(), (info.offset + info.size).toFloat())
        }.sortedBy { it.index }

    fun rowAt(y: Float): RailRow? {
        val header = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == stickyHeaderKey }
        if (header != null && y <= header.offset + header.size) return null
        return boxes().firstOrNull { y >= it.top && y <= it.bottom }?.let { rows[it.index] }
    }

    internal fun start(key: String, position: Offset) {
        held = key
        pointer = position
        intent = null
        moved = false
    }

    internal fun moveTo(position: Offset, cancelMarginPx: Float) {
        moved = true
        pointer = position
        refresh(cancelMarginPx)
    }

    internal fun refresh(cancelMarginPx: Float) {
        val key = held ?: return
        intent = intentAt(pointer, key, cancelMarginPx)
    }

    internal fun finish(): Pair<String, RailIntent?>? {
        val result = held?.let { it to intent }
        held = null
        intent = null
        return result
    }

    /**
     * Port of `intentAt` in Stoat for Web `railDrag.ts`.
     */
    private fun intentAt(position: Offset, key: String, cancelMarginPx: Float): RailIntent? {
        if (position.x < -cancelMarginPx || position.x > railWidthPx + cancelMarginPx) return null

        val source = rows.getOrNull(rowIndex[key] ?: -1)
        val canFold = source is RailRow.ServerRow
        val canNest = source is RailRow.ServerRow
        val boxes = boxes()
        if (boxes.isEmpty()) return RailIntent.Move(null, null)

        fun insertAt(index: Int): RailIntent {
            var neighbour = index
            while (!canNest && rows.getOrNull(neighbour)?.parent != null) neighbour++
            val row = rows.getOrNull(neighbour)
            return RailIntent.Move(row?.key, row?.parent)
        }

        val y = position.y
        for ((i, box) in boxes.withIndex()) {
            val limit = boxes.getOrNull(i + 1)?.top ?: box.bottom
            if (y < box.top || y > limit) continue

            val row = rows[box.index]
            val offset = ((y - box.top) / (box.bottom - box.top)).coerceAtMost(1f)
            if (row.key != key && canFold && row.parent == null &&
                abs(offset - 0.5f) < FOLD_BAND / 2
            ) {
                return RailIntent.Fold(row.key)
            }
            return insertAt(if (offset >= 0.5f) box.index + 1 else box.index)
        }

        return if (y < boxes.first().top) insertAt(boxes.first().index) else insertAt(boxes.last().index + 1)
    }

    /**
     * Returns null when the drop would leave everything where it was
     * Port of `drop` in Stoat for Web `railDrag.ts`.
     */
    fun resolveDrop(key: String, intent: RailIntent?): RailIntent? {
        when (intent) {
            null -> return null
            is RailIntent.Fold -> return intent.takeIf { it.target != key }
            is RailIntent.Move -> {
                val source = rows.getOrNull(rowIndex[key] ?: -1) ?: return null
                if (intent.parent == source.parent &&
                    (intent.before == key || sitsBefore(key, intent.before))
                ) {
                    return null
                }
                return intent
            }
        }
    }

    private fun sitsBefore(key: String, before: String?): Boolean {
        val at = rowIndex[key] ?: return false
        var next = at + 1
        while (next < rows.size && rows[next].parent == key) next++
        return if (before == null) next == rows.size else rows.getOrNull(next)?.key == before
    }

    internal fun autoScrollDelta(maxSpeedPx: Float, edgePx: Float): Float {
        if (!moved) return 0f
        val y = pointer.y
        return when {
            y < edgePx -> -maxSpeedPx * (1f - (y / edgePx).coerceIn(0f, 1f))
            y > railHeightPx - edgePx ->
                maxSpeedPx * ((y - (railHeightPx - edgePx)) / edgePx).coerceIn(0f, 1f)

            else -> 0f
        }
    }
}

fun Modifier.railDragGestures(
    state: RailDragState,
    haptics: HapticFeedback,
    onLongPress: (RailRow) -> Unit,
    onDrop: (key: String, intent: RailIntent) -> Unit,
): Modifier = composed {
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnDrop by rememberUpdatedState(onDrop)

    pointerInput(state) {
        val cancelMarginPx = CancelMargin.toPx()

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val row = state.rowAt(down.position.y) ?: return@awaitEachGesture

            val abandoned = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                var abandoned = false
                while (!abandoned) {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes
                        .firstOrNull { it.id == down.id }
                    abandoned = change == null || !change.pressed || change.isConsumed ||
                            (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                }
                true
            }
            if (abandoned != null) return@awaitEachGesture

            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            state.start(row.key, down.position)

            var moved = false
            var released = false
            try {
                while (true) {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes
                        .firstOrNull { it.id == down.id } ?: break
                    change.consume()
                    if (!change.pressed) {
                        released = true
                        break
                    }
                    if (!moved &&
                        (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                    ) {
                        moved = true
                    }
                    if (moved) state.moveTo(change.position, cancelMarginPx)
                }
            } finally {
                val (key, intent) = state.finish() ?: (null to null)
                if (released && key != null) {
                    if (!moved) {
                        currentOnLongPress(row)
                    } else {
                        state.resolveDrop(key, intent)?.let { currentOnDrop(key, it) }
                    }
                }
            }
        }
    }
}

@Composable
fun RailDragAutoScroll(state: RailDragState, listState: LazyListState) {
    val density = LocalDensity.current
    val edgePx = with(density) { AutoScrollEdge.toPx() }
    val maxSpeedPx = with(density) { AutoScrollMaxSpeed.toPx() }
    val cancelMarginPx = with(density) { CancelMargin.toPx() }

    LaunchedEffect(state.held) {
        if (state.held == null) return@LaunchedEffect
        while (isActive && state.held != null) {
            val delta = state.autoScrollDelta(maxSpeedPx, edgePx)
            if (delta != 0f) {
                listState.scrollBy(delta)
                state.refresh(cancelMarginPx)
            }
            withFrameNanos { }
        }
    }
}

@Composable
fun RailDragOverlay(
    state: RailDragState,
    listState: LazyListState,
    ghost: @Composable (RailRow) -> Unit,
) {
    val held = state.held ?: return
    val row = state.rows.firstOrNull { it.key == held } ?: return

    (state.intent as? RailIntent.Move)?.let { move ->
        val items = listState.layoutInfo.visibleItemsInfo
        val lineY = if (move.before != null) {
            items.firstOrNull { it.key == move.before }?.offset?.toFloat()
        } else {
            items.lastOrNull { item -> state.rows.any { it.key == item.key } }
                ?.let { (it.offset + it.size).toFloat() }
        }
        lineY?.let { y ->
            Box(
                Modifier
                    .offset {
                        IntOffset(0, (y - RailItemGap.toPx() / 2 - 2.dp.toPx()).roundToInt())
                    }
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            )
        }
    }

    Box(
        Modifier
            .offset {
                IntOffset(
                    (state.pointer.x - 24.dp.toPx()).roundToInt(),
                    (state.pointer.y - 24.dp.toPx()).roundToInt()
                )
            }
            .size(48.dp)
            .alpha(0.85f)
    ) {
        ghost(row)
    }
}
