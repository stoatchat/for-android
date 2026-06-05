package chat.stoat.composables.markdown.prose

import android.content.Intent
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import chat.stoat.R
import chat.stoat.activities.InviteActivity
import chat.stoat.api.StoatAPI
import chat.stoat.api.internals.isUlid
import chat.stoat.api.routes.custom.fetchEmoji
import chat.stoat.api.settings.LoadedSettings
import chat.stoat.callbacks.Action
import chat.stoat.callbacks.ActionChannel
import chat.stoat.composables.generic.RemoteImage
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.User
import chat.stoat.core.model.schemas.isInviteUri
import chat.stoat.internals.resolveTimestamp
import chat.stoat.markdown.CHANNEL_MENTION_ELEMENT_TYPE
import chat.stoat.markdown.CUSTOM_EMOTE_ELEMENT_TYPE
import chat.stoat.markdown.MASS_MENTION_ELEMENT_TYPE
import chat.stoat.markdown.ROLE_MENTION_ELEMENT_TYPE
import chat.stoat.markdown.StoatMarkdownFlavour
import chat.stoat.markdown.TIMESTAMP_ELEMENT_TYPE
import chat.stoat.markdown.USER_MENTION_ELEMENT_TYPE
import chat.stoat.ui.theme.FragmentMono
import chat.stoat.ui.theme.isThemeDark
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownInlineContent
import com.mikepenz.markdown.model.rememberMarkdownState
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import kotlinx.coroutines.launch
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.parser.MarkdownParser

// Converts single \n to hard line breaks so chat messages render line by line, while leaving fenced
// code blocks and double-newline paragraph breaks untouched, e.g. GitHub style line breaks
internal fun easyLineBreaks(content: String): String {
    val sb = StringBuilder(content.length + 16)
    var i = 0
    var inFence = false
    while (i < content.length) {
        val ch = content[i]
        if (ch == '`' || ch == '~') {
            val lineStart = sb.lastIndexOf('\n') + 1
            val atLineStart = sb.length == lineStart || sb.substring(lineStart).isBlank()
            if (atLineStart) {
                val fence = ch
                var run = 0
                while (i + run < content.length && content[i + run] == fence) run++
                if (run >= 3) {
                    inFence = !inFence
                    repeat(run) { sb.append(content[i + it]) }
                    i += run
                    continue
                }
            }
        }
        if (ch == '\n' && !inFence) {
            val prev = if (i > 0) content[i - 1] else ' '
            val next = if (i + 1 < content.length) content[i + 1] else ' '
            if (prev != '\n' && next != '\n') {
                sb.append("  ")
            }
        }
        sb.append(ch)
        i++
    }
    return sb.toString()
}

@Composable
fun ChatMarkdown(
    content: String,
    serverId: String? = null,
    fontSizeMultiplier: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val processed = remember(content) { easyLineBreaks(content) }
    val flavour = remember(processed) { StoatMarkdownFlavour(processed) }
    val markdownState = rememberMarkdownState(
        content = processed,
        flavour = flavour,
        parser = remember(flavour) { MarkdownParser(flavour) },
        immediate = true,
    )
    val state by markdownState.state.collectAsState()
    ChatMarkdown(
        state = state,
        serverId = serverId,
        fontSizeMultiplier = fontSizeMultiplier,
        modifier = modifier
    )
}

