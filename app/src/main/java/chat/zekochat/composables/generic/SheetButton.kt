package chat.zekochat.composables.generic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ListItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import chat.zekochat.internals.extensions.TransparentListItemColours

@Composable
fun SheetButton(
    headlineContent: @Composable () -> Unit,
    leadingContent: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    dangerous: Boolean = false,
    special: Boolean = false
) {
    Box(
        modifier = modifier
            .clickable { onClick() }
    ) {
        ListItem(
            colors = TransparentListItemColours,
            headlineContent = {
                CompositionLocalProvider(
                    value = if (dangerous) {
                        LocalContentColor provides MaterialTheme.colorScheme.error
                    } else if (special) {
                        LocalContentColor provides MaterialTheme.colorScheme.primary
                    } else {
                        LocalContentColor provides MaterialTheme.colorScheme.onSurface
                    }
                ) {
                    headlineContent()
                }
            },
            leadingContent = {
                CompositionLocalProvider(
                    value = if (dangerous) {
                        LocalContentColor provides MaterialTheme.colorScheme.error
                    } else if (special) {
                        LocalContentColor provides MaterialTheme.colorScheme.primary
                    } else {
                        LocalContentColor provides MaterialTheme.colorScheme.onSurface
                    }
                ) {
                    leadingContent()
                }
            },
            supportingContent = {
                supportingContent?.run {
                    CompositionLocalProvider(
                        value = if (dangerous) {
                            LocalContentColor provides MaterialTheme.colorScheme.error
                        } else if (special) {
                            LocalContentColor provides MaterialTheme.colorScheme.primary
                        } else {
                            LocalContentColor provides MaterialTheme.colorScheme.onSurface
                        }
                    ) {
                        this()
                    }
                }
            },
            trailingContent = {
                trailingContent?.run {
                    CompositionLocalProvider(
                        value = if (dangerous) {
                            LocalContentColor provides MaterialTheme.colorScheme.error
                        } else if (special) {
                            LocalContentColor provides MaterialTheme.colorScheme.primary
                        } else {
                            LocalContentColor provides MaterialTheme.colorScheme.onSurface
                        }
                    ) {
                        this()
                    }
                }
            }
        )
    }
}

@Preview
@Composable
private fun SheetButtonPreview() {
    MaterialTheme {
        SheetButton(
            headlineContent = { Text("Headline") },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
            onClick = {},
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Preview
@Composable
private fun SheetButtonWithSupportingContentPreview() {
    MaterialTheme {
        SheetButton(
            headlineContent = { Text("Headline") },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
            supportingContent = { Text("Supporting content") },
            onClick = {},
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Preview
@Composable
private fun SheetButtonWithTrailingContentPreview() {
    MaterialTheme {
        SheetButton(
            headlineContent = { Text("Headline") },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
            trailingContent = { Text("Trailing") },
            onClick = {},
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Preview
@Composable
private fun DangerousSheetButtonPreview() {
    MaterialTheme {
        SheetButton(
            headlineContent = { Text("Headline") },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
            onClick = {},
            dangerous = true,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Preview
@Composable
private fun SpecialSheetButtonPreview() {
    MaterialTheme {
        SheetButton(
            headlineContent = { Text("Headline") },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
            onClick = {},
            special = true,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        )
    }
}


