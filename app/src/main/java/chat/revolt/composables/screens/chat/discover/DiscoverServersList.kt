package chat.revolt.composables.screens.chat.discover

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.routes.googlesheets.ServerData

@Composable
fun DiscoverServersList(
    onJoinToServerSuccess: (String) -> Unit,
) {
    val viewModel = hiltViewModel<DiscoverServersListViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    // Handle server invite dialog
    uiState.selectedInviteCode?.let { inviteCode ->
        // Find the server with this invite code
        val selectedServer = uiState.servers.find { it.inviteCode == inviteCode }
        selectedServer?.let { server ->
            ServerInviteHandler(
                inviteCode = inviteCode,
                viewModel = viewModel,
                onDismiss = {
                    viewModel.setSelectedInviteCode(null)
                },
                onJoinSuccess = { serverId ->
                    onJoinToServerSuccess(serverId)
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .padding(top = 24.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier
                .padding(8.dp),
            painter = painterResource(R.drawable.discover_character_image),
            contentDescription = null,
        )

        Text(
            text = stringResource(R.string.discover_servers),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.discover_servers_description),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }

            uiState.error != null -> {
                Text(stringResource(R.string.error))
            }

            uiState.servers.isEmpty() -> {
                Text(stringResource(R.string.no_servers_found))
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    items(uiState.servers) { server ->
                        // Check if the server is already joined
                        val isJoined = isServerAlreadyJoined(server.id)

                        ServerItem(
                            server = server,
                            isProcessing = uiState.processingServerId == server.id,
                            isJoined = isServerAlreadyJoined(server.id),
                            onClick = {
                                if (isJoined) {
                                    // If already joined, navigate directly to server channels
                                    onJoinToServerSuccess(server.id)
                                } else if (server.inviteCode.isNotEmpty()) {
                                    // If not joined, load server data and show invite dialog
                                    viewModel.loadServerDataAndShowDialog(
                                        server.inviteCode,
                                        server.id
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun isServerAlreadyJoined(serverId: String): Boolean {
    // Check if the server exists in RevoltAPI.serverCache
    return RevoltAPI.serverCache.containsKey(serverId)
}

@Composable
fun ServerItem(
    server: ServerData,
    isJoined: Boolean,
    onClick: () -> Unit,
    isProcessing: Boolean = false
) {
    // Safely convert the hex string to a Color, or return null if invalid
    fun parseColorOrNull(hex: String?): Color? {
        return hex?.let { raw ->
            val normalized = if (raw.startsWith("#")) raw else "#$raw"
            try {
                Color(normalized.toColorInt())
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }

    // Determine card colors based on server.showColor
    val containerColor = parseColorOrNull(server.showColor)
    val cardColors = if (containerColor != null) {
        CardDefaults.cardColors(containerColor = containerColor)
    } else {
        CardDefaults.cardColors()
    }

    // Calculate alpha values based on disabled state
    val contentAlpha = if (server.disabled) 0.5f else 1.0f
    val descriptionAlpha = if (server.disabled) 0.4f else 0.7f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(enabled = !isProcessing) { onClick() },
        colors = cardColors,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.three_person),
                contentDescription = "",
                colorFilter = ColorFilter.tint(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = contentAlpha)
                )
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(
                        alpha = contentAlpha
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = server.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(
                        alpha = descriptionAlpha,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = onClick,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        if (isJoined) {
                            // Show tick icon with green background for joined servers
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary) // Green color
                                    .align(Alignment.Center),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    modifier = Modifier.size(16.dp),
                                    painter = painterResource(R.drawable.icn_check_24dp),
                                    contentDescription = "Already joined",
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                                )
                            }
                        } else {
                            // Show arrow icon for servers not joined yet
                            Image(
                                painter = painterResource(R.drawable.icn_arrow_forward_24dp),
                                contentDescription = "Join server",
                                modifier = Modifier
                                    .align(Alignment.Center),
                                colorFilter = ColorFilter.tint(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = contentAlpha)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ServerItemPreview() {
    val server = ServerData(
        id = "1",
        name = "Revolt Lounge",
        description = "The official Revolt server. Hang out, chat, and get help with Revolt.",
        inviteCode = "revolt",
        disabled = false,
        showColor = null
    )
    ServerItem(
        server = server,
        onClick = {},
        isJoined = false,
        isProcessing = false
    )
}

@Preview
@Composable
private fun ColorfulServerItemPreview() {
    val server = ServerData(
        id = "1",
        name = "Revolt Lounge",
        description = "The official Revolt server. Hang out, chat, and get help with Revolt.",
        inviteCode = "revolt",
        disabled = false,
        showColor = "#1591ea"
    )
    ServerItem(
        server = server,
        onClick = {},
        isJoined = false,
        isProcessing = false
    )
}


@Preview
@Composable
private fun JoinedServerItemPreview() {
    val server = ServerData(
        id = "1",
        name = "Revolt Lounge",
        description = "The official Revolt server. Hang out, chat, and get help with Revolt.",
        inviteCode = "revolt",
        disabled = false,
        showColor = null,
    )
    ServerItem(
        server = server,
        onClick = {},
        isJoined = true,
        isProcessing = false
    )
}