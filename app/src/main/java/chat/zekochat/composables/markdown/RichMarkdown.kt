package chat.zekochat.composables.markdown

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.zekochat.api.settings.Experiments
import chat.zekochat.markdown.jbm.JBM
import chat.zekochat.markdown.jbm.JBMRenderer
import chat.zekochat.ndk.Stendal

@OptIn(JBM::class)
@Composable
fun RichMarkdown(input: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        if (Experiments.useKotlinBasedMarkdownRenderer.isEnabled) {
            JBMRenderer(input)
        } else {
            MarkdownTree(node = Stendal.render(input))
        }
    }
}