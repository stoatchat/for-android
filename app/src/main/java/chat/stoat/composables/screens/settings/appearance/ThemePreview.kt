package chat.stoat.composables.screens.settings.appearance

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val PreviewShape = RoundedCornerShape(10.dp)

/**
 * A theme option: a tiny, text-free mock screen drawn in [scheme]'s colours (an app bar, a few list
 * rows and an accent element) with the theme's [label] underneath.
 */
@Composable
fun ThemePreview(
    scheme: ColorScheme,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val ring by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
    )
    val labelColour by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Canvas(
            Modifier
                .border(2.dp, ring, RoundedCornerShape(12.dp))
                .padding(4.dp)
                .size(width = 64.dp, height = 96.dp)
                .clip(PreviewShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, PreviewShape)
                .alpha(if (enabled) 1f else 0.4f)
        ) {
            drawMockScreen(scheme)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColour.copy(alpha = if (enabled) 1f else 0.5f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
        )
    }
}

private fun DrawScope.drawMockScreen(scheme: ColorScheme) {
    val unit = size.width / 16f
    drawRect(scheme.background)

    drawRoundRect(
        color = scheme.onBackground.copy(alpha = 0.85f),
        topLeft = Offset(unit * 2, unit * 2.5f),
        size = Size(unit * 6, unit * 1.2f),
        cornerRadius = CornerRadius(unit * 0.6f),
    )

    val rowsTop = unit * 6.5f
    val rowHeight = unit * 3.6f
    val rowWidth = size.width - unit * 3
    val rows = 3

    repeat(rows) { index ->
        val top = rowsTop + index * (rowHeight + unit * 0.5f)
        val radius = when (index) {
            0, rows - 1 -> unit * 1.6f
            else -> unit * 0.5f
        }
        drawRoundRect(
            color = scheme.surfaceContainer,
            topLeft = Offset(unit * 1.5f, top),
            size = Size(rowWidth, rowHeight),
            cornerRadius = CornerRadius(radius),
        )
        drawRowContent(scheme, top, rowHeight, unit)
    }

    val accentSize = unit * 3.4f
    drawRoundRect(
        color = scheme.primary,
        topLeft = Offset(
            size.width - unit * 1.5f - accentSize * 1.4f,
            size.height - unit * 1.5f - accentSize
        ),
        size = Size(accentSize * 1.4f, accentSize),
        cornerRadius = CornerRadius(accentSize / 2),
    )
}

private fun DrawScope.drawRowContent(
    scheme: ColorScheme,
    top: Float,
    rowHeight: Float,
    unit: Float
) {
    val centreY = top + rowHeight / 2
    drawCircle(
        color = scheme.secondaryContainer,
        radius = unit * 0.9f,
        center = Offset(unit * 3.5f, centreY),
    )
    drawRoundRect(
        color = scheme.onSurface.copy(alpha = 0.7f),
        topLeft = Offset(unit * 5.2f, centreY - unit * 0.45f),
        size = Size(unit * 6.5f, unit * 0.9f),
        cornerRadius = CornerRadius(unit * 0.45f),
    )
}
