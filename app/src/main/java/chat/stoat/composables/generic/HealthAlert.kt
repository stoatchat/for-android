package chat.stoat.composables.generic

import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import chat.stoat.core.model.schemas.HealthNotice
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.service_health_alert
import chat.stoat.shared.resources.service_health_alert_actions_default
import chat.stoat.shared.resources.service_health_alert_actions_dismiss
import chat.stoat.shared.resources.service_health_alert_body_default
import org.jetbrains.compose.resources.stringResource

@Composable
fun HealthAlert(notice: HealthNotice, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val backgroundColour = MaterialTheme.colorScheme.background

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
                            val customTab = CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .setDefaultColorSchemeParams(
                                    CustomTabColorSchemeParams.Builder()
                                        .setToolbarColor(backgroundColour.toArgb())
                                        .build()
                                )
                                .build()
                            customTab.launchUrl(context, action.href?.toUri() ?: return@TextButton)
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