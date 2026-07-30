package chat.stoat.composables.chat

import android.annotation.SuppressLint
import android.content.Intent
import android.icu.text.DateFormat
import android.net.Uri
import android.text.format.DateUtils
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
import androidx.core.net.toUri
import chat.stoat.R
import chat.stoat.activities.media.ImageViewActivity
import chat.stoat.activities.media.VideoViewActivity
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.BrushCompat
import chat.stoat.api.internals.MessageFlag
import chat.stoat.api.internals.Roles
import chat.stoat.api.internals.SpecialUsers
import chat.stoat.api.internals.ULID
import chat.stoat.api.internals.has
import chat.stoat.api.internals.solidColor
import chat.stoat.api.routes.channel.react
import chat.stoat.api.routes.channel.unreact
import chat.stoat.api.routes.microservices.january.asJanuaryProxyUrl
import chat.stoat.api.settings.Experiments
import chat.stoat.api.settings.LoadedSettings
import chat.stoat.api.settings.MessageReplyStyle
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.composables.generic.RemoteImage
import chat.stoat.composables.generic.UserAvatar
import chat.stoat.composables.generic.UserAvatarWidthPlaceholder
import chat.stoat.composables.markdown.prose.ChatMarkdown
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.AutumnResource
import chat.stoat.core.model.schemas.User
import chat.stoat.internals.text.Gigamoji
import chat.stoat.internals.text.GigamojiState
import chat.stoat.internals.text.MessageProcessor
import chat.stoat.internals.text.stripPUAChars
import chat.stoat.internals.toNavigationAction
import chat.stoat.internals.toStoatWebLinkOrNull
import chat.stoat.persistence.KVStorage
import com.mikepenz.markdown.model.State
import kotlinx.coroutines.launch
import chat.stoat.core.model.schemas.Message as MessageSchema

@Composable
fun authorColour(message: MessageSchema): Brush {
    return if (message.masquerade?.colour != null) {
        BrushCompat.parseColour(message.masquerade!!.colour!!)
    } else {
        val defaultColour = Brush.solidColor(LocalContentColor.current)

        val serverId = StoatAPI.channelCache[message.channel]?.server ?: return defaultColour

        val highestRole = message.author?.let {
            Roles.resolveHighestRole(serverId, it, withColour = true)
        } ?: return defaultColour

        highestRole.colour?.let { BrushCompat.parseColour(it) }
            ?: defaultColour
    }
}

@Composable
fun authorRoleIcon(message: MessageSchema): AutumnResource? {
    val serverId = StoatAPI.channelCache[message.channel]?.server ?: return null

    val highestRole = message.author?.let {
        Roles.resolveHighestRole(serverId, it)
    } ?: return null

    return highestRole.icon
}

@Composable
fun displayNameInChannel(userId: String, channelId: String): String {
    val serverId =
        StoatAPI.channelCache[channelId]?.server
            ?: return StoatAPI.userCache[userId]?.let { User.resolveDefaultName(it) }
                ?: stringResource(R.string.unknown)

    val member = userId.let { StoatAPI.members.getMember(serverId, it) }
        ?: return stringResource(R.string.unknown)
    return member.nickname
        ?: StoatAPI.userCache[userId]?.let { User.resolveDefaultName(it) }
        ?: stringResource(R.string.unknown)
}

@Composable
fun authorName(message: MessageSchema): String {
    if (message.masquerade?.name != null) {
        return message.masquerade!!.name!!
    }

    val serverId =
        StoatAPI.channelCache[message.channel]?.server
            ?: return StoatAPI.userCache[message.author]?.let { User.resolveDefaultName(it) }
                ?: stringResource(R.string.unknown)

    val member = message.author?.let { StoatAPI.members.getMember(serverId, it) }
        ?: return stringResource(R.string.unknown)
    return member.nickname
        ?: StoatAPI.userCache[message.author]?.let { User.resolveDefaultName(it) }
        ?: stringResource(R.string.unknown)
}

fun authorPronouns(message: MessageSchema, author: User): String? {
    val serverId = StoatAPI.channelCache[message.channel]?.server
        ?: return author.pronouns

    val memberPronouns = message.author
        ?.let { StoatAPI.members.getMember(serverId, it) }
        ?.pronouns

    return memberPronouns ?: author.pronouns
}

internal fun messageTimestampText(pronouns: String?, timestamp: String): String {
    val visiblePronouns = pronouns?.trim()?.takeIf { it.isNotEmpty() }
    return visiblePronouns?.let { "$it · $timestamp" } ?: timestamp
}

