package chat.revolt.composables.screens.chat.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.revolt.api.RevoltError
import chat.revolt.api.schemas.Invite
import chat.revolt.api.schemas.RsResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun ServerInviteHandler(
    inviteCode: String,
    serverId: String,
    viewModel: DiscoverServersListViewModel = viewModel(),
    onDismiss: () -> Unit = {},
    onJoinSuccess: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var isJoining by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<RevoltError?>(null) }
    
    // Get the pre-loaded invite data
    val loadedInviteData by viewModel.loadedInviteData.collectAsState()
    
    if (showDialog && loadedInviteData != null) {
        ServerInviteDialog(
            isLoading = isJoining,
            invite = loadedInviteData,
            error = error,
            onJoinClick = {
                isJoining = true
                scope.launch {
                    viewModel.joinServer(inviteCode).collectLatest { result ->
                        isJoining = false
                        if (result.ok) {
                            showDialog = false
                            viewModel.clearLoadedData()
                            onJoinSuccess()
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