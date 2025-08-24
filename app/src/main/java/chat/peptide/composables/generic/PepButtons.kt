package chat.peptide.composables.generic

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SquareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors().copy(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ),
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = colors,
        enabled = enabled,
        shape = MaterialTheme.shapes.small.copy(
            topStart = CornerSize(8.dp),
            topEnd = CornerSize(8.dp),
            bottomStart = CornerSize(8.dp),
            bottomEnd = CornerSize(8.dp)
        )
    ) {
        content()
    }
}

@Composable
fun PepTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors().copy(
            contentColor = MaterialTheme.colorScheme.onBackground,
        ),
        shape = MaterialTheme.shapes.small.copy(
            topStart = CornerSize(8.dp),
            topEnd = CornerSize(8.dp),
            bottomStart = CornerSize(8.dp),
            bottomEnd = CornerSize(8.dp)
        ),
        enabled = enabled,
    ) {
        content()
    }
}

@Composable
fun SquareElevatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors().copy(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        shape = MaterialTheme.shapes.small.copy(
            topStart = CornerSize(8.dp),
            topEnd = CornerSize(8.dp),
            bottomStart = CornerSize(8.dp),
            bottomEnd = CornerSize(8.dp)
        )
    ) {
        content()
    }
}