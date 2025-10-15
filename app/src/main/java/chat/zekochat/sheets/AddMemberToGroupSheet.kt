package chat.zekochat.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.zekochat.api.PeptideAPI
import chat.zekochat.api.PeptideHttp
import chat.zekochat.api.PeptideJson
import chat.zekochat.api.api
import chat.zekochat.api.internals.FriendRequests
import chat.zekochat.api.routes.channel.addMember
import chat.zekochat.api.routes.channel.removeMember
import chat.zekochat.api.schemas.User
import chat.zekochat.composables.chat.MemberListItem
import chat.zekochat.screens.create.MAX_ADDABLE_PEOPLE_IN_GROUP
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer

class AddMemberToGroupSheetViewModel : ViewModel() {
    var groupMembers = mutableStateListOf<String>()
    var friendSearchQuery by mutableStateOf("")
    var loadingUser by mutableStateOf("")
    var friendsFilteredBySearch = mutableStateListOf<String>()
    var error by mutableStateOf<String?>(null)

    suspend fun fetchGroupParticipants(channelId: String) {
        val response = PeptideHttp.get("/channels/$channelId/members".api())
            .bodyAsText()

        val users = PeptideJson.decodeFromString(
            ListSerializer(User.serializer()),
            response
        ).mapNotNull { it.id }

        groupMembers.clear()
        groupMembers.addAll(users)

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

            it.displayName.contains(friendSearchQuery, ignoreCase = true) ||
                    it.username.contains(friendSearchQuery, ignoreCase = true)
        }.map { it.id!! })
    }

    fun addMemberToGroup(channelId: String, popBackStack: () -> Unit) {
        if (groupMembers.size > MAX_ADDABLE_PEOPLE_IN_GROUP) {
            error = "Too many members, maximum is $MAX_ADDABLE_PEOPLE_IN_GROUP"
            return
        }

        try {
            error = null
            viewModelScope.launch {
                groupMembers.map {
                    CoroutineScope(Dispatchers.IO).launch {
                        addMember(channelId, it)
                    }
                }
                popBackStack()
            }
        } catch (e: Exception) {
            error = e.message
        }
    }

    fun addMemberToGroup(channelId: String, userId: String) {

        if(loadingUser.isNotEmpty()) return

        viewModelScope.launch {
            try {
                loadingUser = userId
                error = null

                addMember(channelId, userId)
                groupMembers.add(userId)

                loadingUser = ""
            } catch (e: Exception) {
                loadingUser = ""
                error = e.message
            } catch (e: Error) {
                loadingUser = ""
                groupMembers.add(userId)
            }
        }
    }

    fun removeMemberFromGroup(channelId: String, userId: String) {

        if(loadingUser.isNotEmpty()) return

        viewModelScope.launch {
            try {
                loadingUser = userId
                error = null

                removeMember(channelId, userId)
                groupMembers.remove(userId)

                loadingUser = ""
            } catch (e: Exception) {
                loadingUser = ""
                error = e.message
            } catch (e: Error) {
                loadingUser = ""
                groupMembers.remove(userId)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberToGroupSheet(
    channelId: String,
    viewModel: AddMemberToGroupSheetViewModel = viewModel(),
    onHideSheet: suspend () -> Unit
)
{

    LaunchedEffect(Unit) {
        viewModel.filterFriends()
        viewModel.fetchGroupParticipants(channelId);
    }

    Column(
        Modifier
            .imePadding()
    )
    {
        AnimatedVisibility(visible = viewModel.error?.isNotBlank() ?: false) {
            Text(
                text = viewModel.error ?: "",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 78.0.dp)) {
            items(viewModel.friendsFilteredBySearch.size) { index ->
                val friend = PeptideAPI.userCache[viewModel.friendsFilteredBySearch[index]]
                    ?: return@items
                val isMember = viewModel.groupMembers.contains(friend.id)
                val isLoading = viewModel.loadingUser == friend.id

                MemberListItem(
                    member = null,
                    user = friend,
                    serverId = null,
                    userId = friend.id!!,
                    modifier = Modifier.clickable {
                        if (isMember) {
                            viewModel.removeMemberFromGroup(channelId, friend.id)
                        } else {
                            viewModel.addMemberToGroup(channelId, friend.id)
                        }
                    },
                    trailingContent = {
                        if(isLoading){
                            CircularProgressIndicator(Modifier.size(24.dp))
                        }else{

                            Checkbox(
                                checked = isMember,
                                onCheckedChange = null,
                                enabled = (isMember.not() && viewModel.groupMembers.size >= MAX_ADDABLE_PEOPLE_IN_GROUP).not()
                            )
                        }
                    }
                )
            }
        }
    }
}