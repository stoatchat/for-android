package chat.stoat.composables.markdown.prose

import android.content.Intent
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
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
import chat.stoat.internals.toNavigationAction
import chat.stoat.internals.toStoatWebLinkOrNull
import chat.stoat.markdown.CHANNEL_MENTION_ELEMENT_TYPE
import chat.stoat.markdown.CUSTOM_EMOTE_ELEMENT_TYPE
import chat.stoat.markdown.MASS_MENTION_ELEMENT_TYPE
import chat.stoat.markdown.ROLE_MENTION_ELEMENT_TYPE
import chat.stoat.markdown.SPOILER_DELIMITER_TOKEN_TYPE
import chat.stoat.markdown.SPOILER_ELEMENT_TYPE
import chat.stoat.markdown.StoatMarkdownFlavour
import chat.stoat.markdown.TIMESTAMP_ELEMENT_TYPE
import chat.stoat.markdown.USER_MENTION_ELEMENT_TYPE
import chat.stoat.ui.theme.FragmentMono
import chat.stoat.ui.theme.isThemeDark
import com.mikepenz.markdown.annotator.DefaultAnnotatorSettings
import com.mikepenz.markdown.annotator.buildMarkdownAnnotatedString
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.MarkdownAnnotator
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownInlineContent
import com.mikepenz.markdown.model.rememberMarkdownState
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import io.ratex.RaTeXEngine
import io.ratex.RaTeXFontLoader
import io.ratex.RaTeXRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.parser.MarkdownParser
import java.util.concurrent.ConcurrentHashMap

private data class MathEntry(val key: String, val latex: String, val displayMode: Boolean)
private data class RenderedMath(val renderer: RaTeXRenderer, val size: Size)

private const val CONCEALED_INLINE_PREFIX = "concealed:"

private val mathSizeCache = ConcurrentHashMap<Triple<String, Boolean, Float>, Size>()

// Hard-cap ratex blocks size so adversarial expressions can't crash the layout engine
private const val MAX_MATH_DIMENSION_PX = 4096f
private fun Size.clampForLayout(): Size = Size(
    width.coerceIn(0f, MAX_MATH_DIMENSION_PX),
    height.coerceIn(0f, MAX_MATH_DIMENSION_PX),
)

private fun collectMathEntries(node: ASTNode, content: String): List<MathEntry> {
    val entries = mutableListOf<MathEntry>()
    node.children.forEach { child ->
        when (child.type) {
            GFMElementTypes.INLINE_MATH -> {
                val latex = child.getTextInNode(content).toString().removeSurrounding("$")
                entries += MathEntry("math:i:$latex", latex, false)
            }

            GFMElementTypes.BLOCK_MATH -> {
                val latex = child.getTextInNode(content).toString().removeSurrounding("$$").trim()
                entries += MathEntry("math:b:$latex", latex, true)
            }

            else -> entries += collectMathEntries(child, content)
        }
    }
    return entries
}

