package chat.stoat.screens.chat

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.routes.channel.searchChannel
import chat.stoat.composables.chat.SystemMessage
import chat.stoat.core.model.schemas.Message
import chat.stoat.internals.extensions.zero
import kotlinx.coroutines.launch

class ChannelPinsScreenViewModel : ViewModel() {
    val pinnedMessages = mutableStateListOf<Message>()
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun loadPins(channelId: String) {
        pinnedMessages.clear()
        isLoading = true
        error = null

        viewModelScope.launch {
            try {
                val response =
                    searchChannel(channelId, pinned = true, limit = 100, includeUsers = true)

                response.users?.forEach { user ->
                    user.id?.let { id ->
                        StoatAPI.userCache.putIfAbsent(id, user)
                    }
                }

                response.members?.forEach { member ->
                    member.id?.let { memberId ->
                        if (!StoatAPI.members.hasMember(memberId.server, memberId.user)) {
                            StoatAPI.members.setMember(memberId.server, member)
                        }
                    }
                }

                pinnedMessages.addAll(response.messages ?: emptyList())
            } catch (e: Exception) {
                Log.e("ChannelPinsScreen", "Failed to load pinned messages", e)
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelPinsScreen(
    navController: NavController,
    channelId: String,
    modifier: Modifier = Modifier,
    viewModel: ChannelPinsScreenViewModel = viewModel()
) {
    LaunchedEffect(channelId) {
        viewModel.loadPins(channelId)
    }

    Scaffold(
        topBar = {
            Column {
                AnimatedVisibility(LocalIsConnected.current) {
                    Spacer(
                        Modifier
                            .height(
                                WindowInsets.statusBars.asPaddingValues()
                                    .calculateTopPadding()
                            )
                    )
                }
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.pinned_messages),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.popBackStack()
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back_24dp),
                                contentDescription = stringResource(id = R.string.back)
                            )
                        }
                    }
                )
            }
        },
        contentWindowInsets = WindowInsets.zero
    ) { pv ->
        Box(
            modifier = Modifier
                .padding(pv)
                .fillMaxHeight()
        ) {
            Crossfade(targetState = viewModel.isLoading, label = "pinsLoading") { loading ->
                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    }
                } else if (viewModel.pinnedMessages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = viewModel.error
                                    ?: stringResource(R.string.pinned_messages_empty),
                                color = if (viewModel.error != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
                                modifier = Modifier.padding(horizontal = 64.dp),
                                textAlign = TextAlign.Center
                            )
                            if (viewModel.error != null) {
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = { viewModel.loadPins(channelId) }) {
                                    Text(stringResource(R.string.tap_to_retry))
                                }
                            }
                        }
                    }
                } else {
                    PinnedMessageList(
                        messages = viewModel.pinnedMessages,
                        onMessageSelected = { messageId ->
                            navController.previousBackStackEntry?.savedStateHandle?.apply {
                                set(CHANNEL_MESSAGE_JUMP_CHANNEL_KEY, channelId)
                                set(CHANNEL_MESSAGE_JUMP_MESSAGE_KEY, messageId)
                            }
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun PinnedMessageList(
    messages: List<Message>,
    onMessageSelected: (String) -> Unit,
) {
    LazyColumn(contentPadding = WindowInsets.navigationBars.asPaddingValues()) {
        items(
            messages.size,
            key = { index -> messages[index].id ?: index },
        ) { index ->
            val message = messages[index].copy(tail = false)
            val onClick = {
                message.id?.let(onMessageSelected)
                Unit
            }
            if (message.system != null) {
                SystemMessage(message, onClick = onClick)
            } else {
                chat.stoat.composables.chat.Message(
                    message = message,
                    onClick = onClick,
                )
            }
        }
    }
}
