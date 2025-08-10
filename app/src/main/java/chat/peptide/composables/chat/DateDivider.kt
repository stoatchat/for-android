package chat.peptide.composables.chat

import android.icu.text.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant

@Composable
fun DateDivider(instant: Instant, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val formattedDate = remember(instant) {
        DateUtils.formatDateTime(
            context,
            instant.toEpochMilliseconds(),
            DateFormat.FULL
        )
    }

    Column {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = modifier
                .padding(8.dp)
                .alpha(0.8F),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(
                modifier = Modifier.width(44.dp),
                thickness = Dp.Hairline
            )
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = Dp.Hairline
            )
        }
    }
}