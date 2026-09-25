package chat.stoat.internals.extensions

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

fun Modifier.blockSwipeReplyOnHorizontalDrag() = composed {
    val nestedScrollDispatcher = remember { NestedScrollDispatcher() }
    val nestedScrollConnection = remember { object : NestedScrollConnection {} }

    nestedScroll(nestedScrollConnection, nestedScrollDispatcher)
        .pointerInput(nestedScrollDispatcher) {
            awaitEachGesture {
                val down = awaitFirstDown(
                    pass = PointerEventPass.Initial,
                    requireUnconsumed = false,
                )
                var horizontalDistance = 0f

                do {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val deltaX = change.position.x - change.previousPosition.x
                    horizontalDistance += deltaX

                    if (abs(horizontalDistance) > viewConfiguration.touchSlop) {
                        nestedScrollDispatcher.dispatchPostScroll(
                            consumed = Offset(deltaX, 0f),
                            available = Offset.Zero,
                            source = NestedScrollSource.UserInput,
                        )
                    }
                } while (event.changes.any { it.pressed })
            }
        }
}

fun Modifier.supportSwipeReply(
    pass: PointerEventPass = PointerEventPass.Main,
    onDown: (pointer: PointerInputChange) -> Unit,
    onMove: (changes: List<PointerInputChange>) -> Unit,
    onUp: () -> Unit,
) = this.then(
    Modifier.pointerInput(pass) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = pass, requireUnconsumed = false)
            onDown(down)
            do {
                val event: PointerEvent = awaitPointerEvent(
                    pass = pass
                )

                onMove(event.changes)

            } while (event.changes.any { it.pressed })
            onUp()
        }
    }
)
