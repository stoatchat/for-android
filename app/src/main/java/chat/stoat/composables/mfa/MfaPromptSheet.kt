package chat.stoat.composables.mfa

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R
import chat.stoat.composables.generic.NonIdealState
import chat.stoat.ui.theme.FragmentMono

@Composable
fun mfaErrorText(errorType: String?): String = when (errorType) {
    null -> ""
    "InvalidCredentials" -> stringResource(R.string.mfa_error_invalid_credentials)
    "InvalidToken" -> stringResource(R.string.mfa_error_invalid_token)
    "DisallowedMfaMethod" -> stringResource(R.string.mfa_error_disallowed_method)
    else -> stringResource(R.string.mfa_error_generic, errorType)
}

private enum class MfaPromptStage { Loading, LoadError, NoMethods, Form }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MfaPromptSheet(state: MfaPromptState) {
    val session = state.session ?: return

    ModalBottomSheet(
        onDismissRequest = { session.cancel() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetGesturesEnabled = !session.busy
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .imePadding()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.shapes.large
                    )
                    .size(96.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_shield_lock_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.mfa_prompt_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.mfa_prompt_lead),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            val stage = when {
                session.loadError != null -> MfaPromptStage.LoadError
                session.methods == null -> MfaPromptStage.Loading
                session.methods?.isEmpty() == true -> MfaPromptStage.NoMethods
                else -> MfaPromptStage.Form
            }

            AnimatedContent(
                targetState = stage,
                modifier = Modifier.fillMaxWidth()
            ) { currentStage ->
                when (currentStage) {
                    MfaPromptStage.Loading -> Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        LoadingIndicator(modifier = Modifier.size(56.dp))
                    }

                    MfaPromptStage.LoadError -> NonIdealState(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_error_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(it)
                            )
                        },
                        title = { Text(stringResource(R.string.mfa_prompt_load_error_title)) },
                        description = {
                            Text(stringResource(R.string.mfa_prompt_load_error_description))
                        },
                        actions = {
                            TextButton(onClick = { session.cancel() }) {
                                Text(stringResource(R.string.cancel))
                            }
                            Button(onClick = { session.retryLoad() }) {
                                Text(stringResource(R.string.mfa_prompt_retry))
                            }
                        }
                    )

                    MfaPromptStage.NoMethods -> NonIdealState(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_lock_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(it)
                            )
                        },
                        title = { Text(stringResource(R.string.mfa_prompt_no_methods_title)) },
                        description = {
                            Text(stringResource(R.string.mfa_prompt_no_methods_description))
                        },
                        actions = {
                            Button(onClick = { session.cancel() }) {
                                Text(stringResource(R.string.mfa_prompt_close))
                            }
                        }
                    )

                    MfaPromptStage.Form -> MfaPromptForm(session)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MfaPromptForm(session: MfaPromptSession) {
    val methods = session.methods.orEmpty()
    val selected = session.selected ?: return

    Column(modifier = Modifier.fillMaxWidth()) {
        if (methods.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                methods.forEach { method ->
                    FilterChip(
                        selected = method == selected,
                        onClick = { session.selectMethod(method) },
                        label = { Text(stringResource(method.label)) },
                        enabled = !session.busy
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        AnimatedContent(targetState = selected) { method ->
            Column {
                Text(
                    text = stringResource(method.lead),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                MfaMethodInput(session, method)
            }
        }

        AnimatedContent(targetState = session.error) { error ->
            if (error != null) {
                Text(
                    text = mfaErrorText(error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(
                onClick = { session.cancel() },
                enabled = !session.busy
            ) {
                Text(stringResource(R.string.cancel))
            }

            Button(
                onClick = { session.submit() },
                enabled = !session.busy &&
                        selected.isSubmittable(session.input.text.toString())
            ) {
                AnimatedContent(targetState = session.busy) { busy ->
                    if (busy) {
                        LoadingIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text(stringResource(R.string.mfa_prompt_confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun MfaMethodInput(session: MfaPromptSession, method: MfaMethod) {
    val inputModifier = Modifier
        .fillMaxWidth()
        .semantics { method.autofillContentType?.let { contentType = it } }

    if (method.secureInput) {
        SecureTextField(
            state = session.input,
            label = { Text(stringResource(method.inputLabel)) },
            enabled = !session.busy,
            onKeyboardAction = { session.submit() },
            modifier = inputModifier
        )
    } else {
        val transformation = remember(method) {
            InputTransformation {
                val current = asCharSequence().toString()
                val sanitized = method.sanitizeInput(current)
                if (sanitized != current) replace(0, length, sanitized)
            }
        }

        TextField(
            state = session.input,
            label = { Text(stringResource(method.inputLabel)) },
            inputTransformation = transformation,
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(
                keyboardType = method.keyboardType,
                imeAction = ImeAction.Done
            ),
            onKeyboardAction = { session.submit() },
            textStyle = if (method.monospaceInput) {
                LocalTextStyle.current.copy(
                    fontFamily = FragmentMono,
                    letterSpacing = 2.sp
                )
            } else {
                LocalTextStyle.current
            },
            enabled = !session.busy,
            modifier = inputModifier
        )
    }

    if (method.autoSubmit) {
        LaunchedEffect(session, method) {
            snapshotFlow { session.input.text.toString() }
                .collect { text ->
                    if (!session.busy && method.isSubmittable(text)) session.submit()
                }
        }
    }
}
