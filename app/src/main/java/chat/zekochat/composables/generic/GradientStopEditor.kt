package chat.zekochat.composables.generic

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import logcat.logcat

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun GradientStopEditor(
    segments: Int,
    stops: List<Pair<Float, Color>>,
    onStopsChanged: (List<Pair<Float, Color>>) -> Unit,
    _onClickSegment: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Our gradient stop editor uses zero-width "segments" to represent points where the stops can be placed.
    // There are two segments, one at the start and one at the end, that are not visible to the user.
    // They only exist so that the user can place stops at the very beginning and end of the gradient without
    // having to tap at the edge of the screen.
    Canvas(
        modifier
            .padding(horizontal = 16.dp)
            .pointerInput(segments) {
                detectTapGestures { offset ->
                    val segment = (offset.x / size.width * segments).toInt()
                    logcat { "Tapped segment $segment" }
                }
            }
            .fillMaxWidth()
    ) {
        // Debug code for now
        val debugStops = mutableListOf(Color.Transparent)
        for (i in 1 until segments + 1) {
            if (i % 2 == 0) {
                debugStops.add(Color(0xff366fd1))
            } else {
                debugStops.add(Color(0xff36d1af))
            }
        }
        debugStops.add(Color.Transparent)

        drawRect(
            brush = Brush.linearGradient(debugStops, Offset(0f, 0f), Offset(size.width, 0f)),
            size = size,
        )
    }
}