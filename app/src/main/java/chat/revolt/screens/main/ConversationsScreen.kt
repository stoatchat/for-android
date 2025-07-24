package chat.revolt.screens.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.schemas.ChannelType
import chat.revolt.api.schemas.User
import chat.revolt.api.settings.LoadedSettings
import chat.revolt.composables.generic.UserAvatar
import chat.revolt.internals.extensions.zero

// Note - this is not a traditional screen per se, as it is a part of the main screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(navController: NavController) {
    val context = LocalContext.current
    val dmAbleChannels =
        RevoltAPI.channelCache.values
            .filter { it.channelType == ChannelType.DirectMessage || it.channelType == ChannelType.Group }
            .filter { if (it.channelType == ChannelType.DirectMessage) it.active == true else true }
            .sortedBy { it.lastMessageID ?: it.id }
            .reversed()

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(NavigationBarDefaults.windowInsets),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.main_tab_conversations)) },
                windowInsets = WindowInsets.zero
            )
        },
    ) { pv ->
        LazyColumn(
            modifier = Modifier.padding(pv),
        ) {
            item(key = "saved_messages") {
                val notesChannel =
                    RevoltAPI.channelCache.values.firstOrNull { it.channelType == ChannelType.SavedMessages }
                val lastMessage = notesChannel?.lastMessageID?.let { RevoltAPI.messageCache[it] }
                val hasAttachments = remember {
                    context.getString(R.string.reply_message_empty_has_attachments)
                }
                val preview = remember(lastMessage) {
                    (RevoltAPI.userCache[RevoltAPI.selfId]?.let {
                        User.resolveDefaultName(
                            it
                        )
                    }
                        .orEmpty() + ": Remember to fdjhlfhdsfdsjfds").trim()// + (lastMessage?.content ?: hasAttachments)).trim()
                }

                if (notesChannel != null) {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.channel_notes))
                        },
                        supportingContent = {
                            if (preview.isNotBlank()) {
                                Text(
                                    preview,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        },
                        leadingContent = {
                            Box(contentAlignment = Alignment.TopEnd) {
                                RevoltAPI.userCache[RevoltAPI.selfId]?.let {
                                    UserAvatar(
                                        username = it.username.toString(),
                                        avatar = it.avatar,
                                        userId = it.id.toString(),
                                        shape = RoundedCornerShape(LoadedSettings.avatarRadius)
                                    )
                                }
                                Badge {
                                    Icon(
                                        painter = painterResource(R.drawable.icn_keep_24dp),
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            navController.navigate("main/conversation/${notesChannel.id}")
                        }
                    )
                    HorizontalDivider()
                }
            }
            items(1000) {
                Text(
                    "Conversation $it", modifier = Modifier
                        .clickable {
                            navController.navigate("main/conversation/${it}")
                        }
                        .fillMaxWidth())
            }
        }
    }
}