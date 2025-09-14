package chat.zekochat.composables.chat

import android.annotation.SuppressLint
import android.content.Intent
import android.icu.text.DateFormat
import android.net.Uri
import android.text.format.DateUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.zekochat.R
import chat.zekochat.activities.media.ImageViewActivity
import chat.zekochat.activities.media.VideoViewActivity
import chat.zekochat.api.PeptideAPI
import chat.zekochat.api.internals.BrushCompat
import chat.zekochat.api.internals.MessageFlag
import chat.zekochat.api.internals.Roles
import chat.zekochat.api.internals.SpecialUsers
import chat.zekochat.api.internals.ULID
import chat.zekochat.api.internals.has
import chat.zekochat.api.internals.solidColor
import chat.zekochat.api.routes.channel.react
import chat.zekochat.api.routes.channel.unreact
import chat.zekochat.api.routes.microservices.january.asJanuaryProxyUrl
import chat.zekochat.api.schemas.AutumnResource
import chat.zekochat.api.schemas.User
import chat.zekochat.api.settings.Experiments
import chat.zekochat.api.settings.LoadedSettings
import chat.zekochat.api.settings.MessageReplyStyle
import chat.zekochat.callbacks.Action
import chat.zekochat.callbacks.ActionChannel
import chat.zekochat.utils.InternalLinkHandler
import chat.zekochat.composables.generic.RemoteImage
import chat.zekochat.composables.generic.UserAvatar
import chat.zekochat.composables.generic.UserAvatarWidthPlaceholder
import chat.zekochat.composables.markdown.LocalMarkdownTreeConfig
import chat.zekochat.composables.markdown.RichMarkdown
import chat.zekochat.internals.text.Gigamoji
import chat.zekochat.internals.text.MessageProcessor
import chat.zekochat.markdown.jbm.JBM
import chat.zekochat.markdown.jbm.JBMRenderer
import chat.zekochat.markdown.jbm.LocalJBMarkdownTreeState
import chat.zekochat.persistence.KVStorage
import kotlinx.coroutines.launch
import chat.zekochat.api.schemas.Message as MessageSchema

@Composable
fun authorColour(message: MessageSchema): Brush {
    return if (message.masquerade?.colour != null) {
        BrushCompat.parseColour(message.masquerade.colour)
    } else {
        val defaultColour = Brush.solidColor(LocalContentColor.current)

        val serverId = PeptideAPI.channelCache[message.channel]?.server ?: return defaultColour

        val highestRole = message.author?.let {
            Roles.resolveHighestRole(serverId, it, withColour = true)
        } ?: return defaultColour

        highestRole.colour?.let { BrushCompat.parseColour(it) }
            ?: defaultColour
    }
}

@Composable
fun authorName(message: MessageSchema): String {
    if (message.masquerade?.name != null) {
        return message.masquerade.name
    }

    val serverId =
        PeptideAPI.channelCache[message.channel]?.server
            ?: return PeptideAPI.userCache[message.author]?.let { User.resolveDefaultName(it) }
                ?: stringResource(R.string.unknown)

    val member = message.author?.let { PeptideAPI.members.getMember(serverId, it) }
        ?: return stringResource(R.string.unknown)
    return member.nickname
        ?: PeptideAPI.userCache[message.author]?.let { User.resolveDefaultName(it) }
        ?: stringResource(R.string.unknown)
}

@Composable
fun authorAvatarUrl(message: MessageSchema): String? {
    if (message.masquerade?.avatar != null) {
        return asJanuaryProxyUrl(message.masquerade.avatar)
    }

    val serverId =
        PeptideAPI.channelCache[message.channel]?.server ?: return null
    val member = message.author?.let { PeptideAPI.members.getMember(serverId, it) }
        ?: return null

    return member.avatar?.let { "${PeptideAPI.getCurrentFilesUrl()}/avatars/${it.id}" }
}

