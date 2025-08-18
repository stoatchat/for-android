package chat.peptide.composables.generic

import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import chat.peptide.R
import chat.peptide.api.schemas.HealthNotice

@Composable
fun HealthAlert(notice: HealthNotice, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val backgroundColour = MaterialTheme.colorScheme.background

    AlertDialog(
        onDismissRequest = {
            // Purposefully empty
        },
        title = {
            Text(stringResource(R.string.service_health_alert))
        },
        text = {
            Text(notice.alert?.text ?: stringResource(R.string.service_health_alert_body_default))
        },
        confirmButton = {
            notice.alert?.actions?.map { action ->
                when (action.type) {
                    "external" -> PepTextButton(
                        onClick = {
                            val customTab = CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .setDefaultColorSchemeParams(
                                    CustomTabColorSchemeParams.Builder()
                                        .setToolbarColor(backgroundColour.toArgb())
                                        .build()
                                )
                                .build()
                            customTab.launchUrl(context, action.href?.toUri() ?: return@PepTextButton)
                        }
                    ) {
                        Text(
                            action.text
                                ?: stringResource(R.string.service_health_alert_actions_default)
                        )
                    }
                }
            }
            PepTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.service_health_alert_actions_dismiss))
            }
        }
    )
}