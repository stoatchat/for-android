package chat.peptide.composables.screens.settings.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.peptide.R
import chat.peptide.composables.generic.PepTextButton
import chat.peptide.composables.generic.SquareButton

enum class CornerRadiusPreset(val percentage: Int) {
    SHARP(0),
    ROUNDED(15),
    CIRCULAR(50),
}

@Composable
fun CornerRadiusPicker(percentage: Int, onUpdate: (Int) -> Unit, modifier: Modifier = Modifier) {
    var showOtherModal by remember { mutableStateOf(false) }

    if (showOtherModal) {
        var sliderPosition by remember { mutableStateOf(percentage.toFloat()) }
        AlertDialog(
            onDismissRequest = { showOtherModal = false },
            title = {
                Text(
                    text = stringResource(R.string.corner_radius_picker_choose_radius),
                )
            },
            text = {
                Column {
                    Spacer(modifier = Modifier.size(16.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(sliderPosition.toInt()))
                                .background(MaterialTheme.colorScheme.primary)
                                .size(64.dp),
                        )

                        Text(
                            sliderPosition.toInt().toString(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFeatureSettings = "tnum"
                            ),
                        )

                        Slider(
                            value = sliderPosition,
                            onValueChange = { sliderPosition = it },
                            valueRange = 0f..50f,
                            steps = 51
                        )
                    }
                }
            },
            confirmButton = {
                SquareButton(
                    onClick = {
                        showOtherModal = false
                        onUpdate(sliderPosition.toInt())
                    }
                ) {
                    Text(stringResource(R.string.corner_radius_picker_choose_radius_yes))
                }
            },
            dismissButton = {
                PepTextButton(
                    onClick = {
                        showOtherModal = false
                    }
                ) {
                    Text(stringResource(R.string.corner_radius_picker_choose_radius_cancel))
                }
            }
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.clip(MaterialTheme.shapes.medium)
    ) {
        CornerRadiusPickerElement(
            selected = percentage == CornerRadiusPreset.SHARP.percentage,
            onSelect = {
                onUpdate(CornerRadiusPreset.SHARP.percentage)
            },
            icon = painterResource(R.drawable.ux_corner_sharp),
            label = stringResource(R.string.corner_radius_picker_sharp),
        )
        CornerRadiusPickerElement(
            selected = percentage == CornerRadiusPreset.ROUNDED.percentage,
            onSelect = {
                onUpdate(CornerRadiusPreset.ROUNDED.percentage)
            },
            icon = painterResource(R.drawable.ux_corner_rounded),
            label = stringResource(R.string.corner_radius_picker_rounded),
        )
        CornerRadiusPickerElement(
            selected = percentage == CornerRadiusPreset.CIRCULAR.percentage,
            onSelect = {
                onUpdate(CornerRadiusPreset.CIRCULAR.percentage)
            },
            icon = painterResource(R.drawable.ux_corner_circular),
            label = stringResource(R.string.corner_radius_picker_circular),
        )
        CornerRadiusPickerElement(
            selected = percentage !in CornerRadiusPreset.entries.map { it.percentage },
            onSelect = {
                showOtherModal = true
            },
            icon = painterResource(R.drawable.ux_corner_other),
            label = if (percentage !in CornerRadiusPreset.entries.map { it.percentage }) {
                percentage.toString()
            } else stringResource(R.string.corner_radius_picker_other),
            highlightLabel = percentage !in CornerRadiusPreset.entries.map { it.percentage },
        )
    }
}

@Composable
fun RowScope.CornerRadiusPickerElement(
    selected: Boolean,
    onSelect: () -> Unit,
    icon: Painter,
    label: String,
    modifier: Modifier = Modifier,
    highlightLabel: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable { onSelect() }
            .weight(1f)
            .background(
                if (selected)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                else
                    Color.Transparent
            )
            .padding(vertical = 8.dp)
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (highlightLabel)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface,
        )
    }
}