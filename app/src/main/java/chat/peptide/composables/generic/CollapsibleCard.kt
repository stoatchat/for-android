package chat.peptide.composables.generic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun CollapsibleCard(
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.padding(10.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    header()
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 5.dp)
                )
            }
            AnimatedVisibility(visible = expanded) {
                content()
            }
        }
    }
}

@Preview
@Composable
fun CollapsibleCardPreview() {
    CollapsibleCard(
        header = {
            Text(
                text = "Header"
            )
        },
        content = {
            Text(
                text = "Content",
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth()
            )
        }
    )
}
