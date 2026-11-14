package chat.stoat.composables.generic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.stoat.core.designsystem.icons.StoatIcons
import chat.stoat.core.designsystem.icons.painter
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.list_detail_placeholder
import org.jetbrains.compose.resources.stringResource

@Composable
fun ListDetailPlaceholder(modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                painter = StoatIcons.Click.painter(),
                contentDescription = null,
                tint = LocalContentColor.current.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = stringResource(Res.string.list_detail_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
