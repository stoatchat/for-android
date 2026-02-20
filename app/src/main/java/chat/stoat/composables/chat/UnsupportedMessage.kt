package chat.stoat.composables.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chat.stoat.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UnsupportedMessage(modifier: Modifier = Modifier, context: String? = null) {
    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
        LocalTextStyle provides LocalTextStyle.current.copy(
            fontWeight = FontWeight.Medium
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 16.dp)
                .fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
                    .clip(MaterialShapes.PixelCircle.toShape())
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                    )
                    .size(40.dp)
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_error_24dp),
                        contentDescription = null,
                        tint = LocalContentColor.current,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                if (context != null) stringResource(
                    R.string.message_not_supported_with_context,
                    context
                ) else stringResource(R.string.message_not_supported)
            )
        }
    }
}