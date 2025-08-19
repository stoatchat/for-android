package chat.peptide.composables.screens.settings

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import chat.peptide.R
import chat.peptide.api.PeptideAPI
import chat.peptide.api.routes.user.acceptFriendRequest
import chat.peptide.api.routes.user.blockUser
import chat.peptide.api.routes.user.friendUser
import chat.peptide.api.routes.user.openDM
import chat.peptide.api.routes.user.unblockUser
import chat.peptide.api.routes.user.unfriendUser
import chat.peptide.api.schemas.User
import chat.peptide.callbacks.Action
import chat.peptide.callbacks.ActionChannel
import chat.peptide.composables.generic.SquareButton
import chat.peptide.internals.Platform
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.asLog
import logcat.logcat

@Composable
fun UserButtons(
    user: User,
    dismissSheet: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var botEasterEgg by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

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
        when (user.relationship) {
            "None" -> {
                if (user.bot == null) {
                    SquareButton(
                        onClick = {
                            scope.launch {
                                try {
                                    friendUser("${user.username}#${user.discriminator}")
                                } catch (e: Exception) {
                                    if (e.message == "NoEffect") return@launch
                                    logcat(LogPriority.ERROR) { e.asLog() }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.user_info_sheet_add_friend))
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

            "User" -> {
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

            "Friend" -> {
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            val dm = openDM(user.id)
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
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.user_info_sheet_send_message))
                }
                // Remove friend (in overflow menu)
            }

            "Outgoing" -> {
                SquareButton(
                    onClick = {
                        scope.launch {
                            try {
                                unfriendUser(user.id)
                            } catch (e: Exception) {
                                if (e.message == "NoEffect") return@launch
                                logcat(LogPriority.ERROR) { e.asLog() }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.user_info_sheet_cancel_request))
                }
            }

            "Incoming" -> {
                SquareButton(
                    onClick = {
                        scope.launch {
                            try {
                                acceptFriendRequest(user.id)
                            } catch (e: Exception) {
                                if (e.message == "NoEffect") return@launch
                                logcat(LogPriority.ERROR) { e.asLog() }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.user_info_sheet_accept_request))
                }
                SquareButton(
                    onClick = {
                        scope.launch {
                            try {
                                unfriendUser(user.id)
                            } catch (e: Exception) {
                                if (e.message == "NoEffect") return@launch
                                logcat(LogPriority.ERROR) { e.asLog() }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.user_info_sheet_decline_request))
                }
            }

            "Blocked" -> {
                SquareButton(
                    onClick = {
                        scope.launch {
                            try {
                                unblockUser(user.id)
                            } catch (e: Exception) {
                                if (e.message == "NoEffect") return@launch
                                logcat(LogPriority.ERROR) { e.asLog() }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.user_info_sheet_unblock))
                }
            }

            "BlockedOther" -> Box(Modifier.weight(1f))
        }
    }
}