package chat.zekochat.composables.settings.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.zekochat.R
import chat.zekochat.composables.generic.Presence
import chat.zekochat.composables.generic.presenceColour

fun Presence.stringResource(): Int {
    return when (this) {
        Presence.Online -> R.string.status_online
        Presence.Idle -> R.string.status_idle
        Presence.Dnd -> R.string.status_dnd
        Presence.Focus -> R.string.status_focus
        Presence.Offline -> R.string.status_invisible
    }
}

fun Presence.explainerResource(): Int? {
    return when (this) {
        Presence.Online -> null
        Presence.Idle -> null
        Presence.Dnd -> R.string.status_dnd_explainer
        Presence.Focus -> R.string.status_focus_explainer
        Presence.Offline -> R.string.status_invisible_explainer
    }
}

@Composable
fun StatusPicker(
    currentStatus: Presence,
    onStatusChange: (Presence) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.status),
            style = MaterialTheme.typography.labelLarge
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Presence.entries.forEach {
                StatusButton(
                    presence = it,
                    selected = it == currentStatus,
                    onClick = onStatusChange,
                    modifier = modifier
                )
            }
        }

        Text(
            text = stringResource(currentStatus.stringResource()),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold
            )
        )

        currentStatus.explainerResource()?.let {
            Text(
                text = stringResource(it),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StatusButton(
    presence: Presence,
    selected: Boolean,
    onClick: (Presence) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable { onClick(presence) }
            .then(
                if (selected) {
                    Modifier.background(
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .size(24.dp)
                .background(presenceColour(presence))
        )
    }
}
