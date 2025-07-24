package chat.revolt.screens.chat.dialogs.safety

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.routes.safety.putMessageReport
import chat.revolt.api.routes.user.blockUser
import chat.revolt.api.schemas.ContentReportReason
import chat.revolt.composables.chat.Message
import chat.revolt.composables.generic.FormTextField
import kotlinx.coroutines.launch

enum class MessageReportFlowState {
    Reason,
    Sending,
    Done,
    Error
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportMessageDialog(onDismiss: () -> Unit, messageId: String) {
    val message = RevoltAPI.messageCache[messageId]
    if (message == null) {
        onDismiss()
        return
    }

    val author = RevoltAPI.userCache[message.author]
    val messageIsBridged = author?.let { author.bot != null && message.masquerade != null } ?: false

    val state = remember { mutableStateOf(MessageReportFlowState.Reason) }

    val selectedReason = remember { mutableStateOf("Illegal") }
    val userAddedContext = remember { mutableStateOf("") }

    when (state.value) {
        MessageReportFlowState.Reason -> {
            val reasons = mapOf(
                "Illegal" to stringResource(id = R.string.report_reason_content_illegal),
                "IllegalGoods" to stringResource(id = R.string.report_reason_content_illegal_goods),
                "IllegalExtortion" to stringResource(id = R.string.report_reason_content_illegal_extortion),
                "IllegalPornography" to stringResource(id = R.string.report_reason_content_illegal_pornography),
                "IllegalHacking" to stringResource(id = R.string.report_reason_content_illegal_hacking),
                "ExtremeViolence" to stringResource(id = R.string.report_reason_content_extreme_violence),
                "PromotesHarm" to stringResource(id = R.string.report_reason_content_promotes_harm),
                "UnsolicitedSpam" to stringResource(id = R.string.report_reason_content_unsolicited_spam),
                "Raid" to stringResource(id = R.string.report_reason_content_raid),
                "SpamAbuse" to stringResource(id = R.string.report_reason_content_spam_abuse),
                "ScamsFraud" to stringResource(id = R.string.report_reason_content_scams_fraud),
                "Malware" to stringResource(id = R.string.report_reason_content_malware),
                "Harassment" to stringResource(id = R.string.report_reason_content_harassment),
                "NoneSpecified" to stringResource(id = R.string.report_reason_content_other)
            )
            val reasonDropdownExpanded = remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = {
                    // nothing - prevent mistaps from closing the dialog
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.report_message_heading),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column {
                        Text(text = stringResource(id = R.string.report_message))
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(id = R.string.report_message_preview),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .verticalScroll(rememberScrollState())
                                .heightIn(max = 200.dp)
                                .padding(bottom = 8.dp)
                        ) {
                            Message(
                                message = message.copy(
                                    tail = false,
                                    masquerade = null
                                )
                            )
                        }

                        if (messageIsBridged) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = R.string.report_message_bridge_notice),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        ExposedDropdownMenuBox(
                            expanded = reasonDropdownExpanded.value,
                            onExpandedChange = {
                                reasonDropdownExpanded.value = it
                            },
                        ) {
                            TextField(
                                value = reasons[selectedReason.value]
                                    ?: stringResource(id = R.string.unknown),
                                readOnly = true,
                                onValueChange = {},
                                label = {
                                    Text(
                                        text = stringResource(id = R.string.report_reason)
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonDropdownExpanded.value)
                                },
                                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                                modifier = Modifier.menuAnchor()
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
                            label = stringResource(id = R.string.report_reason_additional),
                            onChange = {
                                userAddedContext.value = it
                            },
                            supportingText = {
                                Text(
                                    text = stringResource(
                                        id = R.string.report_reason_additional_hint
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
                        Text(text = stringResource(id = R.string.report_cancel))
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            state.value = MessageReportFlowState.Sending
                        },
                        modifier = Modifier.testTag("report_send")
                    ) {
                        Text(text = stringResource(id = R.string.report_submit))
                    }
                }
            )
        }

        MessageReportFlowState.Sending -> {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(
                        text = stringResource(id = R.string.report_submitting),
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
                                    Log.d("ReportMessageDialog", "Reporting message $messageId")
                                    putMessageReport(
                                        messageId,
                                        ContentReportReason.valueOf(selectedReason.value),
                                        userAddedContext.value
                                    )
                                    state.value = MessageReportFlowState.Done
                                } catch (e: Error) {
                                    state.value = MessageReportFlowState.Error
                                    Log.e("ReportMessageDialog", "Failed to report message", e)
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

        MessageReportFlowState.Done -> {
            val scope = rememberCoroutineScope()

            AlertDialog(
                onDismissRequest = {
                    onDismiss()
                },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.icn_check_24dp),
                        contentDescription = null, // decorative
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.report_submit_success),
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
                                text = stringResource(id = R.string.report_submit_thanks),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = stringResource(id = R.string.report_block_question),
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
                        Text(text = stringResource(id = R.string.report_block_no))
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                blockUser(message.author ?: return@launch)
                            }
                            onDismiss()
                        },
                        modifier = Modifier.testTag("report_block_yes")
                    ) {
                        Text(text = stringResource(id = R.string.report_block_yes))
                    }
                }
            )
        }

        MessageReportFlowState.Error -> {
            AlertDialog(
                onDismissRequest = {
                    onDismiss()
                },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.icn_error_24dp),
                        contentDescription = null, // decorative
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.report_submit_error_header),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column {
                        Text(
                            text = stringResource(id = R.string.report_submit_error),
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
                        Text(text = stringResource(id = R.string.ok))
                    }
                },
                confirmButton = {}
            )
        }
    }
}
