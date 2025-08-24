package chat.peptide.composables.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chat.peptide.R
import chat.peptide.api.realtime.DisconnectionState
import chat.peptide.api.settings.LoadedSettings
import chat.peptide.ui.theme.Theme

private val NON_MATERIAL_COLOURS = mapOf(
    DisconnectionState.Disconnected to (Color(0xff4E0C0C) to Color(0xffff1744)),
    DisconnectionState.Reconnecting to (Color(0xff5B5300) to Color(0xffffea00)),
    DisconnectionState.Connected to (Color(0xff0E2F10) to Color(0xff00e676))
)

@Composable
private fun DisconnectedNoticeBase(
    background: Color,
    foreground: Color,
    icon: ImageVector,
    text: String,
    canTapToRetry: Boolean = false,
    onRetry: () -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier
                .clickable(enabled = canTapToRetry, onClick = onRetry)
                .fillMaxWidth()
                .background(background)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.padding(end = 8.dp),
                imageVector = icon,
                tint = foreground,
                contentDescription = null
            )
            Text(
                text = text,
                color = foreground,
                fontWeight = FontWeight.Bold
            )
            if (canTapToRetry) {
                Text(
                    text = stringResource(R.string.tap_to_reconnect),
                    color = foreground,
                    modifier = Modifier.padding(start = 8.dp),
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun DisconnectedNotice(state: DisconnectionState, onReconnect: () -> Unit) {
    val retries = remember { mutableIntStateOf(0) }

    LaunchedEffect(state) {
        when (state) {
            DisconnectionState.Disconnected -> {
                if (retries.intValue < 3) {
                    onReconnect()
                    retries.intValue++
                }
            }

            DisconnectionState.Connected -> {
                retries.intValue = 0
            }

            else -> Unit
        }
    }

    val materialColours = mapOf(
        DisconnectionState.Disconnected to (MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError),
        DisconnectionState.Reconnecting to (MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary),
        DisconnectionState.Connected to (MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary)
    )

    val (background, foreground) = when (LoadedSettings.theme) {
        Theme.M3Dynamic -> materialColours[state] ?: (Color.Unspecified to Color.Unspecified)
        else -> NON_MATERIAL_COLOURS[state] ?: (Color.Unspecified to Color.Unspecified)
    }

    when (state) {
        DisconnectionState.Disconnected -> DisconnectedNoticeBase(
            background = background,
            foreground = foreground,
            icon = Icons.Default.Warning,
            text = stringResource(id = R.string.disconnected),
            canTapToRetry = true,
            onRetry = onReconnect
        )

        DisconnectionState.Reconnecting -> DisconnectedNoticeBase(
            background = background,
            foreground = foreground,
            icon = Icons.Default.Refresh,
            text = stringResource(id = R.string.reconnecting)
        )

        DisconnectionState.Connected -> DisconnectedNoticeBase(
            background = background,
            foreground = foreground,
            icon = Icons.Default.Done,
            text = stringResource(id = R.string.reconnected)
        )
    }
}

@Preview
@Composable
private fun DisconnectedNoticePreview() {
    DisconnectedNotice(
        state = DisconnectionState.Disconnected,
        onReconnect = {}
    )
}

@Preview
@Composable
private fun ReconnectingNoticePreview() {
    DisconnectedNotice(
        state = DisconnectionState.Reconnecting,
        onReconnect = {}
    )
}

@Preview
@Composable
private fun ReconnectedNoticePreview() {
    DisconnectedNotice(
        state = DisconnectionState.Connected,
        onReconnect = {}
    )
}
