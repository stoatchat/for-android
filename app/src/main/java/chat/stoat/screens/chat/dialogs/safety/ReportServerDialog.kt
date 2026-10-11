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
import chat.stoat.api.routes.safety.putServerReport
import chat.stoat.core.model.schemas.ContentReportReason
import chat.stoat.composables.generic.FormTextField
import chat.stoat.composables.screens.settings.ServerOverview
import chat.stoat.shared.resources.Res
import chat.stoat.shared.resources.ok
import chat.stoat.shared.resources.report_cancel
import chat.stoat.shared.resources.report_reason
import chat.stoat.shared.resources.report_reason_additional
import chat.stoat.shared.resources.report_reason_additional_hint
import chat.stoat.shared.resources.report_reason_content_extreme_violence
import chat.stoat.shared.resources.report_reason_content_harassment
import chat.stoat.shared.resources.report_reason_content_illegal
import chat.stoat.shared.resources.report_reason_content_illegal_extortion
import chat.stoat.shared.resources.report_reason_content_illegal_goods
import chat.stoat.shared.resources.report_reason_content_illegal_hacking
import chat.stoat.shared.resources.report_reason_content_illegal_pornography
import chat.stoat.shared.resources.report_reason_content_malware
import chat.stoat.shared.resources.report_reason_content_other
import chat.stoat.shared.resources.report_reason_content_promotes_harm
import chat.stoat.shared.resources.report_reason_content_raid
import chat.stoat.shared.resources.report_reason_content_scams_fraud
import chat.stoat.shared.resources.report_reason_content_spam_abuse
import chat.stoat.shared.resources.report_reason_content_unsolicited_spam
import chat.stoat.shared.resources.report_server
import chat.stoat.shared.resources.report_server_heading
import chat.stoat.shared.resources.report_server_preview
import chat.stoat.shared.resources.report_submit
import chat.stoat.shared.resources.report_submit_close
import chat.stoat.shared.resources.report_submit_error
import chat.stoat.shared.resources.report_submit_error_header
import chat.stoat.shared.resources.report_submit_success
import chat.stoat.shared.resources.report_submit_thanks
import chat.stoat.shared.resources.report_submitting
import chat.stoat.shared.resources.unknown
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

enum class ServerReportFlowState {
    Reason,
    Sending,
    Done,
    Error
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportServerDialog(onDismiss: () -> Unit, serverId: String) {
    val server = StoatAPI.serverCache[serverId]
    if (server == null) {
        onDismiss()
        return
    }

    val state = remember { mutableStateOf(ServerReportFlowState.Reason) }

    val selectedReason = remember { mutableStateOf("Illegal") }
    val userAddedContext = remember { mutableStateOf("") }

    when (state.value) {
        ServerReportFlowState.Reason -> {
            val reasons = mapOf(
                "Illegal" to stringResource(Res.string.report_reason_content_illegal),
                "IllegalGoods" to stringResource(Res.string.report_reason_content_illegal_goods),
                "IllegalExtortion" to stringResource(Res.string.report_reason_content_illegal_extortion),
                "IllegalPornography" to stringResource(Res.string.report_reason_content_illegal_pornography),
                "IllegalHacking" to stringResource(Res.string.report_reason_content_illegal_hacking),
                "ExtremeViolence" to stringResource(Res.string.report_reason_content_extreme_violence),
                "PromotesHarm" to stringResource(Res.string.report_reason_content_promotes_harm),
                "UnsolicitedSpam" to stringResource(Res.string.report_reason_content_unsolicited_spam),
                "Raid" to stringResource(Res.string.report_reason_content_raid),
                "SpamAbuse" to stringResource(Res.string.report_reason_content_spam_abuse),
                "ScamsFraud" to stringResource(Res.string.report_reason_content_scams_fraud),
                "Malware" to stringResource(Res.string.report_reason_content_malware),
                "Harassment" to stringResource(Res.string.report_reason_content_harassment),
                "NoneSpecified" to stringResource(Res.string.report_reason_content_other)
            )
            val reasonDropdownExpanded = remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = {
                    // nothing - prevent mistaps from closing the dialog
                },
                title = {
                    Text(
                        text = stringResource(Res.string.report_server_heading),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column {
                        Text(text = stringResource(Res.string.report_server))
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(Res.string.report_server_preview),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        ServerOverview(server)

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
                            state.value = ServerReportFlowState.Sending
                        },
                        modifier = Modifier.testTag("report_send")
                    ) {
                        Text(text = stringResource(Res.string.report_submit))
                    }
                }
            )
        }

        ServerReportFlowState.Sending -> {
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
                                    Log.d("ReportServerDialog", "Reporting server $serverId")
                                    putServerReport(
                                        serverId,
                                        ContentReportReason.valueOf(selectedReason.value),
                                        userAddedContext.value
                                    )
                                    state.value = ServerReportFlowState.Done
                                } catch (e: Error) {
                                    state.value = ServerReportFlowState.Error
                                    Log.e("ReportServerDialog", "Failed to report server", e)
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

        ServerReportFlowState.Done -> {
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
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDismiss()
                        },
                        modifier = Modifier.testTag("report_close")
                    ) {
                        Text(text = stringResource(Res.string.report_submit_close))
                    }
                }
            )
        }

        ServerReportFlowState.Error -> {
            AlertDialog(
                onDismissRequest = {
                    onDismiss()
                },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_24dp),
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
