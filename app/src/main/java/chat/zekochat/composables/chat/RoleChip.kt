package chat.zekochat.composables.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun RoleChip(label: String, brush: Brush, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(
                brush = brush
            )
            .background(
                // darken the background a bit
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
            )
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        Text(
            text = label,
            style = LocalTextStyle.current.copy(
                brush = brush
            )
        )
    }
}
