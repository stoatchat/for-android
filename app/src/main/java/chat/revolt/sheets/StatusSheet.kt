package chat.revolt.sheets

import android.util.Log
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.routes.user.patchSelf
import chat.revolt.api.schemas.User
import chat.revolt.composables.generic.SheetButton
import chat.revolt.composables.generic.asApiName
import chat.revolt.composables.generic.presenceFromStatus
import chat.revolt.composables.screens.settings.UserOverview
import chat.revolt.composables.settings.profile.StatusPicker
import kotlinx.coroutines.launch

@Composable
fun StatusTextEditDialog(
    selfUser: User,
    initialStatus: String,
    onDismiss: () -> Unit
) {
    val fieldState = rememberTextFieldState(initialStatus)
    var errorText by remember { mutableStateOf<String?>(null) }
    var isConfirmEnabled by remember { mutableStateOf(false) }
    val context = LocalContext.current
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
                        errorText = context.getString(R.string.status_text_error_too_long, 128)
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
                                } catch (e: Exception) {
                                    Log.e("StatusTextEditDialog", "Failed to remove status text", e)
                                    errorText = context.getString(R.string.status_text_error_other)
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
                                } catch (e: Exception) {
                                    Log.e("StatusTextEditDialog", "Failed to update status text", e)
                                    errorText = context.getString(R.string.status_text_error_other)
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
    val selfUser = RevoltAPI.userCache[RevoltAPI.selfId]!!
    val scope = rememberCoroutineScope()

    var showStatusEditDialog by remember { mutableStateOf(false) }

    if (showStatusEditDialog) {
        StatusTextEditDialog(
            selfUser = selfUser,
            initialStatus = selfUser.status?.text ?: "",
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
                onBeforeNavigation()
                scope.launch {
                    patchSelf(status = selfUser.status?.copy(presence = it.asApiName()))
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
                painter = painterResource(R.drawable.icn_edit_24dp),
                contentDescription = null,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    SheetButton(
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.icn_settings_24dp),
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
