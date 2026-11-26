package chat.stoat.composables.markdown.prose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.mikepenz.markdown.annotator.annotatorSettings
import com.mikepenz.markdown.annotator.buildMarkdownAnnotatedString
import com.mikepenz.markdown.compose.elements.MarkdownText
import org.intellij.markdown.IElementType
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType

internal const val SPOILER_LINK_TAG_PREFIX = "spoiler:"

@Composable
internal fun SpoilerMarkdownText(
    content: String,
    node: ASTNode,
    style: TextStyle,
    spoilerColor: Color,
    modifier: Modifier = Modifier,
    contentChildType: IElementType? = null,
    isHeading: Boolean = false,
) {
    val childNode = contentChildType?.run(node::findChildOfType) ?: node
    val settings = annotatorSettings()
    val styledText = buildAnnotatedString {
        pushStyle(style.toSpanStyle())
        buildMarkdownAnnotatedString(
            content = content,
            node = childNode,
            annotatorSettings = settings,
        )
        pop()
    }
    val hiddenSpoilers = styledText.getLinkAnnotations(0, styledText.length).filter { range ->
        val link = range.item as? LinkAnnotation.Clickable
        link?.tag?.startsWith(SPOILER_LINK_TAG_PREFIX) == true
    }
    var layoutResult by remember(styledText) { mutableStateOf<TextLayoutResult?>(null) }
    val semanticsModifier = if (isHeading) Modifier.semantics { heading() } else Modifier

    MarkdownText(
        content = styledText,
        node = node,
        style = style,
        sourceContent = content,
        onTextLayout = { result, _ -> layoutResult = result },
        modifier = modifier
            .then(semanticsModifier)
            .drawWithContent {
                drawContent()
                val layout = layoutResult ?: return@drawWithContent
                hiddenSpoilers.forEach { spoiler ->
                    if (spoiler.start < spoiler.end) {
                        drawPath(
                            path = layout.getPathForRange(spoiler.start, spoiler.end),
                            color = spoilerColor,
                        )
                    }
                }
            },
    )
}
