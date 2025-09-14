package chat.zekochat.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chat.zekochat.BuildConfig
import chat.zekochat.R
import chat.zekochat.composables.generic.PepTextButton
import chat.zekochat.composables.generic.SquareButton

@Composable
fun NotificationRationaleDialog(
    onSelected: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(id = R.string.spark_notifications_rationale))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ux_notification_rationale),
                    contentDescription = null,
                    modifier = Modifier
                        .widthIn(max = 200.dp)
                        .aspectRatio(1f)
                        .align(Alignment.CenterHorizontally)
                )
                Text(
                    text = stringResource(id = R.string.spark_notifications_rationale_description)
                )
                if (BuildConfig.DEBUG) {
                    Text(
                        text = buildAnnotatedString {
                            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                            append("Debug build:")
                            pop()
                            append(" Required to open Chucker for network debugging. ")
                            append("You can show this dialogue again from Settings -> Debug.")
                        },
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        dismissButton = {
            PepTextButton(
                onClick = {
                    onSelected(false)
                    onDismiss()
                }
            ) {
                Text(stringResource(id = R.string.spark_notifications_rationale_dismiss))
            }
        },
        confirmButton = {
            SquareButton(onClick = {
                onSelected(true)
                onDismiss()
            }) {
                Text(stringResource(id = R.string.spark_notifications_rationale_cta))
            }
        }
    )
}