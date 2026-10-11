package chat.stoat.screens.chat.dialogs.safety

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.routes.safety.putUserReport
import chat.stoat.api.routes.user.blockUser
import chat.stoat.core.model.schemas.UserReportReason
import chat.stoat.composables.generic.FormTextField
import chat.stoat.composables.screens.settings.UserOverview
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.ok
import chat.stoat.shared.resources.report_block_no
import chat.stoat.shared.resources.report_block_question
import chat.stoat.shared.resources.report_block_yes
import chat.stoat.shared.resources.report_cancel
import chat.stoat.shared.resources.report_reason
import chat.stoat.shared.resources.report_reason_additional
import chat.stoat.shared.resources.report_reason_additional_hint
import chat.stoat.shared.resources.report_reason_user_ban_evasion
import chat.stoat.shared.resources.report_reason_user_impersonation
import chat.stoat.shared.resources.report_reason_user_inappropriate_content
import chat.stoat.shared.resources.report_reason_user_other
import chat.stoat.shared.resources.report_reason_user_spam_abuse
import chat.stoat.shared.resources.report_reason_user_underage
import chat.stoat.shared.resources.report_reason_user_unsolicited_spam
import chat.stoat.shared.resources.report_submit
import chat.stoat.shared.resources.report_submit_error
import chat.stoat.shared.resources.report_submit_error_header
import chat.stoat.shared.resources.report_submit_success
import chat.stoat.shared.resources.report_submit_thanks
import chat.stoat.shared.resources.report_submitting
import chat.stoat.shared.resources.report_user
import chat.stoat.shared.resources.report_user_heading
import chat.stoat.shared.resources.report_user_preview
import chat.stoat.shared.resources.unknown
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

enum class UserReportFlowState {
    Reason,
    Sending,
    Done,
    Error
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportUserDialog(onDismiss: () -> Unit, userId: String) {
    val user = StoatAPI.userCache[userId]
    if (user == null) {
        onDismiss()
        return
    }

    val state = remember { mutableStateOf(UserReportFlowState.Reason) }

    val selectedReason = remember { mutableStateOf("UnsolicitedSpam") }
    val userAddedContext = remember { mutableStateOf("") }

    when (state.value) {
        UserReportFlowState.Reason -> {
            val reasons = mapOf(
                "UnsolicitedSpam" to stringResource(Res.string.report_reason_user_unsolicited_spam),
                "SpamAbuse" to stringResource(Res.string.report_reason_user_spam_abuse),
                "InappropriateProfile" to stringResource(Res.string.report_reason_user_inappropriate_content),
                "Impersonation" to stringResource(Res.string.report_reason_user_impersonation),
                "BanEvasion" to stringResource(Res.string.report_reason_user_ban_evasion),
                "Underage" to stringResource(Res.string.report_reason_user_underage),
                "NoneSpecified" to stringResource(Res.string.report_reason_user_other)
            )
            val reasonDropdownExpanded = remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = {
                    // nothing - prevent mistaps from closing the dialog
                },
                title = {
                    Text(
                        text = stringResource(Res.string.report_user_heading),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column {
                        Text(text = stringResource(Res.string.report_user))
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(Res.string.report_user_preview),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        UserOverview(user, internalPadding = false)

                        Spacer(modifier = Modifier.height(16.dp))

                        ExposedDropdownMenuBox(
                            expanded = reasonDropdownExpanded.value,
                            onExpandedChange = {
                                reasonDropdownExpanded.value = it
                            },
                        ) {
                            TextField(
                                value = reasons[selectedReason.value]
                                    ?: stringResource(Res.string.unknown),
                                readOnly = true,
                                onValueChange = {},
                                label = {
                                    Text(
                                        text = stringResource(Res.string.report_reason)
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonDropdownExpanded.value)
                                },
                                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            )

                            ExposedDropdownMenu(
                                expanded = reasonDropdownExpanded.value,
                                onDismissRequest = {
                                    reasonDropdownExpanded.value = false
                                }
                            ) {
                                reasons.forEach { (key, value) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = value)
                                        },
                                        onClick = {
                                            selectedReason.value = key
                                            reasonDropdownExpanded.value = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        FormTextField(
                            value = userAddedContext.value,
                            label = stringResource(Res.string.report_reason_additional),
                            onChange = {
                                userAddedContext.value = it
                            },
                            supportingText = {
                                Text(
                                    text = stringResource(
                                        Res.string.report_reason_additional_hint
                                    )
                                )
                            }
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            onDismiss()
                        },
                        modifier = Modifier.testTag("report_cancel")
                    ) {
                        Text(text = stringResource(Res.string.report_cancel))
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            state.value = UserReportFlowState.Sending
                        },
                        modifier = Modifier.testTag("report_send")
                    ) {
                        Text(text = stringResource(Res.string.report_submit))
                    }
                }
            )
        }

        UserReportFlowState.Sending -> {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(
                        text = stringResource(Res.string.report_submitting),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        LaunchedEffect(true) {
                            launch {
                                try {
                                    Log.d("ReportMessageDialog", "Reporting user $userId")
                                    putUserReport(
                                        userId,
                                        UserReportReason.valueOf(selectedReason.value),
                                        userAddedContext.value
                                    )
                                    state.value = UserReportFlowState.Done
                                } catch (e: Error) {
                                    state.value = UserReportFlowState.Error
                                    Log.e("ReportMessageDialog", "Failed to report user", e)
                                    return@launch
                                }
                            }
                        }
                    }
                },
                dismissButton = {},
                confirmButton = {}
            )
        }

        UserReportFlowState.Done -> {
            val scope = rememberCoroutineScope()

            AlertDialog(
                onDismissRequest = {
                    onDismiss()
                },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_24dp),
                        contentDescription = null, // decorative
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = stringResource(Res.string.report_submit_success),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(Res.string.report_submit_thanks),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = stringResource(Res.string.report_block_question),
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            onDismiss()
                        },
                        modifier = Modifier.testTag("report_block_no")
                    ) {
                        Text(text = stringResource(Res.string.report_block_no))
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                blockUser(userId)
                            }
                            onDismiss()
                        },
                        modifier = Modifier.testTag("report_block_yes")
                    ) {
                        Text(text = stringResource(Res.string.report_block_yes))
                    }
                }
            )
        }

        UserReportFlowState.Error -> {
            AlertDialog(
                onDismissRequest = {
                    onDismiss()
                },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_error_24dp),
                        contentDescription = null, // decorative
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = stringResource(Res.string.report_submit_error_header),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column {
                        Text(
                            text = stringResource(Res.string.report_submit_error),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            onDismiss()
                        },
                        modifier = Modifier.testTag("report_error_ok")
                    ) {
                        Text(text = stringResource(Res.string.ok))
                    }
                },
                confirmButton = {}
            )
        }
    }
}