@Composable
fun authorAvatarUrl(message: MessageSchema): String? {
    if (message.masquerade?.avatar != null) {
        return asJanuaryProxyUrl(message.masquerade!!.avatar!!)
    }

    val serverId =
        StoatAPI.channelCache[message.channel]?.server ?: return null
    val member = message.author?.let { StoatAPI.members.getMember(serverId, it) }
        ?: return null

    return member.avatar?.let { "$STOAT_FILES/avatars/${it.id}" }
}

fun viewUrlInBrowser(ctx: android.content.Context, url: String) {
    val customTab = CustomTabsIntent
        .Builder()
        .build()
    customTab.launchUrl(ctx, Uri.parse(url))
}

fun viewAttachmentInBrowser(ctx: android.content.Context, attachment: AutumnResource) {
    val url = "$STOAT_FILES/attachments/${attachment.id}/${attachment.filename}"
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
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun Message(
    message: MessageSchema,
    onClick: () -> Unit = {},
    onMessageContextMenu: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onNameClick: (() -> Unit)? = null,
    canReply: Boolean = false,
    onReply: () -> Unit = {},
    onAddReaction: () -> Unit = {},
    onJumpToMessage: (String) -> Unit = {},
    fromWebhook: Boolean = false,
    webhookName: String? = null,
    modifier: Modifier = Modifier,
    mdAst: State? = null
) {
    val author = StoatAPI.userCache[message.author] ?: return CircularProgressIndicator()
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    val openMessageLinkOrBrowser: (String) -> Unit = { url ->
        val stoatLink = url.toUri().toStoatWebLinkOrNull()
        if (stoatLink != null) {
            scope.launch {
                ActionChannel.send(stoatLink.toNavigationAction())
            }
        } else {
            viewUrlInBrowser(context, url)
        }
    }
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
            StoatAPI.channelCache[message.channel]?.server ?: return@LaunchedEffect
        var selfMember = StoatAPI.selfId?.let { StoatAPI.members.getMember(serverId, it) }
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
                        onClick = onClick,
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
                            painter = painterResource(R.drawable.ic_block_24dp),
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
                    if ((message.mentions?.contains(StoatAPI.selfId) == true)
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
                    val replyMessage = StoatAPI.messageCache[reply]

                    message.channel?.let { chId ->
                        InReplyTo(
                            channelId = chId,
                            messageId = reply,
                            withMention = (replyMessage?.author?.let {
                                message.mentions?.contains(
                                    replyMessage.author
                                )
                            } == true),
                            onMessageClick = onJumpToMessage,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = onClick,
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

                                val roleIcon = authorRoleIcon(message)
                                if (roleIcon != null) {
                                    Spacer(modifier = Modifier.width(4.dp))

                                    RemoteImage(
                                        url = "$STOAT_FILES/icons/${roleIcon.id}",
                                        contentScale = ContentScale.Fit,
                                        description = null,
                                        modifier = Modifier
                                            .size(16.dp)
                                    )
                                }

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
                                    text = messageTimestampText(
                                        pronouns = authorPronouns(message, author),
                                        timestamp = formatLongAsTime(ULID.asTimestamp(message.id!!))
                                    ),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.width(2.dp))

                                if (message.edited != null) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_edit_24dp),
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
                                val content = it.stripPUAChars()
                                if (content.isBlank()) return@let // if only an attachment is sent

                                val gigamoji = Gigamoji.useGigamojiForMessage(content)
                                val fontSizeMultiplier = when (gigamoji) {
                                    GigamojiState.Single -> 5f
                                    GigamojiState.Multiple -> 2f
                                    GigamojiState.None -> 1f
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                if (mdAst != null) {
                                    ChatMarkdown(
                                        mdAst,
                                        serverId = StoatAPI.channelCache[message.channel]?.server,
                                        fontSizeMultiplier = fontSizeMultiplier,
                                    )
                                } else {
                                    ChatMarkdown(
                                        content = content,
                                        serverId = StoatAPI.channelCache[message.channel]?.server,
                                        fontSizeMultiplier = fontSizeMultiplier,
                                    )
                                }
                            }
                        }

                        message.attachments?.let {
                            it.forEach { attachment ->
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
                            it.forEach { embed ->
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
                                            serverId = StoatAPI.channelCache[message.channel]?.server,
                                            onLinkClick = {
                                                openMessageLinkOrBrowser(it)
                                            })
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    "Image" -> {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        BoxWithConstraints(
                                            modifier = Modifier
                                                .clip(MaterialTheme.shapes.medium)
                                                .clickable {
                                                    embed.url?.let {
                                                        openMessageLinkOrBrowser(it)
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
                                        painter = painterResource(R.drawable.ic_add_reaction_24dp),
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
