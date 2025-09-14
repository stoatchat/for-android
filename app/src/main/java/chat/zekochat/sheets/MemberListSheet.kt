package chat.zekochat.sheets

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.zekochat.R
import chat.zekochat.api.PeptideAPI
import chat.zekochat.api.internals.PermissionBit
import chat.zekochat.api.internals.Roles
import chat.zekochat.api.internals.hasPermission
import chat.zekochat.api.routes.channel.fetchGroupParticipants
import chat.zekochat.api.routes.server.fetchMembers
import chat.zekochat.api.schemas.Member
import chat.zekochat.api.schemas.User
import chat.zekochat.composables.chat.MemberListItem
import chat.zekochat.composables.generic.CountableListHeader
import chat.zekochat.composables.generic.Presence
import chat.zekochat.composables.generic.SheetHeaderPadding
import chat.zekochat.composables.generic.presenceFromStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

val DO_NOT_FETCH_OFFLINE_MEMBERS_SERVERS = listOf(
    "01F7ZSBSFHQ8TA81725KQCSDDP" // Peptide Lounge
)

sealed class MemberListSheetItem {
    data class MemberItem(val member: Member) : MemberListSheetItem()
    data class UserItem(val user: User) : MemberListSheetItem()
    data class CategoryItem(val category: String, val count: Int) : MemberListSheetItem()
}

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class MemberListSheetViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    val fullItemList = mutableStateListOf<MemberListSheetItem>()

    fun fetchServerMemberList(serverId: String, channelId: String) {
        viewModelScope.launch {
            val memberList = fetchMembers(
                serverId = serverId,
                includeOffline = serverId !in DO_NOT_FETCH_OFFLINE_MEMBERS_SERVERS
            ).members
            val channel = PeptideAPI.channelCache[channelId] ?: return@launch

            val categories = mutableMapOf<String, List<Member>>()

            val offlineCategoryName = context.getString(R.string.status_offline)
            val defaultCategoryName = context.getString(R.string.status_online)

            memberList.forEach { member ->
                val user = PeptideAPI.userCache[member.id!!.user] ?: run {
                    Log.w(
                        "MemberListSheet",
                        "User ${member.id.user} found in member list of server $serverId but not in user cache"
                    )
                    return@forEach
                }

                if (user.online == false) {
                    categories[offlineCategoryName] =
                        (categories[offlineCategoryName] ?: listOf()) + member
                    return@forEach
                }

                val highestHoistedRole =
                    Roles.resolveHighestRole(serverId, member.id.user, hoisted = true)

                val category = if (highestHoistedRole != null) {
                    highestHoistedRole.name ?: context.getString(R.string.unknown)
                } else {
                    defaultCategoryName
                }

                if (!Roles.permissionFor(channel, user, member)
                        .hasPermission(PermissionBit.ViewChannel)
                ) {
                    return@forEach
                }

                categories[category] = (categories[category] ?: listOf()) + member
            }

            fullItemList.clear()

            // Hoisted roles
            Roles.inOrder(serverId) { it.hoist == true }.forEach { role ->
                val members = categories[role.name] ?: return@forEach
                fullItemList.add(MemberListSheetItem.CategoryItem(role.name ?: "", members.size))
                members.forEach { member ->
                    fullItemList.add(MemberListSheetItem.MemberItem(member))
                }
            }

            // Online
            if (!categories[defaultCategoryName].isNullOrEmpty()) {
                fullItemList.add(
                    MemberListSheetItem.CategoryItem(
                        defaultCategoryName,
                        categories[defaultCategoryName]?.size ?: 0
                    )
                )
                categories[defaultCategoryName]?.forEach { member ->
                    fullItemList.add(MemberListSheetItem.MemberItem(member))
                }
            }

            // Offline
            if (!categories[offlineCategoryName].isNullOrEmpty()) {
                fullItemList.add(
                    MemberListSheetItem.CategoryItem(
                        offlineCategoryName,
                        categories[offlineCategoryName]?.size ?: 0
                    )
                )
                categories[offlineCategoryName]?.forEach { member ->
                    fullItemList.add(MemberListSheetItem.MemberItem(member))
                }
            }
        }
    }

    fun fetchGroupMemberList(channelId: String) {
        viewModelScope.launch {
            val userList = fetchGroupParticipants(channelId)

            val onlinePredicate = { user: User ->
                presenceFromStatus(
                    user.status?.presence,
                    user.online ?: false
                ) != Presence.Offline
            }
            val offlinePredicate = { user: User ->
                presenceFromStatus(
                    user.status?.presence,
                    user.online ?: false
                ) == Presence.Offline
            }

            fullItemList.clear()

            if (userList.count(onlinePredicate) > 0) {
                fullItemList.add(
                    MemberListSheetItem.CategoryItem(
                        context.getString(R.string.status_online),
                        userList.count(onlinePredicate)
                    )
                )

                userList.filter(onlinePredicate).forEach { user ->
                    fullItemList.add(MemberListSheetItem.UserItem(user))
                }
            }

            if (userList.count(offlinePredicate) > 0) {
                fullItemList.add(
                    MemberListSheetItem.CategoryItem(
                        context.getString(R.string.status_offline),
                        userList.count(offlinePredicate)
                    )
                )

                userList.filter(offlinePredicate).forEach { user ->
                    fullItemList.add(MemberListSheetItem.UserItem(user))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MemberListSheet(
    channelId: String,
    serverId: String? = null,
    viewModel: MemberListSheetViewModel = hiltViewModel()
) {
    var showUserInfoSheet by remember { mutableStateOf(false) }
    var userInfoSheetTarget by remember { mutableStateOf("") }
    var showMemberContextSheet by remember { mutableStateOf(false) }
    var memberContextSheetTarget by remember { mutableStateOf("") }

    // We use LaunchedEffect to make sure that this is called every time any of the users status changes
    LaunchedEffect(PeptideAPI.userCache) {
        snapshotFlow { PeptideAPI.userCache }.distinctUntilChanged().collect {
            if (serverId != null) {
                viewModel.fetchServerMemberList(serverId, channelId)
            } else {
                viewModel.fetchGroupMemberList(channelId)
            }
        }
    }

    if (showUserInfoSheet) {
        val userContextSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

        ModalBottomSheet(
            sheetState = userContextSheetState,
            onDismissRequest = {
                showUserInfoSheet = false
            }
        ) {
            UserInfoSheet(
                userId = userInfoSheetTarget,
                dismissSheet = {
                    userContextSheetState.hide()
                    showUserInfoSheet = false
                }
            )
        }
    }

    if (showMemberContextSheet) {
        val memberContextSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = memberContextSheetState,
            onDismissRequest = {
                showMemberContextSheet = false
            }
        ) {
            if (serverId != null) {
                ServerMemberContextSheet(
                    userId = memberContextSheetTarget,
                    serverId = serverId,
                    channelId = channelId,
                    onRequestUpdateMembers = {
                        viewModel.fetchServerMemberList(serverId, channelId)
                    },
                    dismissSheet = {
                        memberContextSheetState.hide()
                        showMemberContextSheet = false
                    }
                )
            } else {
                GroupDMMemberContextSheet(
                    userId = memberContextSheetTarget,
                    channelId = channelId,
                    onRequestUpdateMembers = {
                        viewModel.fetchGroupMemberList(channelId)
                    },
                    dismissSheet = {
                        memberContextSheetState.hide()
                        showMemberContextSheet = false
                    }
                )
            }

        }
    }

    Column(Modifier.animateContentSize()) {
        if (viewModel.fullItemList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            return@Column
        }

        SheetHeaderPadding {
            Text(
                text = stringResource(R.string.channel_info_sheet_options_members),
                style = MaterialTheme.typography.headlineSmall
            )
        }

        LazyColumn {
            viewModel.fullItemList.forEachIndexed { index, item ->
                when (item) {
                    is MemberListSheetItem.CategoryItem -> stickyHeader(
                        key = "${item.category}-$index"
                    ) {
                        CountableListHeader(
                            text = item.category,
                            count = item.count,
                            backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    }

                    is MemberListSheetItem.MemberItem -> item(key = item.member.id!!.user) {
                        MemberListItem(
                            user = PeptideAPI.userCache[item.member.id.user],
                            member = item.member,
                            serverId = serverId,
                            userId = item.member.id.user,
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = {
                                        userInfoSheetTarget = item.member.id.user
                                        showUserInfoSheet = true
                                    },
                                    onClickLabel = stringResource(R.string.user_info_sheet_open),
                                    onLongClick = {
                                        memberContextSheetTarget = item.member.id.user
                                        showMemberContextSheet = true
                                    },
                                    onLongClickLabel = stringResource(R.string.member_context_sheet_open)
                                )
                        )
                    }

                    is MemberListSheetItem.UserItem -> item(key = item.user.id!!) {
                        MemberListItem(
                            user = item.user,
                            member = null,
                            serverId = serverId,
                            userId = item.user.id,
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    userInfoSheetTarget = item.user.id
                                    showUserInfoSheet = true
                                },
                                onClickLabel = stringResource(R.string.user_info_sheet_open),
                                onLongClick = {
                                    memberContextSheetTarget = item.user.id
                                    showMemberContextSheet = true
                                },
                                onLongClickLabel = stringResource(R.string.member_context_sheet_open)
                            )
                        )
                    }
                }
            }
        }
    }

}