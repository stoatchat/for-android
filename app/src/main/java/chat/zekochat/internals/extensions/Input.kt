package chat.zekochat.internals.extensions

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput

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