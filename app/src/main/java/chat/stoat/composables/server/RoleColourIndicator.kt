package chat.stoat.composables.server

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import chat.stoat.api.internals.BrushCompat

@Composable
fun RoleColourIndicator(
    colour: String?,
    modifier: Modifier = Modifier,
) {
    if (colour != null) {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(BrushCompat.parseColour(colour)),
        )
    } else {
        val outline = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        Canvas(modifier) {
            val stroke = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(3.dp.toPx(), 2.dp.toPx()),
                ),
            )
            drawCircle(
                color = outline,
                radius = size.minDimension / 2 - stroke.width / 2,
                style = stroke,
            )
        }
    }
}
