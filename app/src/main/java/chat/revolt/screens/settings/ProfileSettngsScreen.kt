package chat.revolt.screens.settings

import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.routes.microservices.autumn.uploadToAutumn
import chat.revolt.api.routes.user.fetchUserProfile
import chat.revolt.api.routes.user.patchSelf
import chat.revolt.api.schemas.Profile
import chat.revolt.composables.generic.InlineMediaPicker
import chat.revolt.composables.screens.settings.RawUserOverview
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.ContentType
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
@Suppress("StaticFieldLeak")
class ProfileSettingsScreenViewModel @Inject constructor(@ApplicationContext val context: Context) :
    ViewModel() {
    var isLoading by mutableStateOf(true)
    var pfpModel by mutableStateOf<Any?>(null)
    var currentProfile by mutableStateOf<Profile?>(null)
    var pendingProfile by mutableStateOf<Profile?>(null)
    var backgroundModel by mutableStateOf<Any?>(null)
    var uploadProgress by mutableFloatStateOf(0f)
    var uploadError by mutableStateOf<String?>(null)
    var bioError by mutableStateOf<String?>(null)

    init {
        RevoltAPI.selfId?.let { self ->
            RevoltAPI.userCache[self]?.avatar?.id?.let {
                pfpModel = "https://autumn.revolt.chat/avatars/${it}"
            }
            viewModelScope.launch {
                currentProfile = fetchUserProfile(self)
                currentProfile!!.background?.id?.let {
                    backgroundModel = "https://autumn.revolt.chat/backgrounds/${it}"
                }

                pendingProfile = currentProfile!!.copy()

                isLoading = false
            }
        }

    }

    fun saveNewPfp() {
        uploadError = null

        val uri = when (pfpModel) {
            is Uri -> pfpModel as Uri
            is String -> Uri.parse(pfpModel as String)
            else -> return
        }

        val mFile = File(context.cacheDir, uri.lastPathSegment ?: "avatar")

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
            try {
                val id = uploadToAutumn(
                    mFile,
                    uri.lastPathSegment ?: "avatar",
                    "avatars",
                    ContentType.parse(mime ?: "image/*"),
                    onProgress = { soFar, outOf ->
                        uploadProgress = soFar.toFloat() / outOf.toFloat()
                    }
                )

                patchSelf(avatar = id)
            } catch (e: Exception) {
                uploadError = e.message
                uploadProgress = 0f
                return@launch
            }

            pfpModel = RevoltAPI.userCache[RevoltAPI.selfId]?.avatar?.id?.let {
                "https://autumn.revolt.chat/avatars/${it}"
            }

            uploadProgress = 0f
        }
    }

    fun saveNewBackground() {
        uploadError = null

        val uri = when (backgroundModel) {
            is Uri -> backgroundModel as Uri
            is String -> Uri.parse(backgroundModel as String)
            else -> return
        }

        val mFile = File(context.cacheDir, uri.lastPathSegment ?: "background")

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
            try {
                val id = uploadToAutumn(
                    mFile,
                    uri.lastPathSegment ?: "background",
                    "backgrounds",
                    ContentType.parse(mime ?: "image/*"),
                    onProgress = { soFar, outOf ->
                        uploadProgress = soFar.toFloat() / outOf.toFloat()
                    }
                )

                patchSelf(background = id)
            } catch (e: Exception) {
                uploadError = e.message
                uploadProgress = 0f
                return@launch
            }

            backgroundModel = RevoltAPI.selfId?.let {
                val profile = fetchUserProfile(it)
                currentProfile = profile
                pendingProfile = profile

                profile.background?.id?.let {
                    "https://autumn.revolt.chat/backgrounds/${it}"
                }
            }

            uploadProgress = 0f
        }
    }

    fun removePfp() {
        viewModelScope.launch {
            patchSelf(remove = listOf("Avatar"))
            pfpModel = null
        }
    }

    fun removeBackground() {
        viewModelScope.launch {
            patchSelf(remove = listOf("ProfileBackground"))
            backgroundModel = null
        }
    }

    fun saveBio() {
        bioError = null
        viewModelScope.launch {
            try {
                patchSelf(bio = pendingProfile?.content)

                fetchUserProfile(RevoltAPI.selfId!!).let {
                    currentProfile = it
                    pendingProfile = it
                }
            } catch (e: Exception) {
                bioError = e.message
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    navController: NavController,
    viewModel: ProfileSettingsScreenViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.settings_profile),
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
    ) { pv ->
        Box(
            Modifier
                .padding(pv)
                .imePadding()
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (viewModel.isLoading) {
                            Modifier
                        } else {
                            Modifier.verticalScroll(scrollState)
                        }
                    ),
                verticalArrangement = if (viewModel.isLoading) {
                    Arrangement.Center
                } else {
                    Arrangement.Top
                },
                horizontalAlignment = if (viewModel.isLoading) {
                    Alignment.CenterHorizontally
                } else {
                    Alignment.Start
                }
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                    )
                } else {
                    RevoltAPI.userCache[RevoltAPI.selfId]?.let {
                        RawUserOverview(
                            it,
                            viewModel.pendingProfile,
                            viewModel.pfpModel?.toString(),
                            viewModel.backgroundModel?.toString()
                        )
                    }

                    AnimatedVisibility(visible = viewModel.uploadProgress > 0f) {
                        LinearProgressIndicator(
                            progress = { viewModel.uploadProgress },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 0.dp)
                        )
                    }

                    AnimatedVisibility(visible = viewModel.uploadError != null) {
                        Text(
                            text = viewModel.uploadError ?: "",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 0.dp)
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 0.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.settings_profile_profile_picture),
                                style = MaterialTheme.typography.labelLarge
                            )

                            Spacer(Modifier.height(10.dp))

                            InlineMediaPicker(
                                currentModel = viewModel.pfpModel,
                                circular = true,
                                useAvatarCircularity = true,
                                onPick = {
                                    viewModel.pfpModel = it.toString()
                                    viewModel.saveNewPfp()
                                },
                                canRemove = true,
                                onRemove = {
                                    viewModel.removePfp()
                                }
                            )
                        }

                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.settings_profile_custom_background),
                                style = MaterialTheme.typography.labelLarge,
                            )

                            Spacer(Modifier.height(10.dp))

                            InlineMediaPicker(
                                currentModel = viewModel.backgroundModel,
                                onPick = {
                                    viewModel.backgroundModel = it.toString()
                                    viewModel.saveNewBackground()
                                },
                                canRemove = true,
                                onRemove = {
                                    viewModel.removeBackground()
                                }
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 20.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.pendingProfile?.content ?: "",
                            onValueChange = { value ->
                                viewModel.pendingProfile?.let {
                                    viewModel.pendingProfile = it.copy(content = value)
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(id = R.string.user_info_sheet_category_bio),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        AnimatedVisibility(visible = viewModel.bioError != null) {
                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = viewModel.bioError ?: "",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 0.dp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        TextButton(
                            onClick = {
                                viewModel.saveBio()
                            },
                            enabled = viewModel.pendingProfile?.content != viewModel.currentProfile?.content,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.icn_check_24dp),
                                contentDescription = null
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = stringResource(id = R.string.settings_profile_save),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
