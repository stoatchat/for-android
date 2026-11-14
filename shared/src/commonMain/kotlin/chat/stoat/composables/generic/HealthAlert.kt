package chat.stoat.composables.generic

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import chat.stoat.core.model.schemas.HealthNotice
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.service_health_alert
import chat.stoat.shared.resources.service_health_alert_actions_default
import chat.stoat.shared.resources.service_health_alert_actions_dismiss
import chat.stoat.shared.resources.service_health_alert_body_default
import org.jetbrains.compose.resources.stringResource

@Composable
fun HealthAlert(notice: HealthNotice, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = {
            // Purposefully empty
        },
        title = {
            Text(stringResource(Res.string.service_health_alert))
        },
        text = {
            Text(notice.alert?.text ?: stringResource(Res.string.service_health_alert_body_default))
        },
        confirmButton = {
            notice.alert?.actions?.map { action ->
                when (action.type) {
                    "external" -> TextButton(
                        onClick = {
                            uriHandler.openUri(action.href ?: return@TextButton)
                        }
                    ) {
                        Text(
                            action.text
                                ?: stringResource(Res.string.service_health_alert_actions_default)
                        )
                    }
                }
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.service_health_alert_actions_dismiss))
            }
        }
    )
}