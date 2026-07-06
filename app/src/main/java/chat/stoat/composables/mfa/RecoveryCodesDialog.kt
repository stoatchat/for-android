package chat.stoat.composables.mfa

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.internals.Platform
import chat.stoat.ui.theme.FragmentMono

@Composable
fun RecoveryCodesDialog(codes: List<String>, onDismiss: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val resources = LocalResources.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_mfa_recovery_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.settings_mfa_recovery_dialog_hint))

                SelectionContainer {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(16.dp)
                    ) {
                        codes.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                row.forEach { code ->
                                    Text(
                                        text = code,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = FragmentMono
                                        ),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                clipboardManager.setText(AnnotatedString(codes.joinToString("\n")))
                if (Platform.needsShowClipboardNotification()) {
                    Toast.makeText(
                        context,
                        resources.getString(R.string.copied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }) {
                Text(stringResource(R.string.settings_mfa_recovery_copy))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_mfa_recovery_dialog_done))
            }
        }
    )
}
