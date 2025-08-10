package chat.peptide.composables.generic

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

@Composable
fun RadioItem(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    description: @Composable () -> Unit = {},
) {
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        headlineContent = {
            label()
        },
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null
            )
        },
        supportingContent = {
            description()
        }
    )
}