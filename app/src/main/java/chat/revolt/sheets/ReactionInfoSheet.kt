package chat.revolt.sheets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.isUlid
import chat.revolt.api.routes.custom.fetchEmoji
import chat.revolt.api.routes.user.fetchUser
import chat.revolt.api.schemas.Emoji
import chat.revolt.api.schemas.User
import chat.revolt.api.settings.LoadedSettings
import chat.revolt.composables.chat.MemberListItem
import chat.revolt.composables.generic.RemoteImage
import chat.revolt.internals.text.MessageProcessor
import chat.revolt.persistence.KVStorage
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReactionInfoSheet(messageId: String, emoji: String, onDismiss: () -> Unit) {
    val message = RevoltAPI.messageCache[messageId] ?: return
    val channel = RevoltAPI.channelCache[message.channel] ?: return
    val reactions = message.reactions
    val interactions = message.interactions?.reactions ?: emptyList()
    val reactionEmoji =
        (reactions?.keys?.toList() ?: emptyList())
            .plus(interactions)
            .distinct()
            .filterNot { it.isEmpty() }
            .sortedBy {
                if (it.isUlid()) {
                    RevoltAPI.emojiCache[it]?.name ?: it.codePointAt(0).toString()
                } else {
                    it.codePointAt(0).toString()
                }
            }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val extendedEmojiInfo = remember(emoji) { mutableStateListOf<Emoji>() }

    LaunchedEffect(reactionEmoji) {
        reactionEmoji?.forEach {
            if (it.isUlid()) {
                extendedEmojiInfo.add(RevoltAPI.emojiCache[it] ?: fetchEmoji(it))
            }
        }
    }

    var selectedReactionIndex by remember(
        messageId,
        emoji
    ) { mutableIntStateOf(reactionEmoji.indexOfFirst { it == emoji }) }

    if (selectedReactionIndex >= reactionEmoji.size || selectedReactionIndex < 0) {
        selectedReactionIndex = 0
    }

    if (reactionEmoji.isEmpty()) {
        onDismiss()
    }

    LazyColumn {
        stickyHeader(key = "tabs") {
            if (reactionEmoji.isNotEmpty() && selectedReactionIndex < reactionEmoji.size) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedReactionIndex,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    divider = {}
                ) {
                    reactionEmoji.forEachIndexed { index, emoji ->
                        Tab(
                            text = {
                                if (emoji.isUlid()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RemoteImage(
                                            url = "${RevoltAPI.getCurrentFilesUrl()}/emojis/${emoji}",
                                            description = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.size(6.dp))
                                        Text(
                                            "${reactions?.get(emoji)?.size ?: 0}",
                                            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                        )
                                    }
                                } else {
                                    Text(
                                        "$emoji ${reactions?.get(emoji)?.size ?: 0}",
                                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
                                    )
                                }
                            },
                            selected = selectedReactionIndex == index,
                            onClick = { selectedReactionIndex = index }
                        )
                    }
                }
                HorizontalDivider()
            }
        }

        item("info") {
            if (reactionEmoji.isNotEmpty() == true) {
                val current = reactionEmoji[selectedReactionIndex]

                // <editor-fold desc="Code related to enabling of experimental features">
                val interactionSource = remember { MutableInteractionSource() }
                val canBeUsedForTapCountIncrement =
                    remember(selectedReactionIndex) {
                        MessageProcessor.emoji.unicodeAsShortcode(
                            current
                        ) == ":trolleybus:"
                    }
                var tapCount by remember { mutableIntStateOf(0) }
                var showEnabledConfirmAlert by remember { mutableStateOf(false) }
                var showEnabledAlreadyAlert by remember { mutableStateOf(false) }
                val incrementTapCount = remember {
                    {
                        if (canBeUsedForTapCountIncrement) {
                            tapCount++
                            if (tapCount > 9) {
                                tapCount = 0
                                if (LoadedSettings.experimentsEnabled) {
                                    showEnabledAlreadyAlert = true
                                } else {
                                    showEnabledConfirmAlert = true
                                }
                            }
                        }
                    }
                }

                if (showEnabledAlreadyAlert) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Traveller, you may not unsee your knowledge...") },
                        text = { Text("Experimental features are already unlocked.") },
                        confirmButton = {
                            TextButton(onClick = { showEnabledAlreadyAlert = false }) {
                                Text("OK")
                            }
                        }
                    )
                }

                if (showEnabledConfirmAlert) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("You hear a faint whisper in the wind...") },
                        text = { Text("Would you like to enable experimental features? They may be unstable.") },
                        confirmButton = {
                            TextButton(onClick = {
                                showEnabledConfirmAlert = false
                                LoadedSettings.experimentsEnabled = true
                                scope.launch {
                                    KVStorage(context).set("experimentsEnabled", true)
                                }
                            }) {
                                Text("I dare to try!")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showEnabledConfirmAlert = false }) {
                                Text("I shall not risk it.")
                            }
                        }
                    )
                }
                // </editor-fold>

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(
                        top = 16.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 4.dp
                    ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (current.isUlid()) {
                            val cached = extendedEmojiInfo.find { it.id == current }
                            RemoteImage(
                                url = "${RevoltAPI.getCurrentFilesUrl()}/emojis/$current",
                                description = cached?.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(32.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = current,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        platformStyle = PlatformTextStyle(
                                            includeFontPadding = false
                                        )
                                    ),
                                    modifier = Modifier
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                        ) {
                                            incrementTapCount()
                                        }
                                        .size(64.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            if (current.isUlid()) {
                                val cached = extendedEmojiInfo.find { it.id == current }
                                Text(
                                    text = ":${cached?.name ?: current}:",
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.15.sp
                                )
                            } else {
                                Text(
                                    text = MessageProcessor.emoji.unicodeAsShortcode(current)
                                        ?: current,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.15.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (current.isUlid()) {
                                    val cached = extendedEmojiInfo.find { it.id == current }
                                    if (cached?.parent != null) {
                                        when (cached.parent.type) {
                                            "Server" -> RevoltAPI.serverCache[cached.parent.id]?.name?.let {
                                                stringResource(
                                                    id = R.string.emote_info_from_server,
                                                    it
                                                )
                                            }
                                                ?: stringResource(id = R.string.emote_info_from_server_unknown)

                                            else -> stringResource(id = R.string.emote_info_from_server_unknown)
                                        }
                                    } else {
                                        stringResource(id = R.string.emote_info_from_server_unknown)
                                    }
                                } else {
                                    stringResource(id = R.string.emote_info_from_unicode)
                                }
                            )
                        }
                    }

                    HorizontalDivider()
                }
            }
        }

        val reactionsForEmoji =
            reactions?.get(reactionEmoji[selectedReactionIndex]) ?: emptyList()
        items(items = reactionsForEmoji) { reaction ->
            val userOrNull = RevoltAPI.userCache[reaction]
            val user = userOrNull ?: User.getPlaceholder(reaction)
            val member = if (channel.server != null && user.id != null) {
                RevoltAPI.members.getMember(channel.server, user.id)
            } else {
                null
            }

            LaunchedEffect(reaction) {
                if (reaction !in RevoltAPI.userCache) {
                    try {
                        RevoltAPI.userCache[reaction] = fetchUser(reaction)
                    } catch (e: Exception) {
                        // too bad!
                    }
                }
            }

            MemberListItem(
                member = member,
                user = user,
                serverId = channel.server,
                userId = reaction,
            )
        }

        item("bottom") {

        }
    }
}