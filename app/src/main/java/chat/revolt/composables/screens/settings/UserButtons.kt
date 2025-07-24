package chat.revolt.composables.screens.settings

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.routes.user.acceptFriendRequest
import chat.revolt.api.routes.user.blockUser
import chat.revolt.api.routes.user.friendUser
import chat.revolt.api.routes.user.openDM
import chat.revolt.api.routes.user.unblockUser
import chat.revolt.api.routes.user.unfriendUser
import chat.revolt.api.schemas.User
import chat.revolt.callbacks.Action
import chat.revolt.callbacks.ActionChannel
import chat.revolt.internals.Platform
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
        Button(
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
                    Button(
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
                Button(
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
                                if (RevoltAPI.channelCache[dm.id] == null)
                                    RevoltAPI.channelCache[dm.id] = dm
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
                Button(
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
                Button(
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
                Button(
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
                Button(
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

        if (user.relationship != "User") {
            Row { // Prevent the dropdown menu from counting towards arrangement spacing
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    when (user.relationship) {
                        "Friend" -> {
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.user_info_sheet_remove_friend))
                                },
                                onClick = {
                                    scope.launch {
                                        try {
                                            unfriendUser(user.id)
                                        } catch (e: Exception) {
                                            if (e.message == "NoEffect") return@launch
                                            logcat(LogPriority.ERROR) { e.asLog() }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    when (user.relationship) {
                        "Blocked" -> {}

                        else -> DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.user_info_sheet_block))
                            },
                            onClick = {
                                scope.launch {
                                    try {
                                        blockUser(user.id)
                                    } catch (e: Exception) {
                                        if (e.message == "NoEffect") return@launch
                                        logcat(LogPriority.ERROR) { e.asLog() }
                                    }
                                }
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.user_info_sheet_copy_id))
                        },
                        onClick = {
                            scope.launch {
                                clipboard.setText(AnnotatedString(user.id))
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.user_info_sheet_report))
                        },
                        onClick = {
                            scope.launch {
                                ActionChannel.send(Action.ReportUser(user.id))

                                if (Platform.needsShowClipboardNotification()) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.copied),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                }

                IconButton(
                    onClick = {
                        menuOpen = true
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icn_more_vert_24dp),
                        contentDescription = stringResource(R.string.menu)
                    )
                }
            }
        }
    }
}