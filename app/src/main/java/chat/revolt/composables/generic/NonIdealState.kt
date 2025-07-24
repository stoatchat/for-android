package chat.revolt.composables.generic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.revolt.R

private val NoneLambda = @Composable { throw UnsupportedOperationException() }

@Composable
fun NonIdealState(
    icon: @Composable (Dp) -> Unit,
    title: @Composable () -> Unit,
    description: @Composable () -> Unit = NoneLambda,
    actions: @Composable () -> Unit = NoneLambda
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
            icon(38.dp)
        }

        if (description != NoneLambda) Spacer(modifier = Modifier.height(16.dp))

        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.headlineMedium.copy(
                textAlign = TextAlign.Center
            )
        ) {
            title()
        }

        if (description != NoneLambda) {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Center
                )
            ) {
                description()
            }
        }

        if (actions != NoneLambda) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                actions()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NonIdealStatePreview() {
    NonIdealState(
        icon = {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(it)
            )
        },
        title = { Text("Channel") },
        description = { Text("You are not in any channels.") },
        actions = {
            Button(onClick = {}) {
                Text("Create a channel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun NonIdealStatePreviewNoActions() {
    NonIdealState(
        icon = {
            Icon(
                painter = painterResource(R.drawable.icn_error_24dp),
                contentDescription = null,
                modifier = Modifier.size(it)
            )
        },
        title = { Text("Error") },
        description = { Text("Could not load channels.") }
    )
}

@Preview(showBackground = true)
@Composable
fun NonIdealStatePreviewNoDescription() {
    NonIdealState(
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(it)
            )
        },
        title = { Text("No channels") }
    )
}
