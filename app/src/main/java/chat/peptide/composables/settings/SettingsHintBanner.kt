package chat.peptide.composables.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chat.peptide.composables.generic.SquareButton

@Composable
fun SettingsHintBanner(
    title: @Composable () -> Unit,
    description: @Composable () -> Unit,
    button: @Composable (() -> Unit),
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                    title()
                }
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                    description()
                }
            }
        }
        BoxWithConstraints {
            Row(
                modifier = Modifier.widthIn(max = (maxWidth / 3) * 2),
            ) {
                button()
            }
        }
    }
}

@Preview
@Composable
private fun SimpleSettingsHintBannerPreview() {
    SettingsHintBanner(
        title = {
            Text(
                text = "This is a test"
            )
        },
        description = {
            Text(
                text = "With test settings, you can now test the test settings."
            )
        },
        button = {
            SquareButton(onClick = {}) {
                Text("Let's test it out!")
            }
        },
    )
}