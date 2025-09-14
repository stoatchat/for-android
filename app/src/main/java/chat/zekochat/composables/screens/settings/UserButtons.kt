package chat.zekochat.composables.screens.settings

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.zekochat.R
import chat.zekochat.api.PeptideAPI
import chat.zekochat.api.routes.user.acceptFriendRequest
import chat.zekochat.api.routes.user.friendUser
import chat.zekochat.api.routes.user.openDM
import chat.zekochat.api.routes.user.unfriendUser
import chat.zekochat.api.schemas.User
import chat.zekochat.callbacks.Action
import chat.zekochat.callbacks.ActionChannel
import chat.zekochat.composables.generic.SquareButton
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.asLog
import logcat.logcat

private const val REL_NONE: String = "None"
private const val REL_USER: String = "User"
private const val REL_FRIEND: String = "Friend"
private const val REL_OUTGOING: String = "Outgoing"
private const val REL_INCOMING: String = "Incoming"
private const val REL_BLOCKED: String = "Blocked"
private const val REL_BLOCKED_OTHER: String = "BlockedOther"

@Composable
fun UserButtons(
    user: User,
    dismissSheet: suspend () -> Unit,
    onRelationshipChanged: (String?) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var botEasterEgg by remember { mutableStateOf(false) }
    var isAddFriendLoading by remember { mutableStateOf(false) }
    var isOpenDMLoading by remember { mutableStateOf(false) }
    var isCancelRequestLoading by remember { mutableStateOf(false) }
    var isAcceptRequestLoading by remember { mutableStateOf(false) }
    var isDeclineRequestLoading by remember { mutableStateOf(false) }

    // Always derive latest user from cache to reflect relationship changes
    val latestUser: User = user.id?.let { id -> PeptideAPI.userCache[id] } ?: user
    val relationship: String = latestUser.relationship ?: REL_NONE

    if (user.id == null) return Row {
        SquareButton(
            onClick = {
                scope.launch {
                    try {
                        friendUser("${user.username}#${user.discriminator}")
                    } catch (e: Exception) {
                        // Button did nothing, but not an error
                        if (e.message == "NoEffect") return@launch

                        // Log all other errors
                        logcat(LogPriority.ERROR) { e.asLog() }
                    }
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.user_info_sheet_add_friend))
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (relationship) {
            REL_NONE -> {
                if (latestUser.bot == null) {
                    SquareButton(
                        onClick = {
                            scope.launch {
                                try {
                                    isAddFriendLoading = true
                                    friendUser("${latestUser.username}#${latestUser.discriminator}")
                                    latestUser.id?.let { id ->
                                        val updated = latestUser.copy(relationship = REL_OUTGOING)
                                        PeptideAPI.userCache[id] =
                                            (PeptideAPI.userCache[id]?.copy(relationship = REL_OUTGOING))
                                                ?: updated
                                        onRelationshipChanged(REL_OUTGOING)
                                    }
                                } catch (e: Exception) {
                                    if (e.message == "NoEffect") return@launch
                                    logcat(LogPriority.ERROR) { e.asLog() }
                                } finally {
                                    isAddFriendLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isAddFriendLoading
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.user_info_sheet_add_friend))
                            if (isAddFriendLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                            }
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            8.dp,
                            alignment = Alignment.Start
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .animateContentSize()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { botEasterEgg = true }
                            .padding(8.dp)
                            .weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.icn_smart_toy_24dp),
                            contentDescription = null
                        )
                        Text(
                            if (botEasterEgg) {
                                stringResource(R.string.user_info_sheet_user_is_bot_easter_egg)
                            } else {
                                stringResource(R.string.user_info_sheet_user_is_bot)
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            REL_USER -> {
                SquareButton(
                    onClick = {
                        scope.launch {
                            ActionChannel.send(Action.TopNavigate("settings/profile"))
                            // We must now close the bottom sheet,
                            // else we will crash if we try to open this sheet again
                            dismissSheet()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.user_info_sheet_edit_profile))
                }
            }

            REL_FRIEND -> {
                FilledTonalButton(
                    onClick = {
                        latestUser.id?.let {
                            scope.launch {
                                try {
                                    isOpenDMLoading = true
                                    val dm = openDM(latestUser.id)
                                    if (dm.id != null) {
                                        if (PeptideAPI.channelCache[dm.id] == null)
                                            PeptideAPI.channelCache[dm.id] = dm
                                        ActionChannel.send(Action.SwitchChannel(dm.id))
                                        dismissSheet()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.user_info_sheet_failed_to_open_dm),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } finally {
                                    isOpenDMLoading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isOpenDMLoading
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.user_info_sheet_send_message))
                        if (isOpenDMLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                        }
                    }
                }
                // Remove friend (in overflow menu)
            }

            REL_OUTGOING -> {
                SquareButton(
                    onClick = {
                        latestUser.id?.let {
                            scope.launch {
                                try {
                                    isCancelRequestLoading = true
                                    unfriendUser(latestUser.id)
                                    val updated = latestUser.copy(relationship = REL_NONE)
                                    PeptideAPI.userCache[latestUser.id] =
                                        (PeptideAPI.userCache[latestUser.id]?.copy(relationship = REL_NONE))
                                            ?: updated
                                    onRelationshipChanged(REL_NONE)
                                } catch (e: Exception) {
                                    if (e.message == "NoEffect") return@launch
                                    logcat(LogPriority.ERROR) { e.asLog() }
                                } finally {
                                    isCancelRequestLoading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isCancelRequestLoading
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.user_info_sheet_cancel_request))
                        if (isCancelRequestLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                        }
                    }
                }
            }

            REL_INCOMING -> {
                SquareButton(
                    onClick = {
                        latestUser.id?.let {
                            scope.launch {
                                try {
                                    isAcceptRequestLoading = true
                                    acceptFriendRequest(latestUser.id)
                                    val updated = latestUser.copy(relationship = REL_FRIEND)
                                    PeptideAPI.userCache[latestUser.id] =
                                        (PeptideAPI.userCache[latestUser.id]?.copy(relationship = REL_FRIEND))
                                            ?: updated
                                    onRelationshipChanged(REL_FRIEND)
                                } catch (e: Exception) {
                                    if (e.message == "NoEffect") return@launch
                                    logcat(LogPriority.ERROR) { e.asLog() }
                                } finally {
                                    isAcceptRequestLoading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isAcceptRequestLoading
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.user_info_sheet_accept_request))
                        if (isAcceptRequestLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                        }
                    }
                }
                SquareButton(
                    onClick = {
                        latestUser.id?.let {
                            scope.launch {
                                try {
                                    isDeclineRequestLoading = true
                                    unfriendUser( latestUser.id)
                                    val updated = latestUser.copy(relationship = REL_NONE)
                                    PeptideAPI.userCache[latestUser.id] =
                                        (PeptideAPI.userCache[latestUser.id]?.copy(relationship = REL_NONE))
                                            ?: updated
                                    onRelationshipChanged(REL_NONE)
                                } catch (e: Exception) {
                                    if (e.message == "NoEffect") return@launch
                                    
                                    logcat(LogPriority.ERROR) { e.asLog() }
                                } finally {
                                    isDeclineRequestLoading = false
                                }
                            }
                        }

                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.weight(1f),
                    enabled = !isDeclineRequestLoading
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.user_info_sheet_decline_request))
                        if (isDeclineRequestLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                        }
                    }
                }
            }
            REL_BLOCKED, REL_BLOCKED_OTHER -> Box(Modifier.weight(1f))
            else -> Box(Modifier.weight(1f))
        }
    }
}