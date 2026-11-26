package chat.stoat.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ChannelUtils
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.Roles
import chat.stoat.api.internals.has
import chat.stoat.api.routes.channel.leaveGroup
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.composables.screens.chat.ChannelSheetHeader
import chat.stoat.core.designsystem.components.SheetButton
import chat.stoat.core.designsystem.components.StoatButtonDefaults
import chat.stoat.core.designsystem.icons.StoatIcons
import chat.stoat.core.designsystem.icons.painter
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.internals.extensions.rememberChannelPermissions
import chat.stoat.navigation.Route
import chat.stoat.platform.LocalToaster
import chat.stoat.platform.show
import chat.stoat.screens.chat.ChatRouterDestination
import chat.stoat.screens.chat.dialogs.InviteDialog
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.channel_info_sheet_leave_group_confirm
import chat.stoat.shared.resources.channel_info_sheet_leave_group_confirm_eyebrow
import chat.stoat.shared.resources.channel_info_sheet_leave_group_confirm_no
import chat.stoat.shared.resources.channel_info_sheet_leave_group_confirm_yes
import chat.stoat.shared.resources.channel_info_sheet_leave_group_failed
import chat.stoat.shared.resources.channel_info_sheet_leave_group_silently
import chat.stoat.shared.resources.channel_info_sheet_options_add
import chat.stoat.shared.resources.channel_info_sheet_options_invite
import chat.stoat.shared.resources.channel_info_sheet_options_leave_group
import chat.stoat.shared.resources.channel_info_sheet_options_members
import chat.stoat.shared.resources.pinned_messages_view
import chat.stoat.shared.resources.settings
import chat.stoat.shared.resources.unknown
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelInfoSheet(channelId: String, onHideSheet: suspend () -> Unit) {
    val channel = StoatAPI.channelCache[channelId]
    var memberListSheetShown by remember { mutableStateOf(false) }
    var inviteDialogShown by remember { mutableStateOf(false) }
    var leaveGroupDialogShown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val permissions by rememberChannelPermissions(channelId)

    if (memberListSheetShown) {
        val memberListSheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )

        ModalBottomSheet(
            sheetState = memberListSheetState,
            onDismissRequest = {
                memberListSheetShown = false
            }
        ) {
            MemberListSheet(
                channelId = channelId,
                serverId = channel?.server
            )
        }
    }

    if (inviteDialogShown) {
        Dialog(
            onDismissRequest = {
                inviteDialogShown = false
            }
        ) {
            InviteDialog(
                channelId = channelId,
                onDismissRequest = {
                    inviteDialogShown = false
                }
            )
        }
    }

    if (leaveGroupDialogShown && channel != null) {
        LeaveGroupDialog(
            channelId = channelId,
            groupName = channel.name ?: stringResource(Res.string.unknown),
            onDismiss = { leaveGroupDialogShown = false },
            onLeft = {
                leaveGroupDialogShown = false
                scope.launch { onHideSheet() }
                scope.launch { ActionChannel.send(Action.ChatNavigate(ChatRouterDestination.Friends)) }
            },
        )
    }

    if (channel == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val partner = ChannelUtils
        .resolveDMPartner(channel)
        ?.let {
            StoatAPI.userCache[it]
        }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
    ) {
        ChannelSheetHeader(
            channel = channel,
            dmPartner = partner
        )
        HorizontalDivider()
    }

    when (channel.channelType) {
        ChannelType.TextChannel, ChannelType.VoiceChannel, ChannelType.Group -> {
            SheetButton(
                headlineContent = {
                    Text(
                        text = stringResource(Res.string.channel_info_sheet_options_members),
                    )
                },
                leadingContent = {
                    Icon(
                        painter = StoatIcons.Members.painter(),
                        contentDescription = null
                    )
                },
                onClick = {
                    memberListSheetShown = true
                }
            )
        }

        else -> {}
    }

    if (
        Roles.permissionFor(
            channel,
            StoatAPI.userCache[StoatAPI.selfId]
        ) has PermissionBit.InviteOthers
    ) {
        when (channel.channelType) {
            ChannelType.TextChannel, ChannelType.VoiceChannel -> {
                SheetButton(
                    headlineContent = {
                        Text(
                            text = stringResource(Res.string.channel_info_sheet_options_invite),
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = StoatIcons.AddMembers.painter(),
                            contentDescription = null
                        )
                    },
                    onClick = {
                        inviteDialogShown = true
                    }
                )
            }

            ChannelType.Group -> {
                SheetButton(
                    headlineContent = {
                        Text(
                            text = stringResource(Res.string.channel_info_sheet_options_add),
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = StoatIcons.AddMembers.painter(),
                            contentDescription = null
                        )
                    },
                    onClick = {}
                )
            }

            else -> {}
        }
    }

    // FIXME: Waiting on backend!
    /*SheetButton(
        headlineContent = {
            Text(
                text = stringResource(Res.string.channel_info_sheet_options_notifications_manage),
            )
        },
        leadingContent = {
            Icon(
                painter = StoatIcons.NotificationSettings.painter(),
                contentDescription = null
            )
        },
        onClick = {}
    )*/

    SheetButton(
        headlineContent = {
            Text(
                text = stringResource(Res.string.pinned_messages_view),
            )
        },
        leadingContent = {
            Icon(
                painter = StoatIcons.PinnedMessages.painter(),
                contentDescription = null
            )
        },
        onClick = {
            scope.launch {
                onHideSheet()
            }
            scope.launch {
                channel.id?.let { ActionChannel.send(Action.TopNavigate(Route.Channel.Pins(it))) }
            }
        }
    )

    if (
        (permissions has PermissionBit.ManageChannel || permissions has PermissionBit.ManageRole)
        && (channel.channelType != ChannelType.SavedMessages && channel.channelType != ChannelType.DirectMessage)
    ) {
        SheetButton(
            headlineContent = {
                Text(
                    text = stringResource(Res.string.settings),
                )
            },
            leadingContent = {
                Icon(
                    painter = StoatIcons.Settings.painter(),
                    contentDescription = null
                )
            },
            onClick = {
                scope.launch {
                    onHideSheet()
                }
                scope.launch {
                    delay(100) // wait for the sheet to close or at least start closing
                    channel.id?.let {
                        ActionChannel.send(
                            Action.TopNavigate(
                                Route.ChannelSettings.Home(
                                    it
                                )
                            )
                        )
                    }
                }
            }
        )
    }

    if (channel.channelType == ChannelType.Group) {
        SheetButton(
            headlineContent = {
                Text(
                    text = stringResource(Res.string.channel_info_sheet_options_leave_group),
                )
            },
            leadingContent = {
                Icon(
                    painter = StoatIcons.Leave.painter(),
                    contentDescription = null
                )
            },
            dangerous = true,
            onClick = { leaveGroupDialogShown = true }
        )
    }
}

