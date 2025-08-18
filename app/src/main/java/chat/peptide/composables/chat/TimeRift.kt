package chat.peptide.composables.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.peptide.R
import chat.peptide.composables.generic.PepTextButton

@Composable
fun TimeRift(modifier: Modifier = Modifier, onMessageLoad: () -> Unit) {
    Column(
        modifier = modifier
            .padding(vertical = 10.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.time_rift_heading),
            color = MaterialTheme.colorScheme.onBackground.copy(
                alpha = 0.9f
            ),
            style = MaterialTheme.typography.titleMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp
            ),
            modifier = Modifier
                .padding(horizontal = 2.5.dp, vertical = 3.dp)
        )

        Spacer(modifier = Modifier.height(5.dp))

        PepTextButton(onClick = onMessageLoad) {
            Text(
                text = stringResource(id = R.string.time_rift_cta),
                color = LocalContentColor.current.copy(
                    alpha = 0.9f
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp
                ),
                modifier = Modifier
                    .padding(horizontal = 2.5.dp, vertical = 3.dp)
            )
        }
    }
}
