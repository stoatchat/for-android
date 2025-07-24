package chat.revolt.screens.settings.channel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.PermissionBit
import chat.revolt.api.internals.hasPermission
import chat.revolt.api.routes.channel.leaveDeleteOrCloseChannel
import chat.revolt.api.schemas.ChannelType
import chat.revolt.api.settings.FeatureFlags
import chat.revolt.internals.extensions.rememberChannelPermissions
import chat.revolt.screens.settings.SettingsIcon
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelSettingsHome(navController: NavController, channelId: String) {
    val channel = RevoltAPI.channelCache[channelId]
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val permissions by rememberChannelPermissions(channelId)
    var showDeletionConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showDeletionConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showDeletionConfirmation = false
            },
            title = { Text(stringResource(R.string.channel_settings_delete_confirm)) },
            text = { Text(stringResource(R.string.channel_settings_delete_confirm_description)) },
            dismissButton = {
                TextButton(onClick = {
                    showDeletionConfirmation = false
                }) {
                    Text(stringResource(R.string.channel_settings_delete_confirm_no))
                }
            },
            confirmButton = {
                Button(onClick = {
                    showDeletionConfirmation = false
                    scope.launch {
                        leaveDeleteOrCloseChannel(channelId)
                        navController.popBackStack()
                    }
                }) {
                    Text(stringResource(R.string.channel_settings_delete_confirm_yes))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = channel?.let {
                            when (it.channelType) {
                                ChannelType.TextChannel, ChannelType.VoiceChannel -> stringResource(
                                    R.string.channel_settings_header,
                                    channel.name ?: channelId
                                )

                                else -> channel.name ?: stringResource(R.string.channel_settings)
                            }
                        } ?: stringResource(R.string.channel_settings),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.icn_arrow_back_24dp),
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
            )
        },
    ) { pv ->
        Box(Modifier.padding(pv)) {
            channel?.let {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (permissions.hasPermission(PermissionBit.ManageChannel)) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(id = R.string.channel_settings_overview)
                                )
                            },
                            leadingContent = {
                                SettingsIcon {
                                    Icon(
                                        painter = painterResource(R.drawable.icn_info_24dp),
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier = Modifier
                                .testTag("channel_settings_view_overview")
                                .clickable {
                                    navController.navigate("settings/channel/${channel.id}/overview")
                                }
                        )
                    }

                    // TODO Implement permissions UI and remove the predicate check
                    if (permissions.hasPermission(PermissionBit.ManageRole) && FeatureFlags.labsAccessControlGranted) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(id = R.string.channel_settings_permissions)
                                )
                            },
                            leadingContent = {
                                SettingsIcon {
                                    Icon(
                                        painter = painterResource(id = R.drawable.icn_page_info_24dp),
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier = Modifier
                                .testTag("channel_settings_view_permissions")
                                .clickable {
                                    navController.navigate("settings/channel/${channel.id}/permissions")
                                }
                        )
                    }

                    if (permissions.hasPermission(PermissionBit.ManageChannel) && channel.channelType != ChannelType.DirectMessage && channel.channelType != ChannelType.Group) {
                        ListItem(
                            headlineContent = {
                                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                                    Text(
                                        text = stringResource(id = R.string.channel_settings_delete)
                                    )
                                }
                            },
                            leadingContent = {
                                SettingsIcon(danger = true) {
                                    Icon(
                                        painter = painterResource(R.drawable.icn_delete_24dp),
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier = Modifier
                                .testTag("channel_settings_click_delete")
                                .clickable {
                                    showDeletionConfirmation = true
                                }
                        )
                    }
                }
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}