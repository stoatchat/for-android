package chat.peptide.composables.screens.chat.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import chat.peptide.api.RevoltError
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun ServerInviteHandler(
    inviteCode: String,
    viewModel: DiscoverServersListViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {},
    onJoinSuccess: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var isJoining by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<RevoltError?>(null) }
    
    // Get the UI state
    val uiState by viewModel.uiState.collectAsState()
    
    if (showDialog) {
        ServerInviteDialog(
            isLoading = isJoining,
            invite = uiState.loadedInviteData,
            error = error ?: uiState.loadedError,
            onJoinClick = {
                isJoining = true
                scope.launch {
                    // Don't set the processing server ID here to avoid showing loading in the list
                    viewModel.joinServerWithoutProcessingIndicator(inviteCode).collectLatest { result ->
                        isJoining = false
                        if (result.ok) {
                            showDialog = false
                            viewModel.clearLoadedData()
                            uiState.loadedInviteData?.serverId?.let { serverId ->
                                onJoinSuccess(serverId)
                            }

                        } else {
                            error = result.error
                        }
                    }
                }
            },
            onDismiss = {
                showDialog = false
                viewModel.clearLoadedData()
                onDismiss()
            }
        )
    }
}