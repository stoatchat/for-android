package chat.revolt.composables.screens.chat.discover

import androidx.compose.foundation.Image
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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import chat.revolt.R
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
            .padding(vertical = 24.dp, horizontal = 16.dp),
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
                        ServerItem(
                            server = server,
                            isProcessing = uiState.processingServerId == server.id,
                            onClick = { 
                                if (server.inviteCode.isNotEmpty()) {
                                    // First load server data, dialog will be shown after data is loaded
                                    viewModel.loadServerDataAndShowDialog(server.inviteCode, server.id)
                                }
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(96.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ServerItem(
    server: ServerData,
    onClick: () -> Unit,
    isProcessing: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(enabled = !isProcessing) { onClick() }
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
                colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.onBackground)
            )
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = server.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(
                        alpha = 0.7f,
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
                        Image(
                            painter = painterResource(R.drawable.icn_arrow_forward_24dp),
                            contentDescription = "",
                            modifier = Modifier
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}
