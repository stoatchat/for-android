package chat.stoat.composables.generic

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.activities.StoatTweenColour
import chat.stoat.activities.StoatTweenDp
import chat.stoat.internals.server.PermissionOverrideValue

private val PermissionOverrideOptions = listOf(
    PermissionOverrideValue.Allow,
    PermissionOverrideValue.Neutral,
    PermissionOverrideValue.Deny,
)

@Composable
fun PermissionOverridePicker(
    value: PermissionOverrideValue,
    enabled: Boolean,
    onValueChange: (PermissionOverrideValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colourScheme = MaterialTheme.colorScheme
    val transition = updateTransition(value, label = "permission override")
    val highlightOffset by transition.animateDp(
        transitionSpec = { StoatTweenDp },
        label = "permission override position",
    ) { state -> (PermissionOverrideOptions.indexOf(state) * 48).dp }
    val highlightColor by transition.animateColor(
        transitionSpec = { StoatTweenColour },
        label = "permission override color",
    ) { state ->
        when (state) {
            PermissionOverrideValue.Allow -> colourScheme.onPrimaryContainer
            PermissionOverrideValue.Neutral -> colourScheme.onSecondary
            PermissionOverrideValue.Deny -> colourScheme.onErrorContainer
        }
    }

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(colourScheme.surfaceContainerHigh)
            .alpha(if (enabled) 1f else 0.38f),
    ) {
        Box(
            Modifier
                .offset(x = highlightOffset)
                .size(48.dp)
                .background(highlightColor)
        )

        Row(Modifier.selectableGroup()) {
            PermissionOverrideOptions.forEach { option ->
                val selected = value == option
                val contentColor by transition.animateColor(
                    transitionSpec = { StoatTweenColour },
                    label = "${option.name.lowercase()} permission override icon",
                ) { state ->
                    if (state == option) {
                        when (option) {
                            PermissionOverrideValue.Allow -> colourScheme.primaryContainer
                            PermissionOverrideValue.Neutral -> colourScheme.secondary
                            PermissionOverrideValue.Deny -> colourScheme.errorContainer
                        }
                    } else {
                        colourScheme.onSurface
                    }
                }

                PermissionOverrideOption(
                    option = option,
                    selected = selected,
                    enabled = enabled,
                    contentColor = contentColor,
                    onClick = { onValueChange(option) },
                )
            }
        }
    }
}

@Composable
private fun PermissionOverrideOption(
    option: PermissionOverrideValue,
    selected: Boolean,
    enabled: Boolean,
    contentColor: Color,
    onClick: () -> Unit,
) {
    val label = stringResource(
        when (option) {
            PermissionOverrideValue.Allow -> R.string.permission_allow
            PermissionOverrideValue.Neutral -> R.string.permission_neutral
            PermissionOverrideValue.Deny -> R.string.permission_deny
        }
    )
    val icon = when (option) {
        PermissionOverrideValue.Allow -> R.drawable.ic_check_24dp
        PermissionOverrideValue.Neutral -> R.drawable.ic_remove_24dp
        PermissionOverrideValue.Deny -> R.drawable.ic_close_24dp
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .selectable(
                selected = selected,
                interactionSource = null,
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}
