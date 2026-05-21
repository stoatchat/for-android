package chat.stoat.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.StoatAPIError
import chat.stoat.api.routes.invites.fetchInviteByCode
import chat.stoat.api.routes.invites.joinInviteByCode
import chat.stoat.api.settings.LoadedSettings
import chat.stoat.api.settings.SyncedSettings
import chat.stoat.callbacks.ActionChannel
import chat.stoat.composables.generic.IconPlaceholder
import chat.stoat.composables.generic.RemoteImage
import chat.stoat.core.model.data.STOAT_FILES
import chat.stoat.core.model.schemas.Invite
import chat.stoat.core.model.schemas.InviteJoined
import chat.stoat.core.model.util.RsResult
import chat.stoat.ui.theme.StoatTheme
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import kotlinx.coroutines.launch

class InviteActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inviteCode = intent.data?.lastPathSegment

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.Transparent.toArgb()

        setContent {
            InviteScreen(
                inviteCode = inviteCode,
                onFinish = { finish() }
            )
        }
    }
}

class InviteViewModel : ViewModel() {
    private var _loadingFinished by mutableStateOf(false)
    val loadingFinished: Boolean
        get() = _loadingFinished

    fun setLoadingFinished(loadingFinished: Boolean) {
        _loadingFinished = loadingFinished
    }

    private var _inviteResult by mutableStateOf<RsResult<Invite, StoatAPIError>?>(null)
    val inviteResult: RsResult<Invite, StoatAPIError>?
        get() = _inviteResult

    fun setInviteResult(inviteResult: RsResult<Invite, StoatAPIError>?) {
        _inviteResult = inviteResult
    }

    private var _joinResult by mutableStateOf<RsResult<InviteJoined, StoatAPIError>?>(null)
    val joinResult: RsResult<InviteJoined, StoatAPIError>?
        get() = _joinResult

    fun setJoinResult(joinResult: RsResult<InviteJoined, StoatAPIError>?) {
        _joinResult = joinResult
    }

    fun fetchInvite(inviteCode: String) {
        viewModelScope.launch {
            val result = fetchInviteByCode(inviteCode)
            setInviteResult(result)
            setLoadingFinished(true)
        }
    }

    fun joinInvite(inviteCode: String) {
        viewModelScope.launch {
            val result = joinInviteByCode(inviteCode)
            setJoinResult(result)
        }
    }

    fun navigateToServer(channelId: String) {
        viewModelScope.launch {
            ActionChannel.send(chat.stoat.callbacks.Action.SwitchChannel(channelId))
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun InviteScreen(
    inviteCode: String?,
    onFinish: () -> Unit = {},
    viewModel: InviteViewModel = viewModel()
) {
    LaunchedEffect(inviteCode) {
        if (inviteCode != null) {
            viewModel.fetchInvite(inviteCode)
        }
    }

    LaunchedEffect(viewModel.joinResult) {
        if (viewModel.joinResult?.ok == true) {
            onFinish()
        }
    }

    val inviteValid = if (viewModel.loadingFinished) (viewModel.inviteResult?.ok ?: false) else null
    val invite = viewModel.inviteResult?.value

    StoatTheme(
        requestedTheme = LoadedSettings.theme,
        requestedUserInterfaceFont = LoadedSettings.font,
        colourOverrides = SyncedSettings.android.colourOverrides
    ) {
        Surface(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
        ) {
            if (inviteCode == null) {
                NoInviteSpecifiedError(onDismissRequest = onFinish)
            } else {
                if (inviteValid == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(48.dp)
                        )
                    }
                } else if (!inviteValid || viewModel.joinResult?.err == true) {
                    InvalidInviteError(
                        error = viewModel.inviteResult?.error ?: viewModel.joinResult?.error,
                        onDismissRequest = onFinish
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        GlideImage(
                            model = "$STOAT_FILES/banners/${invite?.serverBanner?.id}/${invite?.serverBanner?.filename}",
                            contentScale = ContentScale.Crop,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Color.Black.copy(alpha = 0.5f)
                                )
                        )

                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .clip(MaterialTheme.shapes.large)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (invite?.serverIcon != null) {
                                RemoteImage(
                                    url = "$STOAT_FILES/icons/${invite.serverIcon!!.id}/${invite.serverIcon!!.filename}",
                                    allowAnimation = false,
                                    description = viewModel.inviteResult?.value?.serverName
                                        ?: stringResource(id = R.string.unknown),
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                IconPlaceholder(
                                    name = invite?.serverName
                                        ?: stringResource(id = R.string.unknown),
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = viewModel.inviteResult?.value?.serverName
                                    ?: stringResource(id = R.string.unknown),
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stringResource(
                                    id = if (StoatAPI.serverCache.containsKey(
                                            invite?.serverId
                                        )
                                    ) R.string.invite_already_member else R.string.invite_message
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row {
                                Button(
                                    onClick = {
                                        if (StoatAPI.serverCache.containsKey(invite?.serverId)) {
                                            invite?.channelId?.let {
                                                viewModel.navigateToServer(it)
                                            }
                                            onFinish()
                                        } else {
                                            viewModel.joinInvite(inviteCode)
                                        }
                                    },
                                    modifier = Modifier
                                        .testTag("accept_invite")
                                ) {
                                    Text(
                                        text = stringResource(
                                            id =
                                                if (StoatAPI.serverCache.containsKey(invite?.serverId)) R.string.invite_open_server
                                                else R.string.invite_join
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                TextButton(
                                    onClick = onFinish,
                                    modifier = Modifier
                                        .testTag("decline_invite")
                                ) {
                                    Text(text = stringResource(id = R.string.invite_cancel))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InvalidInviteError(error: StoatAPIError? = null, onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_error_24dp),
                contentDescription = null, // decorative
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(id = R.string.invite_error_header),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(
                        id = when (error?.type) {
                            "NotFound" -> R.string.invite_error_invalid_invite
                            "Banned" -> R.string.invite_error_banned
                            else -> R.string.invite_error_unknown
                        }
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(text = stringResource(id = R.string.invite_cancel))
            }
        },
        confirmButton = {}
    )
}

@Composable
fun NoInviteSpecifiedError(onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_error_24dp),
                contentDescription = null, // decorative
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(id = R.string.invite_error_header),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.invite_error_no_invite),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(text = stringResource(id = R.string.ok))
            }
        },
        confirmButton = {}
    )
}
