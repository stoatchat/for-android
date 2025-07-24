package chat.revolt.sheets

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import chat.revolt.R
import chat.revolt.activities.InviteActivity
import chat.revolt.api.REVOLT_APP
import chat.revolt.api.routes.server.createServer
import chat.revolt.callbacks.Action
import chat.revolt.callbacks.ActionChannel
import chat.revolt.composables.sheets.SheetSelection
import chat.revolt.composables.vectorassets.CreateServer
import chat.revolt.composables.vectorassets.JoinServer
import chat.revolt.composables.vectorassets.NewServer
import chat.revolt.material.EasingTokens
import chat.revolt.screens.chat.ChatRouterDestination
import chat.revolt.ui.theme.FragmentMono
import kotlinx.coroutines.launch
import logcat.asLog
import logcat.logcat

enum class AddServerSheetStep(val animationValue: Int) {
    Initial(0),
    JoinFromInvite(1),
    CreateServer(1)
}

@Composable
fun AddServerSheet(onDismiss: () -> Unit) {
    var currentStep by remember { mutableStateOf(AddServerSheetStep.Initial) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        AnimatedContent(
            currentStep,
            transitionSpec = {
                if (targetState.animationValue > initialState.animationValue) {
                    (slideInHorizontally(
                        animationSpec = tween(300, 0, EasingTokens.EmphasizedDecelerate),
                        initialOffsetX = { fullWidth -> fullWidth }
                    ) + fadeIn(
                        animationSpec = tween(300, 0, EasingTokens.EmphasizedDecelerate)
                    )) togetherWith (slideOutHorizontally(
                        animationSpec = tween(300, 0, EasingTokens.EmphasizedDecelerate),
                        targetOffsetX = { fullWidth -> -fullWidth }
                    ) + fadeOut(
                        animationSpec = tween(300, 0, EasingTokens.EmphasizedDecelerate)
                    ))
                } else {
                    (slideInHorizontally(
                        animationSpec = tween(300, 0, EasingTokens.EmphasizedDecelerate),
                        initialOffsetX = { fullWidth -> -fullWidth }
                    ) + fadeIn(
                        animationSpec = tween(300, 0, EasingTokens.EmphasizedDecelerate)
                    )) togetherWith (slideOutHorizontally(
                        animationSpec = tween(300, 0, EasingTokens.EmphasizedDecelerate),
                        targetOffsetX = { fullWidth -> fullWidth }
                    ) + fadeOut(
                        animationSpec = tween(300, 0, EasingTokens.EmphasizedDecelerate)
                    ))
                }
            },
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.animateContentSize(
                animationSpec = tween(150, 0, EasingTokens.EmphasizedDecelerate)
            )
        ) { step ->
            Box(
                modifier = Modifier.padding(16.dp)
            ) {
                if (step == AddServerSheetStep.Initial) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            imageVector = NewServer,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                        )

                        Text(
                            text = stringResource(id = R.string.add_server_sheet_step_0_title),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = stringResource(id = R.string.add_server_sheet_step_0_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        SheetSelection(
                            icon = {
                                Image(
                                    imageVector = JoinServer,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                )
                            },
                            title = {
                                Text(
                                    text = stringResource(id = R.string.add_server_sheet_step_0_join)
                                )
                            },
                            description = {
                                Text(
                                    text = stringResource(id = R.string.add_server_sheet_step_0_join_description)
                                )
                            },
                        ) {
                            currentStep = AddServerSheetStep.JoinFromInvite
                        }

                        SheetSelection(
                            icon = {
                                Image(
                                    imageVector = CreateServer,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                )
                            },
                            title = {
                                Text(
                                    text = stringResource(id = R.string.add_server_sheet_step_0_create)
                                )
                            },
                            description = {
                                Text(
                                    text = stringResource(id = R.string.add_server_sheet_step_0_create_description)
                                )
                            },
                        ) {
                            currentStep = AddServerSheetStep.CreateServer
                        }
                    }
                } else if (step == AddServerSheetStep.JoinFromInvite) {
                    val inviteState = rememberTextFieldState()

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(id = R.string.add_server_sheet_step_1j_title),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            IconButton(
                                onClick = {
                                    currentStep = AddServerSheetStep.Initial
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.icn_arrow_back_24dp),
                                    contentDescription = stringResource(id = R.string.back),
                                )
                            }
                        }

                        Text(
                            text = stringResource(id = R.string.add_server_sheet_step_1j_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )


                        Text(
                            text = stringResource(id = R.string.add_server_sheet_step_1j_examples_heading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = R.string.add_server_sheet_step_1j_example_1),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                fontFamily = FragmentMono,
                                modifier = Modifier
                                    .clip(
                                        MaterialTheme.shapes.small.copy(
                                            topStart = MaterialTheme.shapes.large.topStart,
                                            topEnd = MaterialTheme.shapes.large.topEnd,
                                        )
                                    )
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            )
                            Text(
                                text = stringResource(id = R.string.add_server_sheet_step_1j_example_2),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                fontFamily = FragmentMono,
                                modifier = Modifier
                                    .clip(
                                        MaterialTheme.shapes.small
                                    )
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            )
                            Text(
                                text = stringResource(id = R.string.add_server_sheet_step_1j_example_3),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                fontFamily = FragmentMono,
                                modifier = Modifier
                                    .clip(
                                        MaterialTheme.shapes.small.copy(
                                            bottomStart = MaterialTheme.shapes.large.bottomStart,
                                            bottomEnd = MaterialTheme.shapes.large.bottomEnd,
                                        )
                                    )
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            state = inviteState,
                            label = {
                                Text(stringResource(R.string.add_server_sheet_step_1j_label))
                            },
                            lineLimits = TextFieldLineLimits.SingleLine,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 8.dp)
                        )

                        Button(
                            onClick = {
                                val intent = Intent(context, InviteActivity::class.java)
                                intent.data = if (inviteState.text.startsWith("https://")) {
                                    try {
                                        inviteState.text.toString().toUri()
                                    } catch (e: Exception) {
                                        Log.e(
                                            "AddServerSheet",
                                            "Invalid URL: ${inviteState.text}",
                                            e
                                        )
                                        return@Button
                                    }
                                } else {
                                    "https://$REVOLT_APP/invite/${inviteState.text}".toUri()
                                }
                                context.startActivity(intent)

                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(id = R.string.add_server_sheet_step_1j_join)
                            )
                        }
                    }
                } else if (step == AddServerSheetStep.CreateServer) {
                    val serverNameState = rememberTextFieldState()
                    val serverNameIsBlank = remember(serverNameState.text) {
                        serverNameState.text.isBlank()
                    }

                    var serverNameRangeError by remember { mutableStateOf(false) }
                    var serverCreationError by remember { mutableStateOf(false) }

                    LaunchedEffect(serverNameState.text) {
                        if (serverNameState.text.length > 32) {
                            serverNameRangeError = true
                        } else {
                            serverNameRangeError = false
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(id = R.string.add_server_sheet_step_1c_title),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            IconButton(
                                onClick = {
                                    currentStep = AddServerSheetStep.Initial
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.icn_arrow_back_24dp),
                                    contentDescription = stringResource(id = R.string.back),
                                )
                            }
                        }

                        Text(
                            text = stringResource(id = R.string.add_server_sheet_step_1c_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        val outline = MaterialTheme.colorScheme.outline
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Canvas(Modifier.size(80.dp)) {
                                val stroke = Stroke(
                                    width = 4.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(30f, 40f),
                                        0f
                                    )
                                )

                                drawCircle(
                                    color = outline,
                                    style = stroke,
                                    radius = size.minDimension / 2 - stroke.width / 2
                                )
                            }
                            Text(
                                text = when {
                                    serverNameIsBlank -> stringResource(R.string.add_server_sheet_step_1c_name_placeholder)
                                    else -> serverNameState.text.toString()
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = LocalContentColor.current.copy(
                                    alpha = when {
                                        serverNameIsBlank -> 0.5f
                                        else -> 1f
                                    }
                                ),
                                fontWeight = when {
                                    serverNameIsBlank -> FontWeight.Medium
                                    else -> FontWeight.Bold
                                },
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        TextField(
                            state = serverNameState,
                            label = {
                                Text(stringResource(R.string.add_server_sheet_step_1c_name))
                            },
                            lineLimits = TextFieldLineLimits.SingleLine,
                            supportingText = {
                                Text(
                                    when {
                                        serverCreationError -> stringResource(R.string.add_server_sheet_step_1c_error)
                                        serverNameRangeError -> stringResource(
                                            R.string.add_server_sheet_step_1c_name_error_range,
                                            32
                                        )

                                        else -> ""
                                    }
                                )
                            },
                            isError = serverNameRangeError || serverCreationError,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 8.dp)
                        )

                        Button(
                            onClick = {
                                serverCreationError = false

                                scope.launch {
                                    try {
                                        val server = createServer(serverNameState.text.toString())

                                        // Backend should've already created a channel for us to go to
                                        server.channels?.first()?.id?.let {
                                            ActionChannel.send(
                                                Action.ChatNavigate(
                                                    ChatRouterDestination.Channel(it)
                                                )
                                            )
                                        }

                                        onDismiss()
                                    } catch (e: Exception) {
                                        serverCreationError = true
                                        logcat { "Error creating server: ${e.asLog()}" }
                                    }
                                }
                            },
                            enabled = !serverNameIsBlank && !serverNameRangeError,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(id = R.string.add_server_sheet_step_1c_create)
                            )
                        }
                    }
                }
            }
        }
    }
}
