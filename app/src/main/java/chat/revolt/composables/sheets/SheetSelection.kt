package chat.revolt.composables.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import chat.revolt.R

/**
 * Sheet selection. Used when a modal sheet prompts a choice out of x options.
 * The choices have an extended icon, a title and a description.
 * An end-facing chevron is shown on the end side of the row.
 */
@Composable
fun SheetSelection(
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    description: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    arrowTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        icon()
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
                .then(modifier)
        ) {
            ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                title()
            }
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                description()
            }
        }
        Icon(
            painter = painterResource(R.drawable.icn_keyboard_arrow_right_24dp),
            contentDescription = null,
            tint = arrowTint
        )
    }
}