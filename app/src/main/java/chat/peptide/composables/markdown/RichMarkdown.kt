package chat.peptide.composables.markdown

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.peptide.api.settings.Experiments
import chat.peptide.markdown.jbm.JBM
import chat.peptide.markdown.jbm.JBMRenderer
import chat.peptide.ndk.Stendal

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