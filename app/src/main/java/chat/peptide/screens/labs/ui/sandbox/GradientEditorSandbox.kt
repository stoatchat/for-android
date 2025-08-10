package chat.peptide.screens.labs.ui.sandbox

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import chat.peptide.composables.generic.GradientStopEditor
import chat.peptide.settings.dsl.SettingsPage

@Composable
fun GradientEditorSandbox(navController: NavController) {
    var segmentCount by remember { mutableIntStateOf(10) }

    SettingsPage(
        navController,
        title = {
            Text(
                text = "Gradient Editor Sandbox",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    if (segmentCount >= 2)
                        segmentCount--
                    else showSnackbar("Minimum of 2 segments required.")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("-")
            }
            Text(
                "$segmentCount segments",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            TextButton(
                onClick = { segmentCount++ },
                modifier = Modifier.weight(1f)
            ) {
                Text("+")
            }
        }
        GradientStopEditor(
            segments = segmentCount,
            stops = emptyList(),
            onStopsChanged = {},
            _onClickSegment = { showSnackbar("Clicked segment $it") },
            modifier = Modifier.height(24.dp)
        )
    }
}