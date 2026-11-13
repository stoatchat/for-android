package chat.stoat.core.designsystem.components

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
    val contentColour = when {
        dangerous -> MaterialTheme.colorScheme.error
        special -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    fun tinted(content: @Composable () -> Unit): @Composable () -> Unit = {
        CompositionLocalProvider(LocalContentColor provides contentColour, content = content)
    }

    Box(
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp)
    ) {
        ListItem(
            colors = TransparentListItemColours,
            leadingContent = tinted(leadingContent),
            supportingContent = supportingContent?.let(::tinted),
            trailingContent = trailingContent?.let(::tinted),
            content = tinted(headlineContent)
        )
    }
}
