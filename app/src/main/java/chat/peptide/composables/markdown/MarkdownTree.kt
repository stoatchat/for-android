package chat.peptide.composables.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.peptide.api.internals.solidColor
import chat.peptide.ndk.AstNode

data class MarkdownTreeConfig(
    val linksClickable: Boolean = true,
    val currentServer: String? = null,
    val fontSizeMultiplier: Float = 1f
)

val LocalMarkdownTreeConfig =
    compositionLocalOf(structuralEqualityPolicy()) { MarkdownTreeConfig() }

@Composable
private fun Children(node: AstNode) {
    node.children?.forEach { MarkdownTree(it) }
}

@Composable
fun MarkdownTree(node: AstNode) {
    when (node.stringType) {
        "heading" -> {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = when (node.level) {
                        1 -> 32.sp * LocalMarkdownTreeConfig.current.fontSizeMultiplier
                        2 -> 24.sp * LocalMarkdownTreeConfig.current.fontSizeMultiplier
                        3 -> 20.sp * LocalMarkdownTreeConfig.current.fontSizeMultiplier
                        4 -> 16.sp * LocalMarkdownTreeConfig.current.fontSizeMultiplier
                        5 -> 14.sp * LocalMarkdownTreeConfig.current.fontSizeMultiplier
                        else -> 12.sp * LocalMarkdownTreeConfig.current.fontSizeMultiplier
                    }
                )
            ) {
                if (node.startLine != 1) {
                    Box(Modifier.padding(top = 8.dp)) {
                        MarkdownText(node)
                    }
                } else {
                    MarkdownText(node)
                }
            }
        }

        "paragraph" -> {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    fontSize = LocalTextStyle.current.fontSize * LocalMarkdownTreeConfig.current.fontSizeMultiplier
                )
            ) {
                MarkdownText(
                    node,
                    modifier = Modifier
                        .then(
                            if (node.startLine != 1) {
                                Modifier.padding(top = 8.dp)
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }

        "document" -> {
            Children(node)
        }

        "text" -> {
            MarkdownText(node)
        }

        "code_block" -> {
            MarkdownCodeBlock(node)
        }

        "block_quote" -> {
            Row(
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 4.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                // Stripe at the left side of the blockquote
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.solidColor(MaterialTheme.colorScheme.surfaceVariant)
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Children(node)
                }
            }
        }

        else -> {
            Children(node)
        }
    }
}