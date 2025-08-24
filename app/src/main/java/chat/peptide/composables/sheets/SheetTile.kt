package chat.peptide.composables.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import chat.peptide.api.internals.solidColor

@Composable
fun SheetTile(
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    contentPreview: @Composable () -> Unit,
    clickable: Boolean = true,
    backgroundBrush: Brush = Brush.solidColor(MaterialTheme.colorScheme.surfaceContainer),
    content: @Composable () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    if (isExpanded) {
        Dialog(onDismissRequest = {
            isExpanded = false
        }) {
            BoxWithConstraints {
                Column(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(24.dp)
                        .width(maxWidth * 0.85f)
                        .heightIn(max = maxHeight * 0.85f)
                ) {
                    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.headlineMedium) {
                        header()
                    }

                    Spacer(Modifier.height(8.dp))

                    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
                        content()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .then(if (clickable) Modifier.clickable { isExpanded = true } else Modifier)
            .background(backgroundBrush)
            .padding(16.dp)
            .height(128.dp)
            .then(modifier)
    ) {
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.headlineSmall) {
            header()
        }

        Spacer(Modifier.height(8.dp))

        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
            contentPreview()
        }
    }
}