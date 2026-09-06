package chat.stoat.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.api.HitRateLimitException
import chat.stoat.api.StoatAPI
import chat.stoat.api.routes.user.patchSelf
import chat.stoat.composables.generic.SheetButton
import chat.stoat.composables.generic.asApiName
import chat.stoat.composables.generic.presenceFromStatus
import chat.stoat.composables.screens.settings.UserOverview
import chat.stoat.composables.settings.profile.StatusPicker
import chat.stoat.core.model.schemas.User
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.asLog
import logcat.logcat

private data class StatusRateLimit(val retryAfterMilliseconds: Int?)

@Composable
fun StatusTextEditDialog(
    selfUser: User,
    initialStatus: String,
    onRateLimited: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val fieldState = rememberTextFieldState(initialStatus)
    var errorText by remember { mutableStateOf<String?>(null) }
    var isConfirmEnabled by remember { mutableStateOf(false) }
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(fieldState.text) {
        errorText = null
        isConfirmEnabled = fieldState.text.length <= 128
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.status_text)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(id = R.string.status_text_explainer)
                )
                TextField(
                    state = fieldState,
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.status_text_placeholder),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    },
                    isError = errorText != null,
                    supportingText = {
                        errorText?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    lineLimits = TextFieldLineLimits.SingleLine
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isConfirmEnabled,
                onClick = {
                    errorText = null
                    if (fieldState.text.length > 128) {
                        errorText = resources.getString(R.string.status_text_error_too_long, 128)
                    } else {
                        if (fieldState.text == initialStatus) {
                            onDismiss()
                            return@TextButton
                        } else if (fieldState.text.isBlank()) {
                            if (selfUser.status?.text == null) {
                                onDismiss()
                                return@TextButton
                            }
                            scope.launch {
                                try {
                                    patchSelf(remove = listOf("StatusText"))
                                    onDismiss()
                                } catch (e: HitRateLimitException) {
                                    logcat {
                                        "Rate limited while removing status text"
                                    }
                                    onRateLimited(e.retryAfterMilliseconds)
                                } catch (e: Exception) {
                                    logcat(LogPriority.ERROR) {
                                        "Failed to remove status text\n${e.asLog()}"
                                    }
                                    errorText =
                                        resources.getString(R.string.status_text_error_other)
                                }
                            }
                        } else {
                            scope.launch {
                                try {
                                    patchSelf(
                                        status = selfUser.status?.copy(
                                            text = fieldState.text.toString().trim()
                                        )
                                    )
                                    onDismiss()
                                } catch (e: HitRateLimitException) {
                                    logcat {
                                        "Rate limited while updating status text\n${e.asLog()}"
                                    }
                                    onRateLimited(e.retryAfterMilliseconds)
                                } catch (e: Exception) {
                                    logcat {
                                        "Failed to update status text\n${e.asLog()}"
                                    }
                                    errorText =
                                        resources.getString(R.string.status_text_error_other)
                                }
                            }
                        }
                    }

                }
            ) {
                Text(
                    text = stringResource(id = R.string.ok)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = stringResource(id = R.string.cancel)
                )
            }
        }
    )
}

@Composable
fun StatusSheet(onBeforeNavigation: () -> Unit, onGoSettings: () -> Unit) {
    val selfUser = StoatAPI.userCache[StoatAPI.selfId]!!
    val scope = rememberCoroutineScope()

    var showStatusEditDialog by remember { mutableStateOf(false) }
    var rateLimit by remember { mutableStateOf<StatusRateLimit?>(null) }

    rateLimit?.let { limit ->
        val waitTime = limit.retryAfterMilliseconds?.let { milliseconds ->
            val seconds = ((milliseconds.coerceAtLeast(0) + 999) / 1_000).coerceAtLeast(1)
            pluralStringResource(R.plurals.status_rate_limited_seconds, seconds, seconds)
        }
        AlertDialog(
            onDismissRequest = { rateLimit = null },
            title = { Text(stringResource(R.string.status_rate_limited_title)) },
            text = {
                Text(
                    if (waitTime != null) {
                        stringResource(R.string.status_rate_limited_description, waitTime)
                    } else {
                        stringResource(R.string.status_rate_limited_description_unknown)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { rateLimit = null }) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }

    if (showStatusEditDialog) {
        StatusTextEditDialog(
            selfUser = selfUser,
            initialStatus = selfUser.status?.text ?: "",
            onRateLimited = {
                showStatusEditDialog = false
                rateLimit = StatusRateLimit(it)
            },
            onDismiss = { showStatusEditDialog = false }
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        UserOverview(selfUser, internalPadding = false)

        Spacer(modifier = Modifier.height(16.dp))

        StatusPicker(
            currentStatus = presenceFromStatus(selfUser.status?.presence, selfUser.online ?: false),
            onStatusChange = {
                scope.launch {
                    try {
                        patchSelf(status = selfUser.status?.copy(presence = it.asApiName()))
                        onBeforeNavigation()
                    } catch (e: HitRateLimitException) {
                        logcat { "Rate limited while updating presence\n${e.asLog()}" }
                        rateLimit = StatusRateLimit(e.retryAfterMilliseconds)
                    } catch (e: Exception) {
                        logcat { "Failed to update presence\n${e.asLog()}" }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable {
                    showStatusEditDialog = true
                }
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Text(
                text = selfUser.status?.text ?: stringResource(id = R.string.status_text_none),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (selfUser.status?.text == null) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            )

            Icon(
                painter = painterResource(R.drawable.ic_edit_24dp),
                contentDescription = null,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    SheetButton(
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_settings_24dp),
                contentDescription = null
            )
        },
        headlineContent = {
            Text(
                text = stringResource(id = R.string.settings)
            )
        },
        onClick = {
            onBeforeNavigation()
            onGoSettings()
        }
    )


}