fun viewUrlInBrowser(ctx: android.content.Context, url: String) {
    Log.d("Message", "viewUrlInBrowser called with url: $url")

    // Check if this is an internal link that should be handled by the app
    if (InternalLinkHandler.isInternalLink(url)) {
        Log.d("Message", "URL is an internal link, attempting to handle internally")

        // Try to handle the link internally
        val handled = InternalLinkHandler.handleInternalLink(ctx, url, true)

        Log.d("Message", "Internal link handled: $handled")

        if (handled) {
            // Link was handled internally
            Log.d("Message", "Link was handled internally, returning")
            return
        } else {
            Log.d("Message", "Failed to handle link internally, falling back to browser")
        }
    } else {
        Log.d("Message", "URL is not an internal link, opening in browser")
    }

    // If not an internal link or couldn't be handled internally, open in browser
    try {
        Log.d("Message", "Opening URL in browser")
        val customTab = CustomTabsIntent
            .Builder()
            .build()
        customTab.launchUrl(ctx, Uri.parse(url))
        Log.d("Message", "URL opened in browser successfully")
    } catch (e: Exception) {
        Log.e("Message", "Error opening URL in browser", e)
    }
}

fun viewAttachmentInBrowser(ctx: android.content.Context, attachment: AutumnResource) {
    val url =
        "${PeptideAPI.getCurrentFilesUrl()}/attachments/${attachment.id}/${attachment.filename}"
    viewUrlInBrowser(ctx, url)
}

