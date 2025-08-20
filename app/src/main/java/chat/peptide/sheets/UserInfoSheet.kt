package chat.peptide.sheets

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.peptide.R
import chat.peptide.api.PeptideAPI
import chat.peptide.api.routes.user.blockUser
import chat.peptide.api.routes.user.fetchUser
import chat.peptide.api.routes.user.openDM
import chat.peptide.api.routes.user.unblockUser
import chat.peptide.api.routes.user.unfriendUser
import chat.peptide.api.schemas.ChannelType
import chat.peptide.api.schemas.NotificationSettings
import chat.peptide.api.settings.Experiments
import chat.peptide.api.settings.FeatureFlags
import chat.peptide.api.settings.NotificationType
import chat.peptide.api.settings.SyncedSettings
import chat.peptide.callbacks.Action
import chat.peptide.callbacks.ActionChannel
import chat.peptide.composables.generic.NonIdealState
import chat.peptide.composables.generic.SheetButton
import chat.peptide.composables.generic.UserAvatar
import chat.peptide.composables.screens.settings.UserButtons
import chat.peptide.internals.Platform
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.asLog
import logcat.logcat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoSheet(
    userId: String,
    dismissSheet: suspend () -> Unit
) {
    var resolvedUser by remember { mutableStateOf(PeptideAPI.userCache[userId]) }

    LaunchedEffect(userId) {
        try {
            // Show cached value immediately if present
            resolvedUser = PeptideAPI.userCache[userId]
            // Fetch fresh to ensure latest relationship state
            val fresh = fetchUser(userId)
            resolvedUser = fresh
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    if (resolvedUser == null) {
        NonIdealState(
            icon = {
                Icon(
                    painter = painterResource(R.drawable.icn_error_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(it)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.user_info_sheet_user_not_found)
                )
            },
            description = {
                Text(
                    text = stringResource(R.string.user_info_sheet_user_not_found_description)
                )
            }
        )
        Spacer(Modifier.height(20.dp))
        return
    }

    var showUserCard by remember { mutableStateOf(false) }
    if (showUserCard) {
        val sheetState = rememberModalBottomSheetState(true)
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showUserCard = false }
        ) {
            UserCardSheet(resolvedUser!!)
        }
    }

    var showServerIdentityOptions by remember { mutableStateOf(false) }
    if (showServerIdentityOptions) {
        val sheetState = rememberModalBottomSheetState(true)
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showServerIdentityOptions = false }
        ) {
            ServerIdentityOptionsSheet(
                userId = resolvedUser!!.id!!
            )
        }
    }

    // Page state: 0 = main, 1 = notifications
    var page by remember { mutableIntStateOf(0) }

    AnimatedContent(
        targetState = page,
        transitionSpec = {
            if (targetState > initialState) {
                slideInHorizontally(animationSpec = tween(200)) { it } togetherWith
                        slideOutHorizontally(animationSpec = tween(200)) { -it }
            } else {
                slideInHorizontally(animationSpec = tween(200)) { -it } togetherWith
                        slideOutHorizontally(animationSpec = tween(200)) { it }
            }
        }, label = "User info actions pager"
    ) { current ->
        val scope = rememberCoroutineScope()
        val clipboardManager = LocalClipboardManager.current
        val context = LocalContext.current
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalItemSpacing = 16.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            if (current == 0) {
                item(key = "overview", span = StaggeredGridItemSpan.FullLine) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceBright)
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            UserAvatar(
                                username = resolvedUser!!.displayName ?: resolvedUser!!.username
                                ?: stringResource(R.string.unknown),
                                avatar = resolvedUser!!.avatar,
                                userId = resolvedUser!!.id!!,
                                size = 64.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = resolvedUser!!.displayName ?: resolvedUser!!.username
                                    ?: stringResource(R.string.unknown),
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                resolvedUser!!.username?.let { uname ->
                                    Text(
                                        text = "@$uname",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (Experiments.enableServerIdentityOptions.isEnabled || FeatureFlags.userCardsGranted) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (Experiments.enableServerIdentityOptions.isEnabled) {
                                        SmallFloatingActionButton(onClick = {
                                            showServerIdentityOptions = true
                                        }) {
                                            Icon(
                                                painter = painterResource(R.drawable.icn_psychology_alt_24dp),
                                                contentDescription = null
                                            )
                                        }
                                    }
                                    if (FeatureFlags.userCardsGranted) {
                                        SmallFloatingActionButton(onClick = {
                                            showUserCard = true
                                        }) {
                                            Icon(
                                                painter = painterResource(R.drawable.icn_badge_24dp),
                                                contentDescription = null
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        resolvedUser!!.status?.text?.takeIf { it.isNotBlank() }?.let { st ->
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = st,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Primary actions block (View Profile, Message, Notification Options, Copy DM ID)
                item(key = "primary-actions", span = StaggeredGridItemSpan.FullLine) {
                    if (current == 0) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceBright)
                        ) {
                            SheetButton(
                                headlineContent = { Text(text = stringResource(id = R.string.message_friends)) },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_new_message),
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    resolvedUser?.id?.let { targetId ->
                                        scope.launch {
                                            val dm = openDM(targetId)
                                            if (dm.id != null) {
                                                if (PeptideAPI.channelCache[dm.id] == null) {
                                                    PeptideAPI.channelCache[dm.id] = dm
                                                }
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
                                    }
                                }
                            )
                            HorizontalDivider()
                            SheetButton(
                                headlineContent = { Text(text = stringResource(id = R.string.channel_info_sheet_options_notifications_manage)) },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.icn_notification_settings_24dp),
                                        contentDescription = null
                                    )
                                },
                                onClick = { page = 1 }
                            )
                            HorizontalDivider()
                            SheetButton(
                                headlineContent = { Text(text = stringResource(id = R.string.channel_context_sheet_actions_copy_id)) },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.icn_identifier_copy_24dp),
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    resolvedUser?.id?.let { userId ->
                                        scope.launch {
                                            clipboardManager.setText(AnnotatedString(userId))
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
                    }
                }

                // Danger block (Block, Remove Friend, Report User)
                item(key = "danger-actions_${resolvedUser!!.relationship}", span = StaggeredGridItemSpan.FullLine) {
                    var isBlockActionLoading by remember { mutableStateOf(false) }
                    var isRemoveFriendLoading by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceBright)
                    ) {
                        SheetButton(
                            headlineContent = {
                                Text(
                                    text = stringResource(
                                        id = if (resolvedUser?.relationship == "Blocked") {
                                            R.string.user_info_sheet_unblock
                                        } else R.string.user_info_sheet_block
                                    )
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.icn_block_24dp),
                                    contentDescription = null,
                                )
                            },
                            trailingContent = {
                                if (isBlockActionLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = LocalContentColor.current
                                    )
                                }
                            },
                            onClick = {
                                scope.launch {
                                    try {
                                        isBlockActionLoading = true
                                        resolvedUser?.id?.let { userId ->
                                            if (resolvedUser?.relationship == "Blocked") {
                                                unblockUser(userId)
                                                // Update local cache/state to reflect new relationship
                                                val updated = resolvedUser!!.copy(relationship = null)
                                                resolvedUser = updated
                                                PeptideAPI.userCache[userId] =
                                                    (PeptideAPI.userCache[userId]?.copy(relationship = null))
                                                        ?: updated
                                                try { fetchUser(userId) } catch (_: Exception) {}
                                            } else {
                                                blockUser(userId)
                                                // Update local cache/state to reflect new relationship
                                                val updated = resolvedUser!!.copy(relationship = "Blocked")
                                                resolvedUser = updated
                                                PeptideAPI.userCache[userId] =
                                                    (PeptideAPI.userCache[userId]?.copy(relationship = "Blocked"))
                                                        ?: updated
                                                try { fetchUser(userId) } catch (_: Exception) {}
                                            }
                                        }
                                    } catch (e: Exception) {
                                        if (e.message == "NoEffect") return@launch
                                        logcat(LogPriority.ERROR) { e.asLog() }
                                    } finally {
                                        isBlockActionLoading = false
                                    }
                                }
                            },
                            modifier = Modifier,
                            dangerous = true
                        )
                        if (resolvedUser?.relationship == "Friend") {
                            HorizontalDivider()
                            SheetButton(
                                headlineContent = { Text(stringResource(R.string.user_info_sheet_remove_friend)) },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.icn_group_remove_24dp),
                                        contentDescription = null
                                    )
                                },
                                trailingContent = {
                                    if (isRemoveFriendLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = LocalContentColor.current
                                        )
                                    }
                                },
                                onClick = {
                                    resolvedUser?.id?.let { userId ->
                                        scope.launch {
                                            try {
                                                isRemoveFriendLoading = true
                                                unfriendUser(userId)
                                                val updated = resolvedUser!!.copy(relationship = "None")
                                                resolvedUser = updated
                                                PeptideAPI.userCache[userId] =
                                                    (PeptideAPI.userCache[userId]?.copy(relationship = "None"))
                                                        ?: updated
                                                try { fetchUser(userId) } catch (_: Exception) {}
                                            } catch (e: Exception) {
                                                if (e.message == "NoEffect") return@launch
                                                logcat(LogPriority.ERROR) { e.asLog() }
                                            } finally {
                                                isRemoveFriendLoading = false
                                            }
                                        }
                                    }
                                },
                                dangerous = true
                            )
                        }
                        HorizontalDivider()
                        SheetButton(
                            headlineContent = { Text(text = stringResource(id = R.string.report)) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.icn_psychology_alt_24dp),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                resolvedUser?.id?.let { userId ->
                                    scope.launch {
                                        ActionChannel.send(Action.ReportUser(userId))

                                        if (Platform.needsShowClipboardNotification()) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.copied),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            },
                            dangerous = true
                        )
                    }
                }

                // Removed joined, badges, and bio sections per request

                item(key = "actions_${resolvedUser!!.id}_${resolvedUser!!.relationship}", span = StaggeredGridItemSpan.FullLine) {
                    UserButtons(resolvedUser!!, dismissSheet, onRelationshipChanged = { newRel ->
                        resolvedUser = resolvedUser!!.copy(relationship = newRel)
                    })
                }
            } else {
                item(key = "notifications", span = StaggeredGridItemSpan.FullLine) {
                    // Notifications page
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_left_24dp),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { page = 0 }
                                    .align(Alignment.CenterStart)
                            )
                            Text(
                                modifier = Modifier.align(Alignment.Center),
                                text = stringResource(id = R.string.channel_info_sheet_options_notifications_manage),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Column(
                            modifier = Modifier
                                .padding(top = 24.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceBright)
                        ) {

                            // Radio options
                            val dmChannelId = PeptideAPI.channelCache.values.firstOrNull { ch ->
                                ch.channelType == ChannelType.DirectMessage && ch.user == resolvedUser!!.id
                            }?.id

                            var selected by remember {
                                mutableStateOf(
                                    NotificationType.fromStorage(
                                        SyncedSettings.notifications.channel[dmChannelId ?: ""]
                                    ).storageValue
                                )
                            }

                            fun updateSelection(newValue: NotificationType) {
                                selected = newValue.storageValue
                                val currentSettings = SyncedSettings.notifications
                                val updated = NotificationSettings(
                                    server = currentSettings.server,
                                    channel = currentSettings.channel.toMutableMap().apply {
                                        if (dmChannelId.isNullOrBlank() || newValue == NotificationType.DEFAULT) {
                                            if (!dmChannelId.isNullOrBlank()) remove(dmChannelId)
                                        } else {
                                            put(dmChannelId, newValue.storageValue)
                                        }
                                    }
                                )
                                GlobalScope.launch {
                                    SyncedSettings.updateNotifications(updated)
                                }
                            }

                            NotificationRadioRow(
                                title = stringResource(id = R.string.notification_option_use_default),
                                type = NotificationType.DEFAULT,
                                channelId = dmChannelId ?: "",
                                current = selected,
                                onChange = { updateSelection(NotificationType.DEFAULT) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                            NotificationRadioRow(
                                title = stringResource(id = R.string.notification_option_muted),
                                type = NotificationType.MUTED,
                                channelId = dmChannelId ?: "",
                                current = selected,
                                onChange = { updateSelection(NotificationType.MUTED) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                            NotificationRadioRow(
                                title = stringResource(id = R.string.notification_option_all_messages),
                                type = NotificationType.ALL,
                                channelId = dmChannelId ?: "",
                                current = selected,
                                onChange = { updateSelection(NotificationType.ALL) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                            NotificationRadioRow(
                                title = stringResource(id = R.string.notification_option_mentions_only),
                                type = NotificationType.MENTIONS_ONLY,
                                channelId = dmChannelId ?: "",
                                current = selected,
                                onChange = { updateSelection(NotificationType.MENTIONS_ONLY) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                            NotificationRadioRow(
                                title = stringResource(id = R.string.notification_option_none),
                                type = NotificationType.NONE,
                                channelId = dmChannelId ?: "",
                                current = selected,
                                onChange = { updateSelection(NotificationType.NONE) }
                            )
                        }
                    }
                }
            }

        }

    }
}