private fun collectEmoteUlids(node: ASTNode, content: String): List<String> {
    val ulids = mutableListOf<String>()
    node.children.forEach { child ->
        when (child.type) {
            CUSTOM_EMOTE_ELEMENT_TYPE -> ulids += child.getTextInNode(content).toString()
                .removeSurrounding(":")

            else -> ulids += collectEmoteUlids(child, content)
        }
    }
    return ulids
}

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
    val context = LocalContext.current
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSize.toPx() }
    val scope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val spoilerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurfaceArgb = MaterialTheme.colorScheme.onSurface.toArgb()
    val revealedSpoilers = remember(state) { mutableStateMapOf<Int, Boolean>() }
    val mathEntries = remember(state) {
        (state as? State.Success)?.let {
            collectMathEntries(it.node, it.content).distinctBy { e -> e.key }
        } ?: emptyList()
    }
    val emoteUlids = remember(state) {
        (state as? State.Success)?.let {
            collectEmoteUlids(it.node, it.content).distinct()
        } ?: emptyList()
    }
    var mathSizes by remember(mathEntries, fontSizePx) {
        mutableStateOf(buildMap {
            mathEntries.forEach { entry ->
                mathSizeCache[Triple(
                    entry.latex,
                    entry.displayMode,
                    fontSizePx
                )]?.let { put(entry.key, it) }
            }
        })
    }
    // RaTeXView permanently cancels its render scope when detached. Lazy list items can be
    // reattached, so retain the renderer as Compose state and draw it without an embedded View.
    var renderedMath by remember(mathEntries, fontSizePx, onSurfaceArgb) {
        mutableStateOf<Map<String, RenderedMath>>(emptyMap())
    }
    LaunchedEffect(mathEntries, fontSizePx, onSurfaceArgb) {
        if (mathEntries.isEmpty()) return@LaunchedEffect
        withContext(Dispatchers.IO) { RaTeXFontLoader.ensureLoaded(context) }
        val newRenders = withContext(Dispatchers.Default) {
            buildMap {
                mathEntries.forEach { entry ->
                    try {
                        val dl =
                            RaTeXEngine.parseBlocking(entry.latex, entry.displayMode, onSurfaceArgb)
                        val r = RaTeXRenderer(dl, fontSizePx) { RaTeXFontLoader.getTypeface(it) }
                        val size = Size(r.widthPx, r.totalHeightPx).clampForLayout()
                        mathSizeCache[Triple(entry.latex, entry.displayMode, fontSizePx)] = size
                        put(entry.key, RenderedMath(r, size))
                    } catch (_: Exception) {
                    }
                }
            }
        }
        if (newRenders.isNotEmpty()) {
            renderedMath = newRenders
            mathSizes = mathSizes + newRenders.mapValues { it.value.size }
        }
    }
    val mentionLinkStyle = remember(primaryColor) {
        TextLinkStyles(
            SpanStyle(
                color = primaryColor,
                background = primaryColor.copy(alpha = 0.2f)
            )
        )
    }
    val textLinkStyle = remember(primaryColor) {
        TextLinkStyles(
            SpanStyle(
                color = primaryColor,
                textDecoration = TextDecoration.Underline,
            )
        )
    }
    val codeSpanStyle = remember(fontSize, surfaceVariantColor) {
        SpanStyle(
            background = surfaceVariantColor,
            fontFamily = FragmentMono,
            fontSize = fontSize,
        )
    }
    val annotator = remember(
        serverId,
        mentionLinkStyle,
        surfaceVariantColor,
        textLinkStyle,
        codeSpanStyle,
        revealedSpoilers,
    ) {
        var concealedSpoilerDepth = 0
        lateinit var spoilerAwareAnnotator: MarkdownAnnotator
        spoilerAwareAnnotator = markdownAnnotator { content, child ->
            when (child.type) {
                SPOILER_ELEMENT_TYPE -> {
                    val spoilerId = child.startOffset
                    val revealed = revealedSpoilers[spoilerId] == true
                    if (revealed) {
                        buildMarkdownAnnotatedString(
                            content = content,
                            children = child.children.drop(1).dropLast(1),
                            annotatorSettings = DefaultAnnotatorSettings(
                                linkTextSpanStyle = textLinkStyle,
                                codeSpanStyle = codeSpanStyle,
                                annotator = spoilerAwareAnnotator,
                            ),
                        )
                    } else {
                        val concealedContent = buildAnnotatedString {
                            concealedSpoilerDepth++
                            try {
                                buildMarkdownAnnotatedString(
                                    content = content,
                                    children = child.children.drop(1).dropLast(1),
                                    annotatorSettings = DefaultAnnotatorSettings(
                                        linkTextSpanStyle = textLinkStyle,
                                        codeSpanStyle = codeSpanStyle,
                                        annotator = spoilerAwareAnnotator,
                                    ),
                                )
                            } finally {
                                concealedSpoilerDepth--
                            }
                        }.flatMapAnnotations { annotation ->
                            if (annotation.item is LinkAnnotation) emptyList()
                            else listOf(annotation)
                        }
                        withLink(
                            LinkAnnotation.Clickable(
                                tag = "$SPOILER_LINK_TAG_PREFIX$spoilerId",
                                linkInteractionListener = LinkInteractionListener {
                                    revealedSpoilers[spoilerId] = true
                                },
                            )
                        ) {
                            append(concealedContent)
                        }
                    }
                    true
                }

                SPOILER_DELIMITER_TOKEN_TYPE -> {
                    append(child.getTextInNode(content))
                    true
                }

                CUSTOM_EMOTE_ELEMENT_TYPE -> {
                    val ulid = child.getTextInNode(content).toString().removeSurrounding(":")
                    val name = StoatAPI.emojiCache[ulid]?.name ?: ":$ulid:"
                    val prefix = if (concealedSpoilerDepth > 0) CONCEALED_INLINE_PREFIX else ""
                    appendInlineContent("${prefix}emote:$ulid", name)
                    true
                }

                GFMElementTypes.INLINE_MATH -> {
                    val latex = child.getTextInNode(content).toString().removeSurrounding("$")
                    if (latex.isNotEmpty()) {
                        val prefix =
                            if (concealedSpoilerDepth > 0) CONCEALED_INLINE_PREFIX else ""
                        appendInlineContent("${prefix}math:i:$latex", latex)
                    }
                    true
                }

                GFMElementTypes.BLOCK_MATH -> {
                    val latex =
                        child.getTextInNode(content).toString().removeSurrounding("$$").trim()
                    if (latex.isNotEmpty()) {
                        val prefix =
                            if (concealedSpoilerDepth > 0) CONCEALED_INLINE_PREFIX else ""
                        appendInlineContent("${prefix}math:b:$latex", latex)
                    }
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
        spoilerAwareAnnotator
    }
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
                            val stoatLink = parsed.toStoatWebLinkOrNull()
                            if (stoatLink != null) {
                                scope.launch {
                                    ActionChannel.send(stoatLink.toNavigationAction())
                                }
                                return
                            }
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
                textLink = textLinkStyle,
            ),
            inlineContent = markdownInlineContent(
                buildMap {
                    mathEntries.forEach { entry ->
                        val measured = mathSizes[entry.key]
                        val (widthSp, heightSp) = if (measured != null) {
                            with(density) { measured.width.toSp() to measured.height.toSp() }
                        } else if (entry.displayMode) {
                            fontSize * 16f to fontSize * 3f
                        } else {
                            fontSize * 4f to fontSize * 1.5f
                        }
                        put(
                            entry.key, InlineTextContent(
                                Placeholder(
                                    width = widthSp,
                                    height = heightSp,
                                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                                )
                            ) {
                                Canvas(Modifier.fillMaxSize()) {
                                    renderedMath[entry.key]?.renderer?.draw(
                                        drawContext.canvas.nativeCanvas
                                    )
                                }
                            })
                        put(
                            "$CONCEALED_INLINE_PREFIX${entry.key}",
                            InlineTextContent(
                                Placeholder(
                                    width = widthSp,
                                    height = heightSp,
                                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                                )
                            ) { Box(Modifier.fillMaxSize()) },
                        )
                    }
                    emoteUlids.forEach { ulid ->
                        val placeholder = Placeholder(
                            width = fontSize * 1.5f,
                            height = fontSize * 1.5f,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                        )
                        put(
                            "emote:$ulid", InlineTextContent(
                                placeholder
                            ) { _ ->
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
                                                    scope.launch {
                                                        ActionChannel.send(
                                                            Action.EmoteInfo(
                                                                ulid
                                                            )
                                                        )
                                                    }
                                                },
                                        )
                                    }
                                }
                            })
                        put(
                            "${CONCEALED_INLINE_PREFIX}emote:$ulid",
                            InlineTextContent(placeholder) { Box(Modifier.fillMaxSize()) },
                        )
                    }
                }
            ),
            components = markdownComponents(
                text = {
                    SpoilerMarkdownText(
                        content = it.content,
                        node = it.node,
                        style = it.typography.text,
                        spoilerColor = spoilerColor,
                    )
                },
                paragraph = {
                    SpoilerMarkdownText(
                        content = it.content,
                        node = it.node,
                        style = it.typography.paragraph,
                        spoilerColor = spoilerColor,
                    )
                },
                heading1 = {
                    SpoilerMarkdownText(
                        content = it.content,
                        node = it.node,
                        style = it.typography.h1,
                        spoilerColor = spoilerColor,
                        contentChildType = MarkdownTokenTypes.ATX_CONTENT,
                        isHeading = true,
                    )
                },
                heading2 = {
                    SpoilerMarkdownText(
                        content = it.content,
                        node = it.node,
                        style = it.typography.h2,
                        spoilerColor = spoilerColor,
                        contentChildType = MarkdownTokenTypes.ATX_CONTENT,
                        isHeading = true,
                    )
                },
                heading3 = {
                    SpoilerMarkdownText(
                        content = it.content,
                        node = it.node,
                        style = it.typography.h3,
                        spoilerColor = spoilerColor,
                        contentChildType = MarkdownTokenTypes.ATX_CONTENT,
                        isHeading = true,
                    )
                },
                heading4 = {
                    SpoilerMarkdownText(
                        content = it.content,
                        node = it.node,
                        style = it.typography.h4,
                        spoilerColor = spoilerColor,
                        contentChildType = MarkdownTokenTypes.ATX_CONTENT,
                        isHeading = true,
                    )
                },
                heading5 = {
                    SpoilerMarkdownText(
                        content = it.content,
                        node = it.node,
                        style = it.typography.h5,
                        spoilerColor = spoilerColor,
                        contentChildType = MarkdownTokenTypes.ATX_CONTENT,
                        isHeading = true,
                    )
                },
                heading6 = {
                    SpoilerMarkdownText(
                        content = it.content,
                        node = it.node,
                        style = it.typography.h6,
                        spoilerColor = spoilerColor,
                        contentChildType = MarkdownTokenTypes.ATX_CONTENT,
                        isHeading = true,
                    )
                },
                setextHeading1 = {
                    SpoilerMarkdownText(
                        content = it.content,
                        node = it.node,
                        style = it.typography.h1,
                        spoilerColor = spoilerColor,
                        contentChildType = MarkdownTokenTypes.SETEXT_CONTENT,
                        isHeading = true,
                    )
                },
                setextHeading2 = {
                    SpoilerMarkdownText(
                        content = it.content,
                        node = it.node,
                        style = it.typography.h2,
                        spoilerColor = spoilerColor,
                        contentChildType = MarkdownTokenTypes.SETEXT_CONTENT,
                        isHeading = true,
                    )
                },
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
