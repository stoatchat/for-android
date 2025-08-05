package chat.revolt.composables.screens.chat.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    viewModel: DiscoverServersListViewModel = viewModel(),
    onDismiss: () -> Unit = {},
    onJoinSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(true) }
    var inviteResult by remember { mutableStateOf<RsResult<Invite, RevoltError>?>(null) }
    var error by remember { mutableStateOf<RevoltError?>(null) }
    
    // Fetch server invite info
    LaunchedEffect(inviteCode) {
        viewModel.loadServerInviteInfo(inviteCode).collectLatest { result ->
            inviteResult = result
            isLoading = false
            
            if (result.err) {
                error = result.error
            }
        }
    }
    
    if (showDialog) {
        ServerInviteDialog(
            isLoading = isLoading,
            invite = inviteResult?.value,
            error = error,
            onJoinClick = {
                isLoading = true
                scope.launch {
                    viewModel.joinServer(inviteCode).collectLatest { result ->
                        isLoading = false
                        if (result.ok) {
                            showDialog = false
                            onJoinSuccess()
                        } else {
                            error = result.error
                        }
                    }
                }
            },
            onDismiss = {
                showDialog = false
                onDismiss()
            }
        )
    }
}