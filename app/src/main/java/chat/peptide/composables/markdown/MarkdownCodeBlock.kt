package chat.peptide.composables.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import chat.peptide.ndk.AstNode
import chat.peptide.ui.theme.FragmentMono

@Composable
fun MarkdownCodeBlock(node: AstNode, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .then(
                if (node.startLine != 1) {
                    Modifier.padding(top = 8.dp)
                } else {
                    Modifier
                }
            )
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(8.dp)
    ) {
        Text(
            text = node.text?.removeSuffix("\n") ?: "",
            fontFamily = FragmentMono,
        )
    }
}