package chat.zekochat.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import chat.zekochat.composables.generic.SheetHeaderPadding
import chat.zekochat.markdown.jbm.JBM
import chat.zekochat.markdown.jbm.JBMApi
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode

@OptIn(JBM::class)
@Composable
fun JBMDebuggerSheet(content: String, modifier: Modifier = Modifier) {
    val rendered = remember(content) { JBMApi.parse(content) }
    Column {
        SheetHeaderPadding {
            Text(
                text = "Inspect AST",
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Column(modifier.verticalScroll(rememberScrollState())) {
            JBMDebugTree(rendered, content)
        }
    }
}

@Composable
fun JBMDebugTree(node: ASTNode, source: String, modifier: Modifier = Modifier) {
    var showChildren by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier
            .padding(start = 16.dp)
            .clickable { showChildren = !showChildren }) {
        JBMDebugNode(node, source, showChildren)
        if (showChildren && node.children.isNotEmpty()) {
            Column(Modifier.drawBehind {
                drawLine(
                    color = scheme.primary,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }) {
                node.children.forEach { JBMDebugTree(it, source) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JBMDebugNode(node: ASTNode, source: String, showingChildren: Boolean) {
    Column {
        FlowRow {
            if (node.children.isNotEmpty()) {
                Text(if (showingChildren) "▼  " else "▶  ")
            }
            Text(node.type.toString())
            Text(" - ")
            if (node.children.isNotEmpty()) {
                Text("${node.children.size} " + if (node.children.size == 1) "child" else "children")
            } else {
                Text("No children")
            }
        }

        val text = try {
            node.getTextInNode(source)
        } catch (e: Exception) {
            "<exception>"
        }
        Text(
            text.take(50).toString() + if (text.length > 50) "…" else "",
            color = if (text == "<exception>") MaterialTheme.colorScheme.error else LocalContentColor.current,
            modifier = Modifier.padding(4.dp)
        )
        HorizontalDivider(
            Modifier.fillMaxWidth()
        )
    }
}