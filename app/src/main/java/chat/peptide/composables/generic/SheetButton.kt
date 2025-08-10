package chat.peptide.composables.generic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.peptide.internals.extensions.TransparentListItemColours

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
            .padding(horizontal = 16.dp)
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