fun formatLongAsTime(time: Long): String {
    val date = java.util.Date(time)

    val withinLastWeek = System.currentTimeMillis() - time < 604800000

    return if (withinLastWeek) {
        val relativeDate = DateUtils.getRelativeTimeSpanString(
            time,
            System.currentTimeMillis(),
            DateUtils.DAY_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_ALL
        )
        val relativeTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(date)

        if (DateUtils.isToday(time)) {
            return relativeTime
        }
        "$relativeDate $relativeTime"
    } else {
        val absoluteDate = DateFormat.getDateInstance(DateFormat.SHORT).format(date)
        val absoluteTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(date)

        "$absoluteDate $absoluteTime"
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, JBM::class)
@Composable
fun Message(
    modifier: Modifier = Modifier,
    message: MessageSchema,
    onMessageContextMenu: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onNameClick: (() -> Unit)? = null,
    canReply: Boolean = false,
    onReply: () -> Unit = {},
    onAddReaction: () -> Unit = {},
    fromWebhook: Boolean = false,
    webhookName: String? = null,
) {
    val author = PeptideAPI.userCache[message.author] ?: return CircularProgressIndicator()
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    var kv by remember { mutableStateOf<KVStorage?>(null) }
    var showUsernameDiscriminator by remember { mutableStateOf(false) }
    var ignoreServerAvatar by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (Experiments.enableServerIdentityOptions.isEnabled) {
            val userId = author.id ?: return@LaunchedEffect
            kv = KVStorage(context)
            kv?.let {
                showUsernameDiscriminator =
                    it.getBoolean("exp/serverIdentityOptions/$userId/showUsernameDiscriminator") == true
                ignoreServerAvatar =
                    it.getBoolean("exp/serverIdentityOptions/$userId/ignoreServerAvatar") == true
            }
        }
    }

    val attachmentView = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            // do nothing
        }
    )

    val authorIsBlocked = remember(author) { author.relationship == "Blocked" }

    var mentionsSelfRole by remember(message) { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val serverId =
            PeptideAPI.channelCache[message.channel]?.server ?: return@LaunchedEffect
        var selfMember = PeptideAPI.selfId?.let { PeptideAPI.members.getMember(serverId, it) }
            ?: return@LaunchedEffect
        var messageRoleMentions = MessageProcessor.findMentionedRoleIDs(message.content)

        mentionsSelfRole = selfMember.roles?.any { it in messageRoleMentions } == true
    }

    Column(modifier.animateContentSize()) {
        if (message.tail == false) {
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (authorIsBlocked) {
            Row(
                modifier = Modifier
                    .combinedClickable(
                        onClick = {},
                        onDoubleClick = {},
                        onLongClick = {
                            onMessageContextMenu()
                        }
                    )
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
            ) {
                UserAvatarWidthPlaceholder()

                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.icn_block_24dp),
                            contentDescription = null
                        )

                        Text(
                            text = stringResource(R.string.message_blocked),
                            fontSize = 12.sp,
                            color = LocalContentColor.current.copy(alpha = 0.5f),
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.then(
                    if ((message.mentions?.contains(PeptideAPI.selfId) == true)
                        || mentionsSelfRole
                        || message.flags has MessageFlag.MentionsOnline
                        || message.flags has MessageFlag.MentionsEveryone
                    ) {
                        Modifier.background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    } else {
                        Modifier
                    }
                )
            ) {
                message.replies?.forEach { reply ->
                    val replyMessage = PeptideAPI.messageCache[reply]

                    message.channel?.let { chId ->
                        InReplyTo(
                            channelId = chId,
                            messageId = reply,
                            withMention = (replyMessage?.author?.let {
                                message.mentions?.contains(
                                    replyMessage.author
                                )
                            } == true),
                        ) {
                            // TODO Add jump to message
                            if (replyMessage == null) {
                                Toast.makeText(context, "lmao prankd", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {},
                            onDoubleClick = {
                                if (canReply && LoadedSettings.messageReplyStyle == MessageReplyStyle.DoubleTap) {
                                    onReply()
                                }
                            },
                            onLongClick = {
                                onMessageContextMenu()
                            }
                        )
                        .padding(horizontal = 10.dp)
                        .fillMaxWidth()
                ) {
                    if (message.tail == false) {
                        Column {
                            Spacer(modifier = Modifier.height(4.dp))
                            UserAvatar(
                                username = User.resolveDefaultName(author),
                                userId = author.id ?: message.id ?: ULID.makeSpecial(0),
                                avatar = author.avatar,
                                rawUrl = if (ignoreServerAvatar) null else authorAvatarUrl(message),
                                onClick = onAvatarClick
                            )
                        }
                    } else {
                        UserAvatarWidthPlaceholder()
                    }

                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        if (message.tail == false) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = buildAnnotatedString {
                                        if (showUsernameDiscriminator) {
                                            pushStyle(
                                                SpanStyle(
                                                    color = LocalContentColor.current.copy(
                                                        alpha = 0.5f
                                                    ),
                                                    fontWeight = FontWeight.Bold,
                                                    textDecoration = TextDecoration.LineThrough
                                                )
                                            )
                                        }
                                        append(webhookName ?: authorName(message))
                                        if (showUsernameDiscriminator) {
                                            pop()
                                            append(" ${author.username ?: "<?>"}#${author.discriminator ?: "0000"}")
                                        }
                                    },
                                    style = LocalTextStyle.current.copy(
                                        fontWeight = FontWeight.Bold,
                                        brush = authorColour(message)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.then(
                                        if (onNameClick != null)
                                            Modifier.combinedClickable(
                                                onClick = onNameClick,
                                                onLongClick = {
                                                    onMessageContextMenu()
                                                }
                                            )
                                        else Modifier
                                    )
                                )

                                InlineBadges(
                                    bot = author.bot != null && message.masquerade == null,
                                    bridge = message.masquerade != null && author.bot != null,
                                    platformModeration = author.id == SpecialUsers.PLATFORM_MODERATION_USER,
                                    teamMember = author.id in SpecialUsers.TEAM_MEMBER_FLAIRS.keys,
                                    webhook = fromWebhook,
                                    colour = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp),
                                    precedingIfAny = {
                                        Spacer(modifier = Modifier.width(5.dp))
                                    }
                                )

                                Spacer(modifier = Modifier.width(5.dp))

                                Text(
                                    text = formatLongAsTime(ULID.asTimestamp(message.id!!)),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.width(2.dp))

                                if (message.edited != null) {
                                    Icon(
                                        painter = painterResource(R.drawable.icn_edit_24dp),
                                        contentDescription = stringResource(id = R.string.edited),
                                        tint = MaterialTheme.colorScheme.onBackground.copy(
                                            alpha = 0.5f
                                        ),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        key(message.content) {
                            message.content?.let {
                                if (message.content.isBlank()) return@let // if only an attachment is sent

                                if (Experiments.useKotlinBasedMarkdownRenderer.isEnabled) {
                                    CompositionLocalProvider(
                                        LocalJBMarkdownTreeState provides LocalJBMarkdownTreeState.current.copy(
                                            currentServer = PeptideAPI.channelCache[message.channel]?.server,
                                            fontSizeMultiplier = Gigamoji.useGigamojiForMessage(
                                                message.content
                                            )
                                                .let {
                                                    if (it) 2f else 1f
                                                }
                                        )
                                    ) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        JBMRenderer(message.content)
                                    }
                                } else {
                                    CompositionLocalProvider(
                                        LocalMarkdownTreeConfig provides LocalMarkdownTreeConfig.current.copy(
                                            currentServer = PeptideAPI.channelCache[message.channel]?.server,
                                            fontSizeMultiplier = Gigamoji.useGigamojiForMessage(
                                                message.content
                                            )
                                                .let {
                                                    if (it) 2f else 1f
                                                }
                                        )
                                    ) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        RichMarkdown(input = message.content)
                                    }
                                }
                            }
                        }

                        message.attachments?.let {
                            message.attachments.forEach { attachment ->
                                Spacer(modifier = Modifier.height(2.dp))
                                MessageAttachment(attachment) {
                                    when (attachment.metadata?.type) {
                                        "Image" -> {
                                            attachmentView.launch(
                                                Intent(
                                                    context,
                                                    ImageViewActivity::class.java
                                                ).apply {
                                                    putExtra("autumnResource", attachment)
                                                }
                                            )
                                        }

                                        "Video" -> {
                                            attachmentView.launch(
                                                Intent(
                                                    context,
                                                    VideoViewActivity::class.java
                                                ).apply {
                                                    putExtra("autumnResource", attachment)
                                                }
                                            )
                                        }

                                        "Audio" -> {
                                            /* no-op */
                                        }

                                        else -> {
                                            viewAttachmentInBrowser(context, attachment)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }

                        message.embeds?.let {
                            message.embeds.forEach { embed ->
                                when (embed.type) {
                                    "Website", "Text" -> {
                                        val embedIsEmpty =
                                            embed.title == null && embed.description == null && embed.iconURL == null && embed.image == null

                                        if (embedIsEmpty) {
                                            // if we do not emit anything, compose will cause an internal error.
                                            // FIXME if you are doing fixme's anyways then check if this is still an issue
                                            Box {}
                                            return@forEach
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Embed(
                                            embed = embed,
                                            onLinkClick = {
                                                viewUrlInBrowser(context, it)
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    "Image" -> {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        BoxWithConstraints(
                                            modifier = Modifier
                                                .clip(MaterialTheme.shapes.medium)
                                                .clickable {
                                                    embed.url?.let {
                                                        viewUrlInBrowser(context, it)
                                                    }
                                                }
                                        ) {
                                            embed.url?.let { url ->
                                                RemoteImage(
                                                    url = asJanuaryProxyUrl(url),
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier
                                                        .width(
                                                            embed.width?.toInt()?.dp
                                                                ?: maxWidth
                                                        )
                                                        .aspectRatio(
                                                            embed.width!!.toFloat() / embed.height!!.toFloat()
                                                        ),
                                                    description = null
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }

                                    else -> {
                                        // no-op
                                    }
                                }
                            }
                        }

                        val reactionsAndInteractions = remember(message.reactions) {
                            message.reactions.orEmpty().toMutableMap().also {
                                message.interactions?.reactions?.forEach { reaction ->
                                    if (!it.containsKey(reaction)) {
                                        it[reaction] = listOf()
                                    }
                                }
                            }
                        }

                        if (reactionsAndInteractions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                reactionsAndInteractions.forEach { reaction ->
                                    Reaction(
                                        reaction.key, reaction.value,
                                        onClick = { hasOwn ->
                                            scope.launch {
                                                if (hasOwn) {
                                                    unreact(
                                                        message.channel!!,
                                                        message.id!!,
                                                        reaction.key
                                                    )
                                                } else {
                                                    react(
                                                        message.channel!!,
                                                        message.id!!,
                                                        reaction.key
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        scope.launch {
                                            ActionChannel.send(
                                                Action.MessageReactionInfo(
                                                    message.id!!,
                                                    reaction.key
                                                )
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                        .clickable(onClick = onAddReaction)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.icn_add_reaction_24dp),
                                        contentDescription = stringResource(R.string.message_context_sheet_actions_react),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
