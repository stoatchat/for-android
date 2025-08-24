package chat.peptide.sheets

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chat.peptide.R
import chat.peptide.composables.generic.SquareButton

@Composable
fun WebHookUserSheet(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(16.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ux_webhooks),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )
        Text(
            text = stringResource(R.string.user_info_sheet_webhook),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.user_info_sheet_webhook_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        SquareButton(
            onClick = {
                Toast(context).apply {
                    setText(context.getString(R.string.comingsoon_toast))
                    duration = Toast.LENGTH_SHORT
                    show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(text = stringResource(R.string.user_info_sheet_webhook_learn_more))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WebHookUserSheetPreview() {
    Column {
        WebHookUserSheet()
    }
}