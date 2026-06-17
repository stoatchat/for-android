package chat.stoat.screens.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.activities.StoatTweenFloat
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.FriendRequests
import chat.stoat.api.routes.channel.createGroupDM
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.composables.chat.MemberListItem
import kotlinx.coroutines.launch

const val MAX_PEOPLE_IN_GROUP = 50
const val MAX_ADDABLE_PEOPLE_IN_GROUP = MAX_PEOPLE_IN_GROUP - 1

class CreateGroupScreenViewModel : ViewModel() {
    var groupName by mutableStateOf("")
    var groupMembers = mutableStateListOf<String>()
    var friendSearchQuery by mutableStateOf("")
    var friendsFilteredBySearch = mutableStateListOf<String>()
    var error by mutableStateOf<String?>(null)

    fun updateFriendSearchQuery(query: String) {
        friendSearchQuery = query
        filterFriends()
    }

    fun filterFriends() {
        friendsFilteredBySearch.clear()
        friendsFilteredBySearch.addAll(FriendRequests.getFriends().filter {
            if (friendSearchQuery.isBlank()) {
                return@filter true
            }

            if (it.displayName == null || it.username == null) {
                return@filter false
            }

            it.displayName!!.contains(friendSearchQuery, ignoreCase = true) ||
                    it.username!!.contains(friendSearchQuery, ignoreCase = true)
        }.map { it.id!! })
    }

    fun createGroup(popBackStack: () -> Unit) {
        if (groupMembers.size > MAX_ADDABLE_PEOPLE_IN_GROUP) {
            error = "Too many members, maximum is $MAX_ADDABLE_PEOPLE_IN_GROUP"
            return
        }

        try {
            error = null
            viewModelScope.launch {
                val channel = createGroupDM(groupName, groupMembers)
                popBackStack()
                channel.id?.let { ActionChannel.send(Action.SwitchChannel(it)) }
            }
        } catch (e: Exception) {
            error = e.message
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    navController: NavController,
    viewModel: CreateGroupScreenViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.filterFriends()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.create_group),
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
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = viewModel.groupName.isNotBlank() && viewModel.groupMembers.isNotEmpty(),
                enter = scaleIn(animationSpec = StoatTweenFloat),
                exit = scaleOut(animationSpec = StoatTweenFloat)
            ) {
                FloatingActionButton(onClick = { viewModel.createGroup(navController::popBackStack) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_24dp),
                        contentDescription = stringResource(R.string.create_group_action)
                    )
                }
            }
        }
    ) { pv ->
        Column(
            Modifier
                .padding(pv)
                .imePadding()
        ) {
            Text(
                text = stringResource(R.string.create_group_description, MAX_PEOPLE_IN_GROUP),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            AnimatedVisibility(visible = viewModel.error?.isNotBlank() ?: false) {
                Text(
                    text = viewModel.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
            TextField(
                value = viewModel.groupName,
                onValueChange = { viewModel.groupName = it },
                label = { Text(stringResource(R.string.create_group_name)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
            )
            OutlinedTextField(
                value = viewModel.friendSearchQuery,
                onValueChange = { viewModel.updateFriendSearchQuery(it) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search_24dp),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(R.string.create_group_search)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 78.0.dp)) {
                items(viewModel.friendsFilteredBySearch.size) { index ->
                    val friend = StoatAPI.userCache[viewModel.friendsFilteredBySearch[index]]
                        ?: return@items
                    val isMember = viewModel.groupMembers.contains(friend.id)
                    val isLast = index == viewModel.friendsFilteredBySearch.size - 1

                    MemberListItem(
                        member = null,
                        user = friend,
                        serverId = null,
                        userId = friend.id!!,
                        first = index == 0,
                        last = isLast,
                        onClick = {
                            if (isMember) {
                                viewModel.groupMembers.remove(friend.id)
                            } else {
                                if (viewModel.groupMembers.size < MAX_ADDABLE_PEOPLE_IN_GROUP) {
                                    viewModel.groupMembers.add(friend.id!!)
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        trailingContent = {
                            Checkbox(
                                checked = isMember,
                                onCheckedChange = null,
                                enabled = (isMember.not() && viewModel.groupMembers.size >= MAX_ADDABLE_PEOPLE_IN_GROUP).not()
                            )
                        }
                    )
                    if (!isLast) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}