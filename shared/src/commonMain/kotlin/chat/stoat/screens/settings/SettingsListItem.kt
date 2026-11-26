package chat.stoat.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import chat.stoat.core.designsystem.theme.StoatTweenColour
import chat.stoat.navigation.isListDetailSelected
import chat.stoat.navigation.listDetailItem

@Composable
fun SettingsListItem(
    first: Boolean = false,
    last: Boolean = false,
    headlineContent: @Composable () -> Unit,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    @Suppress("ModifierParameter") modifier: Modifier = Modifier,
    detailRoute: NavKey? = null,
) {
    val selected = detailRoute != null && isListDetailSelected(detailRoute)
    val colours = MaterialTheme.colorScheme

    @Composable
    fun animated(selectedColour: Color, colour: Color) =
        animateColorAsState(
            if (selected) selectedColour else colour,
            animationSpec = StoatTweenColour
        ).value

    val containerColour = animated(colours.secondaryContainer, colours.surfaceContainer)
    val headlineColour = animated(colours.onSecondaryContainer, colours.onSurface)
    val secondaryColour = animated(colours.onSecondaryContainer, colours.onSurfaceVariant)
    val iconColour = animated(colours.onSecondaryContainer, colours.onBackground)

    CompositionLocalProvider(LocalSettingsIconColour provides iconColour) {
        ListItem(
            colors = ListItemDefaults.colors(
                containerColor = containerColour,
                headlineColor = headlineColour,
                supportingColor = secondaryColour,
                leadingIconColor = secondaryColour,
                trailingIconColor = secondaryColour,
            ),
            content = headlineContent,
            supportingContent = supportingContent,
            leadingContent = leadingContent,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(
                    when {
                        first && last -> MaterialTheme.shapes.large
                        first -> MaterialTheme.shapes.extraSmall.copy(
                            topStart = MaterialTheme.shapes.large.topStart,
                            topEnd = MaterialTheme.shapes.large.topEnd
                        )

                        last -> MaterialTheme.shapes.extraSmall.copy(
                            bottomStart = MaterialTheme.shapes.large.bottomStart,
                            bottomEnd = MaterialTheme.shapes.large.bottomEnd
                        )

                        else -> MaterialTheme.shapes.extraSmall
                    }
                )
                .then(detailRoute?.let { Modifier.listDetailItem(it) } ?: Modifier)
                .then(modifier)
        )
    }
}

private val LocalSettingsIconColour = compositionLocalOf { Color.Unspecified }

@Composable
fun SettingsIcon(danger: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalContentColor provides when {
            danger -> MaterialTheme.colorScheme.error
            LocalSettingsIconColour.current.isSpecified -> LocalSettingsIconColour.current
            else -> MaterialTheme.colorScheme.onBackground
        }
    ) {
        content()
    }
}
