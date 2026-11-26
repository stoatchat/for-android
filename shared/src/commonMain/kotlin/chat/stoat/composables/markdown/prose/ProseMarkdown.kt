package chat.stoat.composables.markdown.prose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHeader
import com.mikepenz.markdown.compose.elements.MarkdownParagraph
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.elements.MarkdownCheckBox
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState

@Composable
fun ProseMarkdown(markdownText: String, modifier: Modifier = Modifier) {
    Markdown(
        markdownState = rememberMarkdownState(content = markdownText),
        imageTransformer = Coil3ImageTransformerImpl,
        typography = markdownTypography(
            h1 = MaterialTheme.typography.displaySmall.copy(lineHeight = 45.sp),
            h2 = MaterialTheme.typography.titleLarge,
            h3 = MaterialTheme.typography.titleMedium,
            h4 = MaterialTheme.typography.bodyMedium,
            h5 = MaterialTheme.typography.bodySmall,
            h6 = MaterialTheme.typography.labelSmall,
            paragraph = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
        ),
        components = markdownComponents(
            checkbox = {
                MarkdownCheckBox(
                    it.content,
                    it.node,
                    it.typography.text
                )
            },
            paragraph = {
                MarkdownParagraph(
                    it.content,
                    it.node,
                    style = it.typography.paragraph,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            },
            heading1 = {
                Box(
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                ) {
                    MarkdownHeader(
                        it.content,
                        it.node,
                        style = it.typography.h1,
                    )
                }
            }
        ),
        modifier = modifier
    )
}