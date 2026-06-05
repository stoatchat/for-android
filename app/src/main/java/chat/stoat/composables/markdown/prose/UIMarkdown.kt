package chat.stoat.composables.markdown.prose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.rememberMarkdownState

@Composable
fun UIMarkdown(input: String, modifier: Modifier = Modifier) {
    Markdown(
        markdownState = rememberMarkdownState(content = input, immediate = true),
        components = markdownComponents(image = {}, inlineImage = {}),
        modifier = modifier,
    )
}