@Composable
fun ChatMarkdown(
    state: State,
    serverId: String? = null,
    fontSizeMultiplier: Float = 1f,
    modifier: Modifier = Modifier
) {
    val fontSize = LocalTextStyle.current.fontSize * fontSizeMultiplier
    val scope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val mentionLinkStyle = remember(primaryColor) {
        TextLinkStyles(
            SpanStyle(
                color = primaryColor,
                background = primaryColor.copy(alpha = 0.2f)
            )
        )
    }
    val annotator = remember(serverId, mentionLinkStyle, surfaceVariantColor) {
        markdownAnnotator { content, child ->
            when (child.type) {
                CUSTOM_EMOTE_ELEMENT_TYPE -> {
                    val ulid = child.getTextInNode(content).toString().removeSurrounding(":")
                    appendInlineContent("custom_emote", ulid)
                    true
                }

                USER_MENTION_ELEMENT_TYPE -> {
                    val raw = child.getTextInNode(content).toString()
                    val ulid = raw.substring(2, raw.length - 1)
                    val member = serverId?.let { StoatAPI.members.getMember(it, ulid) }
                    val displayName = member?.nickname
                        ?: StoatAPI.userCache[ulid]?.let { User.resolveDefaultName(it) }
                        ?: ulid
                    withLink(LinkAnnotation.Url("stoat-user://$ulid", mentionLinkStyle)) {
                        append("@$displayName")
                    }
                    true
                }

                CHANNEL_MENTION_ELEMENT_TYPE -> {
                    val raw = child.getTextInNode(content).toString()
                    val channelId = raw.substring(2, raw.length - 1)
                    val channelName = StoatAPI.channelCache[channelId]?.name ?: channelId
                    withLink(LinkAnnotation.Url("stoat-channel://$channelId", mentionLinkStyle)) {
                        append("#$channelName")
                    }
                    true
                }

                ROLE_MENTION_ELEMENT_TYPE -> {
                    val raw = child.getTextInNode(content).toString()
                    val roleId = raw.substring(2, raw.length - 1)
                    if (roleId.isUlid()) {
                        val role = serverId?.let { StoatAPI.serverCache[it]?.roles?.get(roleId) }
                        val roleColor = role?.colour
                            ?.takeIf { !it.contains("gradient") }
                            ?.let { runCatching { Color(it.toColorInt()) }.getOrNull() }
                            ?: primaryColor
                        withStyle(
                            SpanStyle(
                                color = roleColor,
                                background = roleColor.copy(alpha = 0.2f)
                            )
                        ) {
                            append("@${role?.name ?: roleId}")
                        }
                    } else {
                        append(raw)
                    }
                    true
                }

                MASS_MENTION_ELEMENT_TYPE -> {
                    val raw = child.getTextInNode(content).toString()
                    val displayText = if (raw.contains("EVERYONE")) "@everyone" else "@online"
                    withStyle(
                        SpanStyle(
                            color = primaryColor,
                            background = primaryColor.copy(alpha = 0.2f)
                        )
                    ) {
                        append(displayText)
                    }
                    true
                }

                TIMESTAMP_ELEMENT_TYPE -> {
                    val raw = child.getTextInNode(content).toString()
                    val inner = raw.removePrefix("<t:").removeSuffix(">")
                    val parts = inner.split(":", limit = 2)
                    val epochSeconds = parts[0].toLongOrNull()
                    if (epochSeconds == null) {
                        append(raw)
                    } else {
                        val style = parts.getOrNull(1)
                        withLink(
                            LinkAnnotation.Url(
                                "stoat-timestamp://$epochSeconds",
                                TextLinkStyles(SpanStyle(background = surfaceVariantColor))
                            )
                        ) {
                            append(resolveTimestamp(epochSeconds, style))
                        }
                    }
                    true
                }

                else -> false
            }
        }
    }
    val context = LocalContext.current
    val resources = LocalResources.current
    val toolbarColor = MaterialTheme.colorScheme.surfaceContainer.toArgb()
    val uriHandler = remember(scope, serverId, toolbarColor) {
        object : UriHandler {
            override fun openUri(uri: String) {
                when {
                    uri.startsWith("stoat-user://") -> {
                        val ulid = uri.removePrefix("stoat-user://")
                        scope.launch { ActionChannel.send(Action.OpenUserSheet(ulid, serverId)) }
                    }

                    uri.startsWith("stoat-channel://") -> {
                        val channelId = uri.removePrefix("stoat-channel://")
                        scope.launch { ActionChannel.send(Action.SwitchChannel(channelId)) }
                    }

                    uri.startsWith("stoat-timestamp://") -> {
                        val epochSeconds = uri.removePrefix("stoat-timestamp://").toLongOrNull()
                        if (epochSeconds != null) {
                            Toast.makeText(
                                context,
                                resolveTimestamp(epochSeconds, "F"),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    else -> {
                        try {
                            val parsed = uri.toUri()
                            if (parsed.isInviteUri()) {
                                context.startActivity(
                                    Intent(context, InviteActivity::class.java)
                                        .apply { data = parsed }
                                )
                                return
                            }
                        } catch (_: Exception) {
                            // no-op
                        }
                        val customTab = CustomTabsIntent.Builder()
                            .setShowTitle(true)
                            .setDefaultColorSchemeParams(
                                CustomTabColorSchemeParams.Builder()
                                    .setToolbarColor(toolbarColor)
                                    .build()
                            )
                            .build()
                        try {
                            customTab.launchUrl(context, uri.toUri())
                        } catch (_: Exception) {
                            Toast.makeText(
                                context,
                                resources.getString(R.string.link_type_no_intent),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
    val systemIsDark = isSystemInDarkTheme()
    val isDarkTheme =
        remember(LoadedSettings.theme) { isThemeDark(LoadedSettings.theme, systemIsDark) }
    val highlightsBuilder = remember(isDarkTheme) {
        Highlights.Builder().theme(SyntaxThemes.atom(darkMode = isDarkTheme))
    }
    CompositionLocalProvider(LocalUriHandler provides uriHandler) {
        Markdown(
            state = state,
            annotator = annotator,
            typography = markdownTypography(
                h1 = MaterialTheme.typography.headlineLarge,
                h2 = MaterialTheme.typography.headlineMedium,
                h3 = MaterialTheme.typography.headlineSmall,
                h4 = MaterialTheme.typography.titleLarge,
                h5 = MaterialTheme.typography.titleMedium,
                h6 = MaterialTheme.typography.titleSmall,
                text = MaterialTheme.typography.bodyLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontSizeMultiplier),
                paragraph = MaterialTheme.typography.bodyLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontSizeMultiplier),
                code = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FragmentMono,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontSizeMultiplier
                ),
                inlineCode = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FragmentMono,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontSizeMultiplier
                ),
                textLink = TextLinkStyles(
                    SpanStyle(
                        color = primaryColor,
                        textDecoration = TextDecoration.Underline
                    )
                ),
            ),
            inlineContent = markdownInlineContent(
                mapOf(
                    "custom_emote" to InlineTextContent(
                        Placeholder(
                            width = fontSize * 1.5f,
                            height = fontSize * 1.5f,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                        )
                    ) { ulid ->
                        val emote = StoatAPI.emojiCache[ulid]
                        if (emote == null) {
                            LaunchedEffect(ulid) {
                                try {
                                    StoatAPI.emojiCache[ulid] = fetchEmoji(ulid)
                                } catch (_: Exception) {
                                }
                            }
                        } else {
                            with(LocalDensity.current) {
                                RemoteImage(
                                    url = "$STOAT_FILES/emojis/$ulid",
                                    description = emote.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .width((fontSize * 1.5f).toDp())
                                        .height((fontSize * 1.5f).toDp())
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) {
                                            scope.launch { ActionChannel.send(Action.EmoteInfo(ulid)) }
                                        },
                                )
                            }
                        }
                    }
                )
            ),
            components = markdownComponents(
                image = {},
                inlineImage = {},
                codeBlock = {
                    MarkdownHighlightedCodeBlock(
                        content = it.content,
                        node = it.node,
                        highlightsBuilder = highlightsBuilder,
                    )
                },
                codeFence = {
                    MarkdownHighlightedCodeFence(
                        content = it.content,
                        node = it.node,
                        highlightsBuilder = highlightsBuilder,
                    )
                },
            ),
            modifier = modifier,
        )
    }
}