@Composable
private fun LeaveGroupDialog(
    channelId: String,
    groupName: String,
    onDismiss: () -> Unit,
    onLeft: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    val failedText = stringResource(Res.string.channel_info_sheet_leave_group_failed)
    var leaveSilently by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!leaving) onDismiss() },
        title = {
            Text(stringResource(Res.string.channel_info_sheet_leave_group_confirm, groupName))
        },
        text = {
            Column {
                Text(stringResource(Res.string.channel_info_sheet_leave_group_confirm_eyebrow))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Checkbox(
                        checked = leaveSilently,
                        onCheckedChange = { leaveSilently = it },
                        enabled = !leaving,
                    )
                    Text(
                        text = stringResource(Res.string.channel_info_sheet_leave_group_silently),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    leaving = true
                    scope.launch {
                        try {
                            leaveGroup(channelId, leaveSilently)
                            StoatAPI.channelCache.remove(channelId)
                            onLeft()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Throwable) {
                            toaster.show(failedText)
                        } finally {
                            leaving = false
                        }
                    }
                },
                enabled = !leaving,
                colors = StoatButtonDefaults.dangerColors(),
            ) {
                Text(stringResource(Res.string.channel_info_sheet_leave_group_confirm_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !leaving) {
                Text(stringResource(Res.string.channel_info_sheet_leave_group_confirm_no))
            }
        }
    )
}
