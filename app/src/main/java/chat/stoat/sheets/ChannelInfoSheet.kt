package chat.stoat.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.ChannelUtils
import chat.stoat.api.internals.PermissionBit
import chat.stoat.api.internals.Roles
import chat.stoat.api.internals.has
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.composables.generic.SheetButton
import chat.stoat.composables.screens.chat.ChannelSheetHeader
import chat.stoat.core.model.schemas.ChannelType
import chat.stoat.internals.extensions.rememberChannelPermissions
import chat.stoat.screens.chat.dialogs.InviteDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelInfoSheet(channelId: String, onHideSheet: suspend () -> Unit) {
    val channel = StoatAPI.channelCache[channelId]
    var memberListSheetShown by remember { mutableStateOf(false) }
    var inviteDialogShown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val permissions by rememberChannelPermissions(channelId)

    if (memberListSheetShown) {
        val memberListSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                        text = stringResource(id = R.string.channel_info_sheet_options_members),
                    )
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_list_24dp),
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
                            text = stringResource(id = R.string.channel_info_sheet_options_invite),
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_add_24dp),
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
                            text = stringResource(id = R.string.channel_info_sheet_options_add),
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_add_24dp),
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
                text = stringResource(id = R.string.channel_info_sheet_options_notifications_manage),
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_notification_settings_24dp),
                contentDescription = null
            )
        },
        onClick = {}
    )*/

    SheetButton(
        headlineContent = {
            Text(
                text = stringResource(id = R.string.pinned_messages_view),
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_pinboard_24dp),
                contentDescription = null
            )
        },
        onClick = {
            scope.launch {
                onHideSheet()
            }
            scope.launch {
                ActionChannel.send(Action.TopNavigate("channel/${channel.id}/pins"))
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
                    text = stringResource(id = R.string.settings),
                )
            },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_settings_24dp),
                    contentDescription = null
                )
            },
            onClick = {
                scope.launch {
                    onHideSheet()
                }
                scope.launch {
                    delay(100) // wait for the sheet to close or at least start closing
                    ActionChannel.send(Action.TopNavigate("settings/channel/${channel.id}"))
                }
            }
        )
    }


}
