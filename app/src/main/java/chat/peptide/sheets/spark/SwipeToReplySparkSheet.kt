package chat.peptide.sheets.spark

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chat.peptide.R
import chat.peptide.composables.vectorassets.SwipeToReplySpark

@Composable
fun SwipeToReplySparkSheet(
    onDismissSheet: () -> Unit = {},
    onOpenOptions: () -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Image(SwipeToReplySpark, contentDescription = null)

        Spacer(Modifier) // Counts towards the vertical arrangement

        Text(
            text = stringResource(R.string.spark_swipe_to_reply),
            style = MaterialTheme.typography.titleMedium,
        )

        Text(
            text = stringResource(R.string.spark_swipe_to_reply_description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onOpenOptions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.spark_swipe_to_reply_customise))
            }
            Button(
                onClick = onDismissSheet,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.spark_swipe_to_reply_cta))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SwipeToReplySparkSheetPreview() {
    Box(Modifier.padding(16.dp)) {
        SwipeToReplySparkSheet()
    }
}