package chat.revolt.screens.settings.channel

import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.activities.RevoltTweenFloat
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.api.routes.channel.patchChannel
import chat.revolt.api.routes.microservices.autumn.uploadToAutumn
import chat.revolt.api.schemas.Channel
import chat.revolt.composables.generic.InlineMediaPicker
import chat.revolt.composables.generic.ListHeader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.ContentType
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChannelSettingsOverviewViewModel @Inject constructor(@ApplicationContext val context: Context) :
    ViewModel() {
    var initialChannel by mutableStateOf<Channel?>(null)

    var channelName by mutableStateOf("")
    var channelDescription by mutableStateOf("")

    var iconModel by mutableStateOf<Any?>(null)
    var iconIsUploading by mutableStateOf(false)
    var iconUploadProgress by mutableFloatStateOf(0f)

    var uploadError by mutableStateOf<String?>(null)
    var updateError by mutableStateOf<String?>(null)

    var showNsfwToggleDialogue by mutableStateOf(false)

    fun populateWithChannel(channelId: String) {
        val channel = RevoltAPI.channelCache[channelId]
        initialChannel = channel
        channel?.let {
            channelName = it.name ?: ""
            channelDescription = it.description ?: ""
            iconModel = it.icon?.let { icon -> "$REVOLT_FILES/icons/${icon.id}" }
        }
    }

    private fun unsetIcon() {
        iconIsUploading = true
        iconUploadProgress = 0f
        uploadError = null

        initialChannel?.id?.let { channelId ->
            viewModelScope.launch {
                try {
                    patchChannel(channelId, remove = listOf("Icon"))
                    iconModel = null
                } catch (e: Exception) {
                    updateError = e.message
                }
                iconIsUploading = false
            }
        } ?: run {
            iconIsUploading = false
        }
    }

    fun pickIcon(newModel: Any?) {
        iconModel = newModel
        uploadError = null
        iconUploadProgress = 0f

        val uri = when (newModel) {
            is Uri -> newModel
            is String -> Uri.parse(newModel)
            else -> null
        } ?: run {
            unsetIcon()
            return
        }

        val mFile = File(context.cacheDir, uri.lastPathSegment ?: "icon")

        mFile.outputStream().use { output ->
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.copyTo(output)
            }
        }

        val mime = context.contentResolver.getType(uri)

        if (mime?.endsWith("webp") == true) {
            uploadError = "WebP is not supported"
            return
        }

        viewModelScope.launch {
            iconIsUploading = true
            try {
                val id = uploadToAutumn(
                    mFile,
                    uri.lastPathSegment ?: "icon",
                    "icons",
                    ContentType.parse(mime ?: "image/*"),
                    onProgress = { soFar, outOf ->
                        iconUploadProgress = soFar.toFloat() / outOf.toFloat()
                    }
                )

                patchChannel(initialChannel?.id ?: "", icon = id)

                iconIsUploading = false
            } catch (e: Exception) {
                uploadError = e.message
                iconUploadProgress = 0f
                iconIsUploading = false
                return@launch
            }
        }
    }

    fun updateChannel() {
        updateError = null
        viewModelScope.launch {
            try {
                patchChannel(
                    initialChannel?.id ?: "",
                    name = if (channelName != initialChannel?.name) channelName else null,
                    description = if (channelDescription != initialChannel?.description) channelDescription else null
                )
                initialChannel = initialChannel?.copy(
                    name = channelName,
                    description = channelDescription
                )
            } catch (e: Exception) {
                updateError = e.message
            }
        }
    }

    fun toggleNsfw() {
        updateError = null
        viewModelScope.launch {
            try {
                patchChannel(
                    initialChannel?.id ?: "",
                    nsfw = !(initialChannel?.nsfw ?: false)
                )
                initialChannel = initialChannel?.copy(
                    nsfw = !(initialChannel?.nsfw ?: false)
                )
            } catch (e: Exception) {
                updateError = e.message
            }
            showNsfwToggleDialogue = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelSettingsOverview(
    navController: NavController,
    channelId: String,
    viewModel: ChannelSettingsOverviewViewModel = hiltViewModel()
) {
    val currentChannel = RevoltAPI.channelCache[channelId]
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(channelId) {
        viewModel.populateWithChannel(channelId)
    }

    val channelInfoUpdated by remember(
        currentChannel,
        viewModel.channelName,
        viewModel.channelDescription
    ) {
        derivedStateOf {
            currentChannel?.let { channel ->
                (channel.name ?: "") != viewModel.channelName ||
                        (channel.description ?: "") != viewModel.channelDescription
            } ?: false
        }
    }

    if (viewModel.showNsfwToggleDialogue) {
        AlertDialog(
            onDismissRequest = {
                viewModel.showNsfwToggleDialogue = false
            },
            title = {
                if (currentChannel?.nsfw == true) {
                    Text(stringResource(R.string.channel_settings_overview_nsfw_undo_confirm_title))
                } else {
                    Text(stringResource(R.string.channel_settings_overview_nsfw_confirm_title))
                }
            },
            text = {
                if (currentChannel?.nsfw == true) {
                    Text(stringResource(R.string.channel_settings_overview_nsfw_undo_confirm_description))
                } else {
                    Text(stringResource(R.string.channel_settings_overview_nsfw_confirm_description))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleNsfw()
                    }
                ) {
                    if (currentChannel?.nsfw == true) {
                        Text(stringResource(R.string.channel_settings_overview_nsfw_undo_confirm_yes))
                    } else {
                        Text(stringResource(R.string.channel_settings_overview_nsfw_confirm_yes))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.showNsfwToggleDialogue = false
                    }
                ) {
                    if (currentChannel?.nsfw == true) {
                        Text(stringResource(R.string.channel_settings_overview_nsfw_undo_confirm_no))
                    } else {
                        Text(stringResource(R.string.channel_settings_overview_nsfw_confirm_no))
                    }
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.channel_settings_overview),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.icn_arrow_back_24dp),
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = channelInfoUpdated,
                enter = scaleIn(animationSpec = RevoltTweenFloat),
                exit = scaleOut(animationSpec = RevoltTweenFloat)
            ) {
                FloatingActionButton(onClick = { viewModel.updateChannel() }) {
                    Icon(
                        painter = painterResource(R.drawable.icn_check_24dp),
                        contentDescription = stringResource(R.string.channel_settings_overview_save)
                    )
                }
            }
        }
    ) { pv ->
        Box(
            Modifier
                .padding(pv)
                .imePadding()
        ) {
            currentChannel?.let {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    ListHeader {
                        Text(stringResource(R.string.channel_settings_overview_info))
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        InlineMediaPicker(
                            currentModel = viewModel.iconModel,
                            onPick = { viewModel.pickIcon(it) },
                            circular = true,
                            mimeType = "image/*",
                            canRemove = true,
                            enabled = !viewModel.iconIsUploading,
                            onRemove = { viewModel.pickIcon(null) },
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                        )
                    }

                    AnimatedVisibility(visible = viewModel.iconIsUploading) {
                        LinearProgressIndicator(
                            progress = { viewModel.iconUploadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 16.dp)
                        )
                    }

                    AnimatedVisibility(visible = viewModel.uploadError != null) {
                        Text(
                            viewModel.uploadError
                                ?: stringResource(R.string.channel_settings_overview_update_info_error_fallback),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 16.dp)
                        )
                    }

                    AnimatedVisibility(visible = viewModel.updateError != null) {
                        Text(
                            viewModel.updateError
                                ?: stringResource(R.string.channel_settings_overview_update_info_error_fallback),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 16.dp)
                        )
                    }

                    TextField(
                        label = {
                            Text(stringResource(R.string.channel_settings_overview_name))
                        },
                        value = viewModel.channelName,
                        onValueChange = { viewModel.channelName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        singleLine = true
                    )

                    TextField(
                        label = {
                            Text(stringResource(R.string.channel_settings_overview_description))
                        },
                        placeholder = {
                            Text(stringResource(R.string.channel_settings_overview_description_hint))
                        },
                        value = viewModel.channelDescription,
                        onValueChange = { viewModel.channelDescription = it },
                        modifier = Modifier
                            .animateContentSize()
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        singleLine = false,
                        minLines = 3
                    )

                    ListHeader {
                        Text(stringResource(R.string.channel_settings_overview_nsfw))
                    }

                    Text(
                        stringResource(R.string.channel_settings_overview_nsfw_description),
                        modifier = Modifier
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                    )

                    OutlinedButton(
                        content = {
                            if (currentChannel.nsfw == true) {
                                Text(stringResource(R.string.channel_settings_overview_nsfw_undo_confirm_yes))
                            } else {
                                Text(stringResource(R.string.channel_settings_overview_nsfw_confirm_yes))
                            }
                        },
                        onClick = {
                            viewModel.showNsfwToggleDialogue = true
                        },
                        modifier = Modifier
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                    )
                }
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